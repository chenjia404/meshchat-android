package com.github.com.chenjia404.meshchat.service.download

import android.content.Context
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
    suspend fun download(url: String, fileName: String): File = withContext(ioDispatcher) {
        val targetDir = context.getExternalFilesDir("downloads") ?: context.filesDir
        val target = File(targetDir, fileName.ifBlank { "meshchat-file" })
        val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) throw IOException("download failed: ${response.code}")
        response.body?.byteStream()?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target
    }
}

