package com.vasudev.ai.master.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

object ImageVault {

    /** Copies a picked gallery image into app-private storage (content URIs expire). */
    fun copyToVault(context: Context, uri: Uri): String {
        val dir = File(context.filesDir, "vault/images").apply { mkdirs() }
        val dest = File(dir, "img_" + System.currentTimeMillis() + ".jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(dest).toString()
    }

    /** Saves a captured camera bitmap into app-private storage. */
    fun saveToVault(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "vault/images").apply { mkdirs() }
        val dest = File(dir, "img_" + System.currentTimeMillis() + ".jpg")
        dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return Uri.fromFile(dest).toString()
    }
}
