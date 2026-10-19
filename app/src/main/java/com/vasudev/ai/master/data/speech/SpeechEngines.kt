package com.vasudev.ai.master.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.vasudev.ai.master.core.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechToText @Inject constructor(@ApplicationContext private val context: Context) {

    sealed class Event {
        data object Listening : Event()
        data class Partial(val text: String) : Event()
        data class Final(val text: String) : Event()
        data class Error(val message: String) : Event()
    }

    /** Supports Indian language tags: en-IN, hi-IN, ta-IN, te-IN, mr-IN, bn-IN. */
    fun listen(languageTag: String): Flow<Event> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(Event.Error("Speech recognition is not available on this device"))
            close()
            return@callbackFlow
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(Event.Listening) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                trySend(Event.Error("Mic error $error (7 = no speech, 9 = permission)"))
            }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                trySend(Event.Final(text))
                close()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { trySend(Event.Partial(it)) }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
        awaitClose { recognizer.destroy() }
    }
}

@Singleton
class TtsEngine @Inject constructor(@ApplicationContext private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    fun ensure() {
        if (tts == null) {
            tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }
        }
    }

    fun speak(text: String, languageTag: String) {
        val engine = tts ?: return
        if (!ready) return
        val locale = Locale.forLanguageTag(languageTag)
        engine.language = if (engine.isLanguageAvailable(locale) >= 0) locale else Locale.forLanguageTag("en-IN")
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, Config.TTS_UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }
}
