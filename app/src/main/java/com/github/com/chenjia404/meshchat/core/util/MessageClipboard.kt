package com.github.com.chenjia404.meshchat.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageUiModel

fun copyChatMessageToClipboard(context: Context, message: ChatMessageUiModel) {
    val text = when (message.renderType) {
        AttachmentRenderType.TEXT -> message.text
        AttachmentRenderType.SYSTEM -> message.text.ifBlank { message.subtitle }
        AttachmentRenderType.IMAGE,
        AttachmentRenderType.VIDEO,
        AttachmentRenderType.AUDIO,
        AttachmentRenderType.FILE,
        -> {
            val name = message.fileName?.takeIf { it.isNotBlank() }
            val typeLabel = message.subtitle.takeIf { it.isNotBlank() } ?: "附件"
            listOfNotNull(typeLabel, name).joinToString(" ").ifBlank { "[附件]" }
        }
    }
    if (text.isBlank()) return
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("message", text))
}
