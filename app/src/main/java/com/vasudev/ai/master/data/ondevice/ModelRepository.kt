package com.vasudev.ai.master.data.ondevice

import android.content.Context
import android.net.Uri
import com.vasudev.ai.master.core.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Progress of a model download. */
sealed interface DownloadState {
    data class Progress(val fraction: Float) : DownloadState
    data object Done : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * Gets the on-device model onto the phone: either by downloading it, or by
 * importing a `.task` file the user already has. Everything lands in app-private
 * storage, so no storage permission is needed.
 */
@Singleton
class ModelRepository @Inject constructor(@ApplicationContext private val context: Context) {

    fun isModelPresent(): Boolean = OnDeviceModel.isPresent(context)

    fun modelSizeMb(): Long = OnDeviceModel.sizeMb(context)

    /** Downloads the model, emitting progress 0f..1f then Done (or Failed). */
    fun download(url: String = Config.ON_DEVICE_MODEL_URL): Flow<DownloadState> = flow {
        val dest = OnDeviceModel.file(context)
        val tmp = File(context.filesDir, Config.ON_DEVICE_MODEL_FILE + ".part")
        emit(DownloadState.Progress(0f))
        try {
            // Hugging Face redirects to a CDN; follow redirects manually so the
            // request stays valid across hosts.
            var currentUrl = url
            var connection: HttpURLConnection
            var redirects = 0
            while (true) {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.connect()
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: error("Redirect without a Location header")
                    connection.disconnect()
                    currentUrl = if (location.startsWith("http")) location
                    else URL(URL(currentUrl), location).toString()
                    if (++redirects > 10) error("Too many redirects")
                    continue
                }
                if (code != 200) error("Server returned HTTP $code")
                break
            }

            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) emit(DownloadState.Progress(downloaded.toFloat() / total))
                    }
                }
            }
            connection.disconnect()

            if (tmp.length() < Config.ON_DEVICE_MIN_VALID_BYTES) {
                error("Downloaded file looks incomplete")
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            emit(DownloadState.Progress(1f))
            emit(DownloadState.Done)
        } catch (e: Exception) {
            runCatching { tmp.delete() }
            emit(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    /** Copies a user-picked `.task` file into app storage. Returns true on success. */
    fun importModel(uri: Uri): Boolean = try {
        val dest = OnDeviceModel.file(context)
        val tmp = File(context.filesDir, Config.ON_DEVICE_MODEL_FILE + ".part")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        }
        if (tmp.length() < Config.ON_DEVICE_MIN_VALID_BYTES) {
            tmp.delete()
            false
        } else {
            if (dest.exists()) dest.delete()
            tmp.renameTo(dest)
            OnDeviceModel.isPresent(context)
        }
    } catch (e: Exception) {
        false
    }

    fun deleteModel() {
        OnDeviceModel.file(context).delete()
    }
}
