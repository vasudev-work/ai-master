package com.vasudev.ai.master.data.ondevice

import android.content.Context
import com.vasudev.ai.master.core.Config
import java.io.File

/**
 * Location + status of the on-device text-to-image model.
 *
 * The model is a Stable Diffusion 1.5 bundle converted for the MediaPipe Image
 * Generator. It is a FOLDER of many small `.bin` files (not a single file), so it
 * is downloaded as a zip and unpacked into the app's private storage.
 */
object ImageModel {

    fun dir(context: Context): File = File(context.filesDir, Config.IMAGE_MODEL_DIR)

    /** True when the unpacked model folder looks complete. */
    fun isPresent(context: Context): Boolean {
        val d = dir(context)
        if (!d.isDirectory) return false
        val files = d.listFiles() ?: return false
        if (files.size < 5) return false
        return files.sumOf { it.length() } >= Config.IMAGE_MODEL_MIN_BYTES
    }

    fun sizeMb(context: Context): Long {
        val d = dir(context)
        if (!d.isDirectory) return 0L
        return (d.listFiles()?.sumOf { it.length() } ?: 0L) / (1024L * 1024L)
    }
}
