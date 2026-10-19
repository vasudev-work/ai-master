package com.vasudev.ai.master.data.ondevice

import android.content.Context
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
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the on-device image model (a zip) and unpacks it into app-private
 * storage. Everything stays on the phone; no key or account is needed.
 */
@Singleton
class ImageModelRepository @Inject constructor(@ApplicationContext private val context: Context) {

    fun isModelPresent(): Boolean = ImageModel.isPresent(context)

    fun modelSizeMb(): Long = ImageModel.sizeMb(context)

    fun download(url: String = Config.IMAGE_MODEL_URL): Flow<DownloadState> = flow {
        val tmpZip = File(context.filesDir, "sd15-mediapipe.zip.part")
        emit(DownloadState.Progress(0f))
        try {
            var currentUrl = url
            var connection: HttpURLConnection
            var redirects = 0
            while (true) {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 30_000
                connection.readTimeout = 120_000
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
                FileOutputStream(tmpZip).use { output ->
                    val buffer = ByteArray(128 * 1024)
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

            // Unpack into a staging folder, then move the model folder into place.
            val staging = File(context.filesDir, "sd15_staging")
            staging.deleteRecursively()
            staging.mkdirs()
            unzip(tmpZip, staging)

            val bins = findDir(staging, Config.IMAGE_MODEL_DIR)
                ?: error("Model folder not found inside the archive")
            val dest = ImageModel.dir(context)
            dest.deleteRecursively()
            if (!bins.renameTo(dest)) {
                bins.copyRecursively(dest, overwrite = true)
            }
            staging.deleteRecursively()
            tmpZip.delete()

            if (!ImageModel.isPresent(context)) error("Unpacked model looks incomplete")
            emit(DownloadState.Progress(1f))
            emit(DownloadState.Done)
        } catch (e: Exception) {
            runCatching { tmpZip.delete() }
            emit(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteModel() {
        ImageModel.dir(context).deleteRecursively()
    }

    private fun findDir(root: File, name: String): File? {
        if (root.name == name) return root
        val children = root.listFiles() ?: return null
        for (c in children) {
            if (c.isDirectory) {
                val found = findDir(c, name)
                if (found != null) return found
            }
        }
        return null
    }

    private fun unzip(zip: File, targetDir: File) {
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            val root = targetDir.canonicalFile
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                // Guard against zip-slip
                if (!outFile.canonicalPath.startsWith(root.path + File.separator)) {
                    error("Unsafe path in archive: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
