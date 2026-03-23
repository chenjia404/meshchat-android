package com.github.com.chenjia404.meshchat.core.util

enum class AttachmentRenderType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    SYSTEM,
}

fun resolveRenderType(msgType: String, mimeType: String?): AttachmentRenderType {
    return when (msgType) {
        "chat_text", "group_chat_text" -> AttachmentRenderType.TEXT
        "chat_file", "group_chat_file" -> when {
            mimeType.orEmpty().startsWith("image/") -> AttachmentRenderType.IMAGE
            mimeType.orEmpty().startsWith("video/") -> AttachmentRenderType.VIDEO
            mimeType.orEmpty().startsWith("audio/") -> AttachmentRenderType.AUDIO
            else -> AttachmentRenderType.FILE
        }

        else -> AttachmentRenderType.SYSTEM
    }
}

