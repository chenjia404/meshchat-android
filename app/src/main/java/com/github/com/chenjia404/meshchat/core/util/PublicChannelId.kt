package com.github.com.chenjia404.meshchat.core.util

import java.util.UUID

/**
 * 公开频道 ID：`owner_peer_id`（libp2p peer_id）+ `:` + UUIDv7。
 * 仅允许 **一个** 冒号，左侧为 owner，右侧为 UUID。
 */
object PublicChannelIdValidator {

    fun isValid(raw: String): Boolean {
        val t = raw.trim()
        if (t.isEmpty()) return false
        val idx = t.indexOf(':')
        if (idx <= 0 || idx >= t.length - 1) return false
        val ownerPeerId = t.substring(0, idx).trim()
        val uuidPart = t.substring(idx + 1).trim()
        if (ownerPeerId.isEmpty() || !isLikelyLibp2pPeerId(ownerPeerId)) return false
        return runCatching {
            val u = UUID.fromString(uuidPart)
            u.version() == 7
        }.getOrDefault(false)
    }

    /**
     * 宽松校验：长度与字符集与「不含冒号」——避免误把多段当频道 ID。
     * libp2p peer id 多为 base58/base32 等可打印串，不含空白。
     */
    private fun isLikelyLibp2pPeerId(s: String): Boolean {
        if (s.length < 32 || s.length > 512) return false
        if (':' in s) return false
        return s.all { !it.isWhitespace() }
    }
}
