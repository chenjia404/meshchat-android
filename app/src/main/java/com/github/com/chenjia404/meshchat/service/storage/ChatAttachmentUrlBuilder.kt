package com.github.com.chenjia404.meshchat.service.storage

import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatAttachmentUrlBuilder @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    fun avatarUrl(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return "${settingsStore.currentBaseUrl()}api/v1/chat/avatars/$name"
    }

    fun directFileUrl(conversationId: String, msgId: String): String {
        return "${settingsStore.currentBaseUrl()}api/v1/chat/conversations/$conversationId/messages/$msgId/file"
    }

    fun groupFileUrl(groupId: String, msgId: String): String {
        return "${settingsStore.currentBaseUrl()}api/v1/groups/$groupId/messages/$msgId/file"
    }

    /**
     * 本地网关拉取公开频道消息附件（与 meshproxy 群文件路径风格一致）。
     * 若接口未实现 GET，可配合 [absoluteUrlOrNull] 使用消息 JSON 内 `url`。
     */
    fun publicChannelFileUrl(channelId: String, messageId: Long): String {
        return "${settingsStore.currentBaseUrl()}api/v1/public-channels/$channelId/messages/$messageId/file"
    }

    /** 将相对路径或完整 URL 补全为可请求的绝对地址 */
    fun absoluteUrlOrNull(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return null
        if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) {
            return t
        }
        val base = settingsStore.currentBaseUrl().trimEnd('/')
        val path = if (t.startsWith("/")) t else "/$t"
        return "$base$path"
    }

    /**
     * 本地 IPFS 网关：`{baseUrl}ipfs/{blob_id}`（blob_id 与 cid 等价）。
     * 会规整 `ipfs://`、`/ipfs/` 等前缀，只保留哈希段。
     */
    fun ipfsBlobAbsoluteUrl(blobIdOrCid: String?): String? {
        val raw = blobIdOrCid?.trim().orEmpty()
        if (raw.isEmpty()) return null
        var id = raw
        if (id.startsWith("ipfs://", ignoreCase = true)) {
            id = id.removePrefix("ipfs://").removePrefix("IPFS://")
        }
        id = id.trimStart('/')
        if (id.startsWith("ipfs/", ignoreCase = true)) {
            id = id.removePrefix("ipfs/").removePrefix("IPFS/")
        }
        id = id.trimStart('/')
        if (id.isEmpty()) return null
        val base = settingsStore.currentBaseUrl().trimEnd('/')
        return "$base/ipfs/$id"
    }

    /**
     * meshchat-server / meshproxy 网关：`GET /ipfs/{cid}/?filename=...`
     */
    fun ipfsGatewayUrlWithFilename(cid: String?, fileName: String?): String? {
        val base = ipfsBlobAbsoluteUrl(cid) ?: return null
        val fn = fileName?.trim().orEmpty()
        if (fn.isEmpty()) return base
        val q = URLEncoder.encode(fn, StandardCharsets.UTF_8.toString())
        return if (base.contains('?')) "$base&filename=$q" else "$base?filename=$q"
    }

    /**
     * 公开频道附件展示/下载顺序：
     * 1. 本地 `/ipfs/{blob_id}`（blob_id 即 cid）
     * 2. 接口 `content.files[].url`（含相对路径补全）
     * 3. `.../public-channels/.../messages/{id}/file`
     */
    fun resolvePublicChannelMediaUrl(
        channelId: String,
        messageId: Long,
        fileUrlFromApi: String?,
        blobIdOrCid: String?,
    ): String? {
        ipfsBlobAbsoluteUrl(blobIdOrCid)?.let { return it }
        absoluteUrlOrNull(fileUrlFromApi)?.let { return it }
        return publicChannelFileUrl(channelId, messageId)
    }
}

