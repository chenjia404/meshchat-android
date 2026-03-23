package com.github.com.chenjia404.meshchat.core.util

import android.content.res.Resources
import com.github.com.chenjia404.meshchat.R
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
 * 会话列表右侧时间：**单一单位**相对时间。
 */
fun formatConversationListRelativeTime(resources: Resources, raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val msg = toZonedOnDevice(raw) ?: return raw
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    if (msg.isAfter(now)) return formatChatTime(raw)

    val msgLdt = msg.toLocalDateTime()
    val nowLdt = now.toLocalDateTime()

    val minutes = ChronoUnit.MINUTES.between(msgLdt, nowLdt)
    if (minutes < 1L) return resources.getString(R.string.time_just_now)
    if (minutes < 60L) return resources.getString(R.string.time_minutes_ago, minutes.toInt())

    val hours = ChronoUnit.HOURS.between(msgLdt, nowLdt)
    if (hours < 24L) return resources.getString(R.string.time_hours_ago, hours.toInt())

    val days = ChronoUnit.DAYS.between(msg.toLocalDate(), now.toLocalDate())
    if (days < 30L) return resources.getString(R.string.time_days_ago, days.toInt())

    val months = ChronoUnit.MONTHS.between(msgLdt, nowLdt)
    if (months < 12L) return resources.getString(R.string.time_months_ago, months.toInt())

    val years = ChronoUnit.YEARS.between(msgLdt, nowLdt)
    return resources.getString(R.string.time_years_ago, years.toInt())
}

/** 私聊气泡下方时间：yyyy-MM-dd HH:mm（设备本地时区） */
fun formatChatMessageLineTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val z = toZonedOnDevice(raw) ?: return raw
    return z.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()))
}

fun renderConversationPreview(
    resources: Resources,
    msgType: String?,
    plaintext: String?,
    mimeType: String?,
    fileName: String?,
): String {
    return when (resolveRenderType(msgType.orEmpty(), mimeType, fileName)) {
        AttachmentRenderType.TEXT -> plaintext.orEmpty()
        AttachmentRenderType.IMAGE -> resources.getString(R.string.preview_image)
        AttachmentRenderType.VIDEO -> resources.getString(R.string.preview_video)
        AttachmentRenderType.AUDIO -> resources.getString(R.string.preview_voice)
        AttachmentRenderType.FILE ->
            fileName?.let { resources.getString(R.string.preview_file_named, it) }
                ?: resources.getString(R.string.preview_file)
        AttachmentRenderType.SYSTEM -> plaintext?.takeIf { it.isNotBlank() } ?: msgType.orEmpty()
    }
}

fun formatRetentionMinutes(resources: Resources, minutes: Int): String {
    if (minutes <= 0) return resources.getString(R.string.retention_no_auto_delete)
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val mins = minutes % 60
    return buildString {
        if (days > 0) append(resources.getString(R.string.retention_days_part, days))
        if (hours > 0) append(resources.getString(R.string.retention_hours_part, hours))
        if (mins > 0 || isEmpty()) append(resources.getString(R.string.retention_minutes_part, mins))
    }
}
