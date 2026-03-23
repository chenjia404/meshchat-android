package com.github.com.chenjia404.meshchat.core.util

import android.content.res.Resources
import com.github.com.chenjia404.meshchat.R
import java.util.Locale

/**
 * 将后端消息状态转为界面展示文案（发送中 / 已送达 等）。
 */
fun formatMessageStateLabel(resources: Resources, state: String?, deliveredHint: Boolean = false): String {
    if (state.isNullOrBlank()) {
        return if (deliveredHint) resources.getString(R.string.message_state_delivered_hint) else ""
    }
    val lower = state.trim().lowercase(Locale.US).replace('-', '_')
    return when (lower) {
        "pending", "queued" -> resources.getString(R.string.message_state_pending)
        "sending" -> resources.getString(R.string.message_state_sending)
        "sent" -> resources.getString(R.string.message_state_sent)
        "sent_to_transport", "transport" -> resources.getString(R.string.message_state_transport)
        "delivered", "delivered_remote" -> resources.getString(R.string.message_state_delivered)
        "read" -> resources.getString(R.string.message_state_read)
        "failed", "error", "send_failed" -> resources.getString(R.string.message_state_failed)
        "revoked" -> resources.getString(R.string.message_state_revoked)
        else -> state
    }
}

fun isFailedMessageState(state: String?): Boolean {
    if (state.isNullOrBlank()) return false
    val l = state.lowercase(Locale.US)
    return l.contains("fail") || l.contains("error")
}
