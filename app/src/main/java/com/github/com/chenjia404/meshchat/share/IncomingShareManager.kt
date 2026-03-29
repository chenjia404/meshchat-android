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
    /** 系统「分享文字」等场景（无 EXTRA_STREAM） */
    val plainText: String? = null,
) {
    val hasContent: Boolean
        get() = uris.isNotEmpty() || !plainText.isNullOrBlank()
}

@Singleton
class IncomingShareManager @Inject constructor() {

    private val _pending = MutableStateFlow<IncomingSharePayload?>(null)
    val pending: StateFlow<IncomingSharePayload?> = _pending.asStateFlow()

    fun setFromIntent(intent: Intent?) {
        if (intent == null) {
            _pending.value = null
            return
        }
        val uris = extractShareUris(intent)
        val plainText = extractSharePlainText(intent)
        _pending.value = when {
            uris.isNotEmpty() -> IncomingSharePayload(uris, intent.type, plainText)
            !plainText.isNullOrBlank() -> IncomingSharePayload(emptyList(), intent.type, plainText)
            else -> null
        }
    }

    fun clear() {
        _pending.value = null
    }
}

private fun extractSharePlainText(intent: Intent): String? {
    val action = intent.action ?: return null
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
    return intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() }
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
