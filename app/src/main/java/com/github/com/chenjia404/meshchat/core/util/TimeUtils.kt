package com.github.com.chenjia404.meshchat.core.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 将接口返回的 ISO-8601 时间转为 **用户手机当前时区** 后再用于展示。
 * 支持 `OffsetDateTime` 与 `Instant`（如带 `Z`）格式。
 */
private fun toZonedOnDevice(raw: String): ZonedDateTime? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    runCatching {
        OffsetDateTime.parse(t).atZoneSameInstant(ZoneId.systemDefault())
    }.getOrNull()?.let { return it }
    return runCatching {
        Instant.parse(t).atZone(ZoneId.systemDefault())
    }.getOrNull()
}

fun formatChatTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val z = toZonedOnDevice(raw) ?: return raw
    return z.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
}

/** 私聊气泡下方时间：yyyy-MM-dd HH:mm（设备本地时区） */
fun formatChatMessageLineTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val z = toZonedOnDevice(raw) ?: return raw
    return z.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()))
}

fun renderConversationPreview(msgType: String?, plaintext: String?, mimeType: String?, fileName: String?): String {
    return when (resolveRenderType(msgType.orEmpty(), mimeType, fileName)) {
        AttachmentRenderType.TEXT -> plaintext.orEmpty()
        AttachmentRenderType.IMAGE -> "[图片]"
        AttachmentRenderType.VIDEO -> "[视频]"
        AttachmentRenderType.AUDIO -> "[语音]"
        AttachmentRenderType.FILE -> fileName?.let { "[文件] $it" } ?: "[文件]"
        AttachmentRenderType.SYSTEM -> plaintext?.takeIf { it.isNotBlank() } ?: msgType.orEmpty()
    }
}

fun formatRetentionMinutes(minutes: Int): String {
    if (minutes <= 0) return "不自动删除"
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val mins = minutes % 60
    return buildString {
        if (days > 0) append("${days}天")
        if (hours > 0) append("${hours}小时")
        if (mins > 0 || isEmpty()) append("${mins}分钟")
    }
}
