package com.github.com.chenjia404.meshchat.data.mapper

import com.github.com.chenjia404.meshchat.data.local.entity.GroupEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupMessageEntity
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerGroupDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerUserDto
import com.google.gson.JsonObject

fun MeshChatServerGroupDto.toGroupEntity(superGroupApiBaseUrl: String): GroupEntity {
    val now = updatedAt ?: createdAt ?: ""
    return GroupEntity(
        groupId = groupId,
        title = title,
        avatar = avatarCid,
        controllerPeerId = "",
        currentEpoch = 0L,
        retentionMinutes = 0,
        state = status ?: "active",
        lastEventSeq = lastMessageSeq ?: 0L,
        lastMessageAt = updatedAt,
        memberCount = null,
        localMemberRole = null,
        localMemberState = null,
        createdAt = createdAt ?: now,
        updatedAt = now,
        isSuperGroup = true,
        superGroupApiBaseUrl = superGroupApiBaseUrl,
    )
}

private fun MeshChatServerUserDto?.senderKey(): String =
    this?.id?.let { "meshchat_user_$it" } ?: "meshchat_unknown"

private fun MeshChatServerUserDto?.senderLabel(): String? =
    when {
        this == null -> null
        !displayName.isNullOrBlank() -> displayName
        !username.isNullOrBlank() -> username
        id != null -> "user_$id"
        else -> null
    }

/**
 * 将 meshchat-server 消息映射为本地群消息实体（[msgType] 使用 `group_chat_text` / `group_chat_file` 供 [resolveRenderType]）。
 */
fun MeshChatServerMessageDto.toGroupMessageEntity(): GroupMessageEntity {
    val p: JsonObject? = payload?.asJsonObject
    val senderId = sender.senderKey()
    val label = sender.senderLabel()
    val st = when (status) {
        "deleted" -> "revoked"
        else -> "normal"
    }
    return when (contentType) {
        "text" -> {
            val text = p?.get("text")?.asString ?: ""
            GroupMessageEntity(
                msgId = messageId,
                groupId = groupId,
                epoch = seq,
                senderPeerId = senderId,
                senderLabel = label,
                senderSeq = seq,
                msgType = "group_chat_text",
                plaintext = text,
                fileName = null,
                mimeType = null,
                fileSize = null,
                fileCid = null,
                signature = null,
                state = st,
                deliveryTotal = null,
                deliveryPending = null,
                deliverySentToTransport = null,
                deliveryQueuedForRetry = null,
                deliveryDeliveredRemote = null,
                deliveryFailed = null,
                createdAt = createdAt.orEmpty(),
            )
        }
        "forward" -> {
            val comment = p?.get("comment")?.asString ?: ""
            GroupMessageEntity(
                msgId = messageId,
                groupId = groupId,
                epoch = seq,
                senderPeerId = senderId,
                senderLabel = label,
                senderSeq = seq,
                msgType = "group_chat_text",
                plaintext = comment,
                fileName = null,
                mimeType = null,
                fileSize = null,
                fileCid = null,
                signature = null,
                state = st,
                deliveryTotal = null,
                deliveryPending = null,
                deliverySentToTransport = null,
                deliveryQueuedForRetry = null,
                deliveryDeliveredRemote = null,
                deliveryFailed = null,
                createdAt = createdAt.orEmpty(),
            )
        }
        "image", "video", "voice", "file" -> {
            val cid = p?.get("cid")?.asString
            val mime = p?.get("mime_type")?.asString
            val size = p?.get("size")?.asLong
            val cap = p?.get("caption")?.asString
            val fn = p?.get("file_name")?.asString
            val name = fn ?: when (contentType) {
                "image" -> "image"
                "video" -> "video"
                "voice" -> "voice"
                else -> "file"
            }
            GroupMessageEntity(
                msgId = messageId,
                groupId = groupId,
                epoch = seq,
                senderPeerId = senderId,
                senderLabel = label,
                senderSeq = seq,
                msgType = "group_chat_file",
                plaintext = cap,
                fileName = name,
                mimeType = mime,
                fileSize = size,
                fileCid = cid,
                signature = null,
                state = st,
                deliveryTotal = null,
                deliveryPending = null,
                deliverySentToTransport = null,
                deliveryQueuedForRetry = null,
                deliveryDeliveredRemote = null,
                deliveryFailed = null,
                createdAt = createdAt.orEmpty(),
            )
        }
        else -> {
            GroupMessageEntity(
                msgId = messageId,
                groupId = groupId,
                epoch = seq,
                senderPeerId = senderId,
                senderLabel = label,
                senderSeq = seq,
                msgType = "system",
                plaintext = "[$contentType]",
                fileName = null,
                mimeType = null,
                fileSize = null,
                fileCid = null,
                signature = null,
                state = st,
                deliveryTotal = null,
                deliveryPending = null,
                deliverySentToTransport = null,
                deliveryQueuedForRetry = null,
                deliveryDeliveredRemote = null,
                deliveryFailed = null,
                createdAt = createdAt.orEmpty(),
            )
        }
    }
}
