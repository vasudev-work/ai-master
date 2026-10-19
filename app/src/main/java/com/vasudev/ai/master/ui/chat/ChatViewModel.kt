package com.vasudev.ai.master.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasudev.ai.master.core.ChatMessage
import com.vasudev.ai.master.core.ChatMode
import com.vasudev.ai.master.core.Config
import com.vasudev.ai.master.core.SearchResult
import com.vasudev.ai.master.core.domain
import com.vasudev.ai.master.core.parseSearchQueries
import com.vasudev.ai.master.data.SecurePrefs
import com.vasudev.ai.master.data.SettingsStore
import com.vasudev.ai.master.data.UiSettings
import com.vasudev.ai.master.data.db.ChatDao
import com.vasudev.ai.master.data.db.MessageEntity
import com.vasudev.ai.master.data.db.toDomain
import com.vasudev.ai.master.data.mlkit.VisionEngine
import com.vasudev.ai.master.data.ondevice.OnDeviceEngine
import com.vasudev.ai.master.data.remote.CompletionRequest
import com.vasudev.ai.master.data.remote.LlmApi
import com.vasudev.ai.master.data.remote.SearchApi
import com.vasudev.ai.master.data.remote.WireMessage
import com.vasudev.ai.master.data.remote.buildLocalAnswer
import com.vasudev.ai.master.data.remote.buildOnDeviceStoryPrompt
import com.vasudev.ai.master.data.remote.buildResearchReportPrompt
import com.vasudev.ai.master.data.remote.buildSystemPrompt
import com.vasudev.ai.master.data.remote.researchPlannerPrompt
import com.vasudev.ai.master.data.speech.SpeechToText
import com.vasudev.ai.master.data.speech.TtsEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dao: ChatDao,
    private val llm: LlmApi,
    private val search: SearchApi,
    private val vision: VisionEngine,
    private val stt: SpeechToText,
    private val tts: TtsEngine,
    private val settingsStore: SettingsStore,
    private val prefs: SecurePrefs,
    private val onDevice: OnDeviceEngine
) : ViewModel() {

    data class UiState(
        val input: String = "",
        val isListening: Boolean = false,
        val partialSpeech: String = "",
        val pendingImageUri: String? = null,
        val streamingId: Long? = null,
        val tier: String = "ONLINE",
        val error: String? = null,
        val ttsEnabled: Boolean = false,
        val mode: ChatMode = ChatMode.ASK
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val streamBuffer = MutableStateFlow("")
    private val streamingId = MutableStateFlow<Long?>(null)
    private var streamJob: Job? = null
    private var settings = UiSettings()

    val messages: StateFlow<List<ChatMessage>> = combine(
        dao.observeSession(SESSION),
        streamBuffer,
        streamingId
    ) { list, buffer, sid ->
        list.map { entity ->
            if (entity.id == sid) entity.copy(content = buffer) else entity
        }.map { it.toDomain() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            settingsStore.flow.collect { settings = it }
        }
    }

    fun onInput(text: String) = _ui.update { it.copy(input = text) }

    fun onImage(uri: String?) = _ui.update { it.copy(pendingImageUri = uri) }

    fun setMode(mode: ChatMode) = _ui.update { it.copy(mode = mode) }

    fun toggleTts() {
        if (_ui.value.ttsEnabled) tts.stop()
        tts.ensure()
        _ui.update { it.copy(ttsEnabled = !it.ttsEnabled) }
    }

    fun dismissError() = _ui.update { it.copy(error = null) }

    fun clearChat() {
        viewModelScope.launch { dao.clear() }
    }

    fun startVoice() {
        if (_ui.value.isListening) {
            _ui.update { it.copy(isListening = false) }
            return
        }
        _ui.update { it.copy(isListening = true, partialSpeech = "") }
        viewModelScope.launch {
            stt.listen(settings.languageTag).collect { event ->
                when (event) {
                    is SpeechToText.Event.Listening -> Unit
                    is SpeechToText.Event.Partial ->
                        _ui.update { it.copy(partialSpeech = event.text) }
                    is SpeechToText.Event.Final ->
                        _ui.update { it.copy(isListening = false, input = event.text) }
                    is SpeechToText.Event.Error ->
                        _ui.update { it.copy(isListening = false, error = event.message) }
                }
            }
        }
    }

    fun stopStream() {
        streamJob?.cancel()
        finishStream()
    }

    private fun finishStream() {
        val sid = streamingId.value ?: return
        val text = streamBuffer.value
        viewModelScope.launch { dao.updateContent(sid, text) }
        streamingId.value = null
        streamBuffer.value = ""
        _ui.update { it.copy(streamingId = null) }
    }

    fun send(forceWebSearch: Boolean = false) {
        val state = _ui.value
        if (state.streamingId != null) return
        val rawText = state.input.trim()
        val image = state.pendingImageUri
        if (rawText.isEmpty() && image == null) return
        val mode = state.mode

        if (!settings.privateMode && prefs.llmApiKey.isBlank()) {
            _ui.update { it.copy(error = "Add your AI provider API key in Settings first.") }
            return
        }

        _ui.update { it.copy(input = "", pendingImageUri = null, error = null) }

        streamJob = viewModelScope.launch {
            try {
                var ocr: String? = null
                var labels: List<String> = emptyList()
                var faces = 0
                if (image != null) {
                    val devanagari = settings.languageTag.startsWith("hi") ||
                        settings.languageTag.startsWith("mr")
                    runCatching { vision.ocr(image, devanagari) }.onSuccess { ocr = it }
                    runCatching { vision.labels(image) }.onSuccess { labels = it }
                    runCatching { vision.faceCount(image) }.onSuccess { faces = it }
                }

                dao.insert(
                    MessageEntity(
                        sessionId = SESSION,
                        role = "user",
                        content = rawText,
                        imageUri = image
                    )
                )

                // Private Mode answers from on-device data only; on-device story
                // mode takes precedence when both are enabled.
                if (settings.privateMode && !settings.onDeviceMode) {
                    dao.insert(
                        MessageEntity(
                            sessionId = SESSION,
                            role = "assistant",
                            content = buildLocalAnswer(ocr, labels, faces),
                            modelTier = "ON-DEVICE"
                        )
                    )
                    return@launch
                }

                // On-device (offline) story generation, no network involved.
                if (settings.onDeviceMode) {
                    val loaded = withContext(Dispatchers.IO) { onDevice.loadIfNeeded() }
                    if (!loaded) {
                        _ui.update {
                            it.copy(error = "On-device model is not ready. Download it in Settings first.")
                        }
                        return@launch
                    }
                    val onDeviceId = dao.insert(
                        MessageEntity(
                            sessionId = SESSION,
                            role = "assistant",
                            content = "",
                            modelTier = "ON-DEVICE"
                        )
                    )
                    streamingId.value = onDeviceId
                    _ui.update { it.copy(streamingId = onDeviceId, tier = "ON-DEVICE") }
                    streamBuffer.value = ""

                    val prompt = buildOnDeviceStoryPrompt(settings.styleExamples, rawText)
                    val builder = StringBuilder()
                    withContext(Dispatchers.IO) {
                        onDevice.generate(prompt).collect { chunk ->
                            builder.append(chunk)
                            streamBuffer.value = builder.toString()
                        }
                    }
                    dao.updateContent(onDeviceId, builder.toString())
                    streamingId.value = null
                    streamBuffer.value = ""
                    _ui.update { it.copy(streamingId = null) }
                    if (_ui.value.ttsEnabled) {
                        tts.speak(builder.toString(), settings.languageTag)
                    }
                    return@launch
                }

                // ---- Deep Research: plan -> search -> synthesise a cited report ----
                if (mode == ChatMode.RESEARCH) {
                    val assistantId = dao.insert(
                        MessageEntity(sessionId = SESSION, role = "assistant", content = "", modelTier = "RESEARCH")
                    )
                    streamingId.value = assistantId
                    _ui.update { it.copy(streamingId = assistantId, tier = "RESEARCH") }
                    streamBuffer.value = "Planning research…"

                    // 1) plan a few search queries
                    val planReq = CompletionRequest(
                        model = prefs.model.ifBlank { Config.DEFAULT_MODEL },
                        messages = listOf(
                            WireMessage("system", researchPlannerPrompt()),
                            WireMessage("user", rawText)
                        ),
                        stream = false
                    )
                    val plan = runCatching {
                        llm.completeOnce(prefs.baseUrl, prefs.llmApiKey, planReq)
                    }.getOrDefault("")
                    val queries = parseSearchQueries(plan, rawText)

                    // 2) run the searches and collect notes
                    val notes = StringBuilder()
                    var sources = 0
                    for (q in queries) {
                        streamBuffer.value = "Searching: $q …"
                        val res = runCatching {
                            search.search(q, settings.languageTag, 5)
                        }.getOrDefault(emptyList())
                        res.forEach {
                            notes.append("[").append(it.domain()).append("] ")
                                .append(it.title).append(": ").append(it.snippet).append('\n')
                            sources++
                        }
                    }

                    // 3) synthesise the report, streamed
                    val reportReq = CompletionRequest(
                        model = prefs.model.ifBlank { Config.DEFAULT_MODEL },
                        messages = listOf(
                            WireMessage(
                                "system",
                                buildResearchReportPrompt(
                                    rawText, notes.toString(), settings.languageTag, settings.memory
                                )
                            )
                        ),
                        stream = true
                    )
                    streamBuffer.value = "Writing report…"
                    val builder = StringBuilder()
                    llm.streamChat(prefs.baseUrl, prefs.llmApiKey, reportReq).collect { chunk ->
                        builder.append(chunk)
                        streamBuffer.value = builder.toString()
                    }
                    val finalTier = if (sources > 0) "RESEARCH · $sources sources" else "RESEARCH"
                    dao.updateContent(assistantId, builder.toString())
                    streamingId.value = null
                    streamBuffer.value = ""
                    _ui.update { it.copy(streamingId = null, tier = finalTier) }
                    if (_ui.value.ttsEnabled) {
                        tts.speak(builder.toString(), settings.languageTag)
                    }
                    return@launch
                }

                // ---- Normal / Web / Study ----
                val history = dao.history(SESSION)
                    .filter { it.content.isNotBlank() }
                    .map { WireMessage(it.role, it.content) }

                var results: List<SearchResult> = emptyList()
                val wantSearch = forceWebSearch || settings.grounded || mode == ChatMode.WEB
                if (wantSearch) {
                    runCatching { search.search(rawText, settings.languageTag) }
                        .onSuccess { results = it }
                }
                val tier = when {
                    mode == ChatMode.STUDY -> "STUDY"
                    results.isNotEmpty() -> "ONLINE · WEB"
                    else -> "ONLINE"
                }
                _ui.update { it.copy(tier = tier) }

                val assistantId = dao.insert(
                    MessageEntity(sessionId = SESSION, role = "assistant", content = "", modelTier = tier)
                )
                streamingId.value = assistantId
                _ui.update { it.copy(streamingId = assistantId) }
                streamBuffer.value = ""

                val request = CompletionRequest(
                    model = prefs.model.ifBlank { Config.DEFAULT_MODEL },
                    messages = listOf(
                        WireMessage(
                            "system",
                            buildSystemPrompt(
                                ocr, labels, results, settings.languageTag,
                                settings.styleExamples, faces, mode, settings.memory
                            )
                        )
                    ) + history,
                    stream = true
                )

                val builder = StringBuilder()
                llm.streamChat(prefs.baseUrl, prefs.llmApiKey, request).collect { chunk ->
                    builder.append(chunk)
                    streamBuffer.value = builder.toString()
                }
                dao.updateContent(assistantId, builder.toString())
                streamingId.value = null
                streamBuffer.value = ""
                _ui.update { it.copy(streamingId = null) }
                if (_ui.value.ttsEnabled) {
                    tts.speak(builder.toString(), settings.languageTag)
                }
            } catch (e: Exception) {
                val sid = streamingId.value
                if (sid != null) {
                    runCatching { dao.updateContent(sid, streamBuffer.value) }
                }
                streamingId.value = null
                streamBuffer.value = ""
                _ui.update { it.copy(streamingId = null, error = e.message ?: "Something went wrong") }
            }
        }
    }

    companion object {
        const val SESSION = Config.SESSION_ID
    }
}
