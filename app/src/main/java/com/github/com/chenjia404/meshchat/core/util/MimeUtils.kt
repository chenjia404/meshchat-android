package com.github.com.chenjia404.meshchat.core.util

import java.util.Locale

enum class AttachmentRenderType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    SYSTEM,
}

/**
 * 是否应按**语音/录音**展示（对齐 QuarkPay：`ChatVoiceRecorder.looksLikeVoiceMimeAndName` / `ChatMessageDto.isVoiceContent`）。
 *
 * - 后端可能返回 **`application/ogg`**（Ogg 容器），需与 `audio/ogg; codecs=opus` 一样走语音，而不是视频预览。
 * - 亦支持 `voice_*.m4a` 等旧版 AAC 语音文件名。
 */
fun looksLikeVoiceAttachment(mimeType: String?, fileName: String?): Boolean {
    val mt = mimeType?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val fn = fileName?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (mt.isEmpty() && fn.isEmpty()) return false
    if (mt.contains("opus")) return true
    if (mt.contains("ogg") && mt.startsWith("audio")) return true
    if (mt == "application/ogg" || mt.startsWith("application/ogg")) return true
    if (mt.startsWith("audio/ogg")) return true
    if (mt.startsWith("audio/") && fn.isNotEmpty()) {
        if (fn.startsWith("voice_") && (fn.endsWith(".m4a") || fn.endsWith(".aac"))) return true
    }
    if (fn.endsWith(".ogg") || fn.endsWith(".opus")) return true
    if (fn.startsWith("voice_") && (fn.endsWith(".m4a") || fn.endsWith(".aac"))) return true
    return false
}

fun resolveRenderType(msgType: String, mimeType: String?, fileName: String? = null): AttachmentRenderType {
    return when (msgType) {
        "chat_text", "group_chat_text" -> AttachmentRenderType.TEXT
        "chat_file", "group_chat_file" -> when {
            // 须在 image/video 之前：避免 `video/ogg`+`.ogg` 等被当成视频缩略图
            looksLikeVoiceAttachment(mimeType, fileName) -> AttachmentRenderType.AUDIO
            mimeType.orEmpty().startsWith("image/") -> AttachmentRenderType.IMAGE
            mimeType.orEmpty().startsWith("video/") -> AttachmentRenderType.VIDEO
            mimeType.orEmpty().startsWith("audio/") -> AttachmentRenderType.AUDIO
            else -> AttachmentRenderType.FILE
        }

        else -> AttachmentRenderType.SYSTEM
    }
}
