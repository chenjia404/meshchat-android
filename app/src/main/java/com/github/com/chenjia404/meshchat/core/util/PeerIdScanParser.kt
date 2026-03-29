package com.github.com.chenjia404.meshchat.core.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 从扫码或粘贴的字符串中提取 peer_id：纯文本直接使用；
 * 若为带 query 的 HTTP(S) URL，尝试读取 peer_id / peerId 参数。
 */
fun extractPeerIdFromScanString(raw: String): String {
    val t = raw.trim()
    if (t.isEmpty()) return ""
    val url = t.toHttpUrlOrNull() ?: return t
    url.queryParameter("peer_id")?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    url.queryParameter("peerId")?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    return t
}
