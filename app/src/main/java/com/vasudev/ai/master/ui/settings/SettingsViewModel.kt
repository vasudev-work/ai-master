package com.vasudev.ai.master.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasudev.ai.master.core.Config
import com.vasudev.ai.master.data.SecurePrefs
import com.vasudev.ai.master.data.SettingsStore
import com.vasudev.ai.master.data.UiSettings
import com.vasudev.ai.master.data.db.ChatDao
import com.vasudev.ai.master.data.ondevice.DownloadState
import com.vasudev.ai.master.data.ondevice.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SecurePrefs,
    private val settingsStore: SettingsStore,
    private val dao: ChatDao,
    private val modelRepo: ModelRepository
) : ViewModel() {

    val baseUrl = MutableStateFlow(prefs.baseUrl)
    val apiKey = MutableStateFlow(prefs.llmApiKey)
    val model = MutableStateFlow(prefs.model)
    val searchKey = MutableStateFlow(prefs.searchApiKey)
    val styleExamples = MutableStateFlow("")

    val settings: StateFlow<UiSettings> = settingsStore.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    // --- on-device model state ---
    val modelReady = MutableStateFlow(modelRepo.isModelPresent())
    val modelSizeMb = MutableStateFlow(modelRepo.modelSizeMb())
    val download = MutableStateFlow<DownloadState?>(null)

    init {
        viewModelScope.launch {
            settingsStore.flow.collect { styleExamples.value = it.styleExamples }
        }
        refreshModel()
    }

    fun saveAll() {
        prefs.baseUrl = baseUrl.value.trim().ifBlank { Config.DEFAULT_BASE_URL }
        prefs.llmApiKey = apiKey.value.trim()
        prefs.model = model.value.trim().ifBlank { Config.DEFAULT_MODEL }
        prefs.searchApiKey = searchKey.value.trim()
        viewModelScope.launch { settingsStore.setStyleExamples(styleExamples.value) }
    }

    fun setLanguage(tag: String) = viewModelScope.launch { settingsStore.setLanguage(tag) }

    fun setGrounded(value: Boolean) = viewModelScope.launch { settingsStore.setGrounded(value) }

    fun setPrivate(value: Boolean) = viewModelScope.launch { settingsStore.setPrivate(value) }

    fun setOnDevice(value: Boolean) = viewModelScope.launch { settingsStore.setOnDevice(value) }

    fun clearHistory() = viewModelScope.launch { dao.clear() }

    // --- on-device model management ---
    private fun refreshModel() {
        modelReady.value = modelRepo.isModelPresent()
        modelSizeMb.value = modelRepo.modelSizeMb()
    }

    fun downloadModel() = viewModelScope.launch {
        modelRepo.download().collect { state ->
            download.value = state
            if (state is DownloadState.Done || state is DownloadState.Failed) refreshModel()
        }
    }

    fun importModel(uri: Uri) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { modelRepo.importModel(uri) }
        download.value = if (ok) DownloadState.Done else DownloadState.Failed("Import failed")
        refreshModel()
    }

    fun deleteModel() {
        modelRepo.deleteModel()
        download.value = null
        refreshModel()
    }
}
