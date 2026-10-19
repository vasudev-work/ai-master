package com.vasudev.ai.master.data.mlkit

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/** Smart Lens: on-device OCR (Latin + Devanagari) and image labelling. */
@Singleton
class VisionEngine @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun ocr(imageUri: String, devanagari: Boolean = false): String =
        withContext(Dispatchers.IO) {
            val recognizer = if (devanagari) {
                TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            } else {
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
            val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { text -> cont.resume(text.text) }
                    .addOnFailureListener { cont.resume("") }
                    .addOnCompleteListener { recognizer.close() }
            }
        }

    suspend fun labels(imageUri: String): List<String> =
        withContext(Dispatchers.IO) {
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
            suspendCancellableCoroutine { cont ->
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        cont.resume(labels.filter { it.confidence > 0.6f }.map { it.text })
                    }
                    .addOnFailureListener { cont.resume(emptyList()) }
                    .addOnCompleteListener { labeler.close() }
            }
        }

    /** Counts human faces on-device. No identity data is produced or stored. */
    suspend fun faceCount(imageUri: String): Int =
        withContext(Dispatchers.IO) {
            val detector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build()
            )
            val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
            suspendCancellableCoroutine { cont ->
                detector.process(image)
                    .addOnSuccessListener { faces -> cont.resume(faces.size) }
                    .addOnFailureListener { cont.resume(0) }
                    .addOnCompleteListener { detector.close() }
            }
        }
}
