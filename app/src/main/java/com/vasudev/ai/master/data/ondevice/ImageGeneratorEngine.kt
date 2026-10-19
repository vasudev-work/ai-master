package com.vasudev.ai.master.data.ondevice

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import com.vasudev.ai.master.core.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the on-device text-to-image model (Stable Diffusion 1.5 via MediaPipe).
 * Fully offline once the model folder is present. Generation is slow, so call
 * [loadIfNeeded] and [generate] off the main thread.
 */
@Singleton
class ImageGeneratorEngine @Inject constructor(@ApplicationContext private val context: Context) {

    @Volatile
    private var generator: ImageGenerator? = null

    val isReady: Boolean get() = generator != null

    /** Loads the model. Blocking. Returns true on success. */
    @Synchronized
    fun loadIfNeeded(): Boolean {
        if (generator != null) return true
        if (!ImageModel.isPresent(context)) return false
        return try {
            val options = ImageGenerator.ImageGeneratorOptions.builder()
                .setImageGeneratorModelDirectory(ImageModel.dir(context).absolutePath)
                .build()
            generator = ImageGenerator.createFromOptions(context, options)
            true
        } catch (e: Exception) {
            generator = null
            false
        }
    }

    /** Generates an image for the prompt. Blocking. Returns null on failure. */
    @Synchronized
    fun generate(prompt: String, seed: Int = 42): Bitmap? {
        val engine = generator ?: return null
        return try {
            val result = engine.generate(prompt, Config.IMAGE_ITERATIONS, seed)
            BitmapExtractor.extract(result?.generatedImage())
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun close() {
        runCatching { generator?.close() }
        generator = null
    }
}
