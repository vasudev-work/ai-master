package com.vasudev.ai.master.data.ondevice

import android.content.Context
import com.vasudev.ai.master.core.Config
import java.io.File

/**
 * Metadata + location helpers for the on-device story model.
 *
 * The model is a Gemma-3 1B (int4) bundle in MediaPipe `.task` format (~555 MB).
 * It is NOT bundled in the APK (far too large) — it is downloaded once into the
 * app's private files directory, then runs fully offline.
 *
 * Official source (gated — requires accepting the Gemma Terms of Use):
 *   https://huggingface.co/litert-community/Gemma3-1B-IT
 * The default URL in Config is a public, unmodified re-host of the same file, so
 * the app can fetch it without each user needing a Hugging Face account. If that
 * URL is ever unavailable, the user can import the `.task` file manually instead.
 */
object OnDeviceModel {

    fun file(context: Context): File = File(context.filesDir, Config.ON_DEVICE_MODEL_FILE)

    /** True only when a complete-looking model file is present. */
    fun isPresent(context: Context): Boolean {
        val f = file(context)
        return f.exists() && f.length() >= Config.ON_DEVICE_MIN_VALID_BYTES
    }

    fun sizeMb(context: Context): Long {
        val f = file(context)
        return if (f.exists()) f.length() / (1024L * 1024L) else 0L
    }
}
