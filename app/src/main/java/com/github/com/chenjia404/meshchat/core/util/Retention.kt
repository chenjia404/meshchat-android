package com.github.com.chenjia404.meshchat.core.util

import android.content.res.Resources
import androidx.annotation.StringRes
import com.github.com.chenjia404.meshchat.R

/**
 * 与 Quark 一致：月 = 30 天、周 = 7 天。
 */
enum class RetentionTimeUnit(@StringRes val labelRes: Int) {
    MINUTE(R.string.retention_unit_minute),
    HOUR(R.string.retention_unit_hour),
    DAY(R.string.retention_unit_day),
    WEEK(R.string.retention_unit_week),
    /** 按 30×24×60 分钟，与 Quark `chat_retention_unit_month` 一致 */
    MONTH(R.string.retention_unit_month),
}

private const val MINUTES_PER_MONTH = 30 * 24 * 60
private const val MINUTES_PER_WEEK = 7 * 24 * 60
private const val MINUTES_PER_DAY = 24 * 60
private const val MINUTES_PER_HOUR = 60

/**
 * 顶栏短文案：在 月 > 周 > 天 > 小时 > 分钟 中取**最大粒度**，对总分钟数**向下取整**为整数个单位。
 */
fun formatRetentionDisplayShort(resources: Resources, totalMinutes: Int): String {
    if (totalMinutes <= 0) return resources.getString(R.string.retention_unlimited)
    val m = totalMinutes
    if (m >= MINUTES_PER_MONTH) {
        return resources.getString(R.string.retention_display_months, m / MINUTES_PER_MONTH)
    }
    if (m >= MINUTES_PER_WEEK) {
        return resources.getString(R.string.retention_display_weeks, m / MINUTES_PER_WEEK)
    }
    if (m >= MINUTES_PER_DAY) {
        return resources.getString(R.string.retention_display_days, m / MINUTES_PER_DAY)
    }
    if (m >= MINUTES_PER_HOUR) {
        return resources.getString(R.string.retention_display_hours, m / MINUTES_PER_HOUR)
    }
    return resources.getString(R.string.retention_display_minutes, m)
}

/** 数量 × 单位 → 总分钟（Long，避免乘法溢出中间步骤） */
fun retentionQuantityToMinutes(value: Long, unit: RetentionTimeUnit): Long {
    return when (unit) {
        RetentionTimeUnit.MINUTE -> value
        RetentionTimeUnit.HOUR -> value * 60L
        RetentionTimeUnit.DAY -> value * 24L * 60L
        RetentionTimeUnit.WEEK -> value * 7L * 24L * 60L
        RetentionTimeUnit.MONTH -> value * 30L * 24L * 60L
    }
}

/**
 * 编辑弹窗预填：与 Quark [fillValueAndUnitFromMinutes] 一致——能整除则优先用更大单位（月→周→天→小时），否则分钟。
 */
fun minutesToPickerPrefill(totalMinutes: Int): Pair<Long, RetentionTimeUnit> {
    if (totalMinutes <= 0) return 0L to RetentionTimeUnit.MINUTE
    val m = totalMinutes
    if (m % MINUTES_PER_MONTH == 0) return (m / MINUTES_PER_MONTH).toLong() to RetentionTimeUnit.MONTH
    if (m % MINUTES_PER_WEEK == 0) return (m / MINUTES_PER_WEEK).toLong() to RetentionTimeUnit.WEEK
    if (m % MINUTES_PER_DAY == 0) return (m / MINUTES_PER_DAY).toLong() to RetentionTimeUnit.DAY
    if (m % MINUTES_PER_HOUR == 0) return (m / MINUTES_PER_HOUR).toLong() to RetentionTimeUnit.HOUR
    return m.toLong() to RetentionTimeUnit.MINUTE
}
