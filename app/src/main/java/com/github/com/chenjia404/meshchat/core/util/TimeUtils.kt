package com.github.com.chenjia404.meshchat.core.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

/**
 * 会话列表右侧时间：**单一单位**相对时间（刚刚 / N分钟前 / N小时前 / N天前 / N个月前 / N年前）。
 * 优先用较大粒度；与「当前时刻」比较，使用设备本地时区。
 */
fun formatConversationListRelativeTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val msg = toZonedOnDevice(raw) ?: return raw
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    if (msg.isAfter(now)) return formatChatTime(raw)

    val msgLdt = msg.toLocalDateTime()
    val nowLdt = now.toLocalDateTime()

    val minutes = ChronoUnit.MINUTES.between(msgLdt, nowLdt)
    if (minutes < 1L) return "刚刚"
    if (minutes < 60L) return "${minutes}分钟前"

    val hours = ChronoUnit.HOURS.between(msgLdt, nowLdt)
    if (hours < 24L) return "${hours}小时前"

    val days = ChronoUnit.DAYS.between(msg.toLocalDate(), now.toLocalDate())
    if (days < 30L) return "${days}天前"

    val months = ChronoUnit.MONTHS.between(msgLdt, nowLdt)
    if (months < 12L) return "${months}个月前"

    val years = ChronoUnit.YEARS.between(msgLdt, nowLdt)
    return "${years}年前"
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
