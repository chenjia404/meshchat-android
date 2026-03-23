package com.github.com.chenjia404.meshchat.service.download

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class FileDownloadService @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("http") private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** 转发等内部逻辑：保存到应用目录临时文件，用完即删 */
    suspend fun downloadToTempForForward(url: String, fileName: String): File = withContext(ioDispatcher) {
        val targetDir = context.getExternalFilesDir("downloads") ?: context.filesDir
        val safeName = sanitizeFileName(fileName)
        val target = File(targetDir, safeName)
        downloadStreamToFile(url, target)
        target
    }

    /**
     * 用户点击「下载」：保存到系统**公共下载目录**（可见于「文件」/「下载」应用）。
     * - Android 10+：使用 [MediaStore.Downloads]
     * - Android 9 及以下：在已授予 [android.Manifest.permission.WRITE_EXTERNAL_STORAGE] 时写入 [Environment.DIRECTORY_DOWNLOADS]/MeshChat；
     *   未授权时退化为 [DownloadManager] 队列（由系统完成下载）
     */
    suspend fun downloadToPublicDownloads(url: String, fileName: String): PublicDownloadResult =
        withContext(ioDispatcher) {
            val safeName = sanitizeFileName(fileName)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    val uri = saveToMediaStoreDownloads(url, safeName)
                    PublicDownloadResult.Success(uri)
                }
                hasLegacyWritePermission() -> {
                    val file = saveToLegacyPublicDownloads(url, safeName)
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    PublicDownloadResult.Success(contentUri)
                }
                else -> {
                    enqueueSystemDownloadManager(url, safeName)
                    PublicDownloadResult.QueuedSystemDownload
                }
            }
        }

    private fun hasLegacyWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun saveToMediaStoreDownloads(url: String, safeName: String): Uri {
        val resolver = context.contentResolver
        val mime = guessMimeType(safeName)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MeshChat")
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collection, contentValues)
            ?: throw IOException("无法创建下载文件（MediaStore）")
        resolver.openOutputStream(itemUri)?.use { output ->
            okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("download failed: ${response.code}")
                response.body?.byteStream()?.use { input -> input.copyTo(output) }
                    ?: throw IOException("empty body")
            }
        } ?: throw IOException("无法写入下载文件")
        return itemUri
    }

    private fun saveToLegacyPublicDownloads(url: String, safeName: String): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(publicDir, "MeshChat")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("无法创建目录")
        }
        val target = File(dir, safeName)
        downloadStreamToFile(url, target)
        return target
    }

    private fun enqueueSystemDownloadManager(url: String, safeName: String) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(safeName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MeshChat/$safeName")
        dm.enqueue(request)
    }

    private fun downloadStreamToFile(url: String, target: File) {
        val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw IOException("download failed: ${response.code}")
        response.body?.byteStream()?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("empty body")
    }

    private fun guessMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".apk") -> "application/vnd.android.package-archive"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".mp3") -> "audio/mpeg"
            else -> "application/octet-stream"
        }
    }

    private fun sanitizeFileName(name: String): String {
        val t = name.trim().replace("/", "_").replace("..", "_")
        return if (t.isBlank()) "meshchat-file" else t
    }
}

/** 用户下载到公共目录的结果 */
sealed class PublicDownloadResult {
    /** 可直接用于打开/安装（如 APK）的 [Uri] */
    data class Success(val uri: Uri) : PublicDownloadResult()

    /** 已交给系统 [DownloadManager]，请在通知栏或「下载」应用中查看 */
    data object QueuedSystemDownload : PublicDownloadResult()
}
