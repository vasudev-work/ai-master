package com.vasudev.ai.master.data.ondevice

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.vasudev.ai.master.core.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the on-device LLM (Gemma-3 1B via MediaPipe LLM Inference).
 *
 * Everything happens on the phone — no network is used for generation once the
 * model file is present. Loading the model is slow (a few seconds), so
 * [loadIfNeeded] and [generate] must be called off the main thread.
 */
@Singleton
class OnDeviceEngine @Inject constructor(@ApplicationContext private val context: Context) {

    @Volatile
    private var llm: LlmInference? = null

    val isReady: Boolean get() = llm != null

    /** Loads the model if not already loaded. Returns true on success. Blocking. */
    @Synchronized
    fun loadIfNeeded(): Boolean {
        if (llm != null) return true
        if (!OnDeviceModel.isPresent(context)) return false
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(OnDeviceModel.file(context).absolutePath)
                .setMaxTokens(Config.ON_DEVICE_MAX_TOKENS)
                .setMaxTopK(Config.ON_DEVICE_TOP_K)
                // CPU backend is the most broadly compatible; it is slower than GPU.
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()
            llm = LlmInference.createFromOptions(context, options)
            true
        } catch (e: Exception) {
            llm = null
            false
        }
    }

    /** Streams the generated text. Collect this on a background dispatcher. */
    fun generate(prompt: String): Flow<String> = callbackFlow {
        val engine = llm
        if (engine == null) {
            close(IllegalStateException("On-device model is not loaded"))
            return@callbackFlow
        }
        try {
            engine.generateResponseAsync(prompt, ProgressListener<String> { partial, done ->
                if (partial != null) trySend(partial)
                if (done) close()
            })
        } catch (e: Exception) {
            close(e)
        }
        awaitClose { }
    }

    @Synchronized
    fun close() {
        runCatching { llm?.close() }
        llm = null
    }
}
