package com.github.com.chenjia404.meshchat.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingSharePayload(
    val uris: List<Uri>,
    val mimeType: String?,
)

@Singleton
class IncomingShareManager @Inject constructor() {

    private val _pending = MutableStateFlow<IncomingSharePayload?>(null)
    val pending: StateFlow<IncomingSharePayload?> = _pending.asStateFlow()

    fun setFromIntent(intent: Intent?) {
        val uris = extractShareUris(intent)
        _pending.value = if (uris.isEmpty()) null else IncomingSharePayload(uris, intent?.type)
    }

    fun clear() {
        _pending.value = null
    }
}

fun extractShareUris(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    return when (intent.action) {
        Intent.ACTION_SEND -> {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            listOfNotNull(uri)
        }
        Intent.ACTION_SEND_MULTIPLE -> {
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            }
        }
        else -> emptyList()
    }
}
