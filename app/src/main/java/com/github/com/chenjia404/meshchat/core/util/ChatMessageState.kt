package com.github.com.chenjia404.meshchat.core.util

import java.util.Locale

/**
 * 将后端消息状态转为界面展示文案（与设计稿一致：发送中 / 已送达 等）。
 */
fun formatMessageStateLabel(state: String?, deliveredHint: Boolean = false): String {
    if (state.isNullOrBlank()) {
        return if (deliveredHint) "已送达" else ""
    }
    val lower = state.trim().lowercase(Locale.US).replace('-', '_')
    return when (lower) {
        "pending", "queued" -> "排队中"
        "sending" -> "发送中"
        "sent" -> "已发送"
        "sent_to_transport", "transport" -> "已送至传输"
        "delivered", "delivered_remote" -> "已送达"
        "read" -> "已读"
        "failed", "error", "send_failed" -> "发送失败"
        "revoked" -> "已撤回"
        else -> state
    }
}

fun isFailedMessageState(state: String?): Boolean {
    if (state.isNullOrBlank()) return false
    val l = state.lowercase(Locale.US)
    return l.contains("fail") || l.contains("error")
}
