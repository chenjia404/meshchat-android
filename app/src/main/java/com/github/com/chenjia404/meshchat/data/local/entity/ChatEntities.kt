package com.github.com.chenjia404.meshchat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val peerId: String,
    val nickname: String,
    val bio: String,
    val avatar: String?,
    val avatarCid: String?,
    val chatKexPub: String?,
    val createdAt: String,
)

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey val requestId: String,
    val fromPeerId: String,
    val toPeerId: String,
    val state: String,
    val introText: String,
    val nickname: String,
    val bio: String,
    val avatar: String?,
    val retentionMinutes: Int,
    val remoteChatKexPub: String?,
    val conversationId: String?,
    val lastTransportMode: String?,
    val retryCount: Int?,
    val nextRetryAt: String?,
    val retryJobStatus: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val peerId: String,
    val nickname: String,
    val bio: String,
    val avatar: String?,
    val cid: String?,
    val remoteNickname: String?,
    val retentionMinutes: Int,
    val blocked: Boolean,
    val lastSeenAt: String?,
    val updatedAt: String,
)

@Entity(tableName = "direct_conversations")
data class DirectConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerId: String,
    val state: String,
    val lastMessageAt: String?,
    val lastTransportMode: String?,
    val unreadCount: Int,
    val retentionMinutes: Int,
    val retentionSyncState: String?,
    val retentionSyncedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val msgId: String,
    val conversationId: String,
    val senderPeerId: String,
    val receiverPeerId: String,
    val direction: String,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val transportMode: String?,
    val state: String,
    val counter: Long?,
    val createdAt: String,
    val deliveredAt: String?,
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val title: String,
    val avatar: String?,
    val controllerPeerId: String,
    val currentEpoch: Long,
    val retentionMinutes: Int,
    val state: String,
    val lastEventSeq: Long,
    val lastMessageAt: String?,
    val memberCount: Int?,
    val localMemberRole: String?,
    val localMemberState: String?,
    val createdAt: String,
    val updatedAt: String,
    /** `true` 表示 meshchat-server 超级群（路径 `/groups/...`），与普通 meshproxy 群区分 */
    val isSuperGroup: Boolean = false,
    /** 超级群 HTTP API 根地址（如 `https://chat-api.example.com/`），可与设置中的 meshproxy 不同 */
    val superGroupApiBaseUrl: String? = null,
)

@Entity(tableName = "group_messages")
data class GroupMessageEntity(
    @PrimaryKey val msgId: String,
    val groupId: String,
    val epoch: Long?,
    val senderPeerId: String,
    /** 超级群展示用（meshchat-server 无 peer_id，存 display_name / username） */
    val senderLabel: String?,
    val senderSeq: Long?,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val signature: String?,
    val state: String,
    val deliveryTotal: Int?,
    val deliveryPending: Int?,
    val deliverySentToTransport: Int?,
    val deliveryQueuedForRetry: Int?,
    val deliveryDeliveredRemote: Int?,
    val deliveryFailed: Int?,
    val createdAt: String,
)

@Entity(tableName = "chat_events")
data class ChatEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val kind: String?,
    val conversationId: String?,
    val msgId: String?,
    val msgType: String?,
    val requestId: String?,
    val fromPeerId: String?,
    val toPeerId: String?,
    val state: String?,
    val atUnixMillis: Long,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val senderPeerId: String?,
    val receiverPeerId: String?,
    val direction: String?,
    val counter: Long?,
    val transportMode: String?,
    val messageState: String?,
    val createdAtUnixMillis: Long?,
    val deliveredAtUnixMillis: Long?,
    val epoch: Long?,
    val senderSeq: Long?,
    val deliveryTotal: Int?,
    val deliveryPending: Int?,
    val deliverySentToTransport: Int?,
    val deliveryQueuedForRetry: Int?,
    val deliveryDeliveredRemote: Int?,
    val deliveryFailed: Int?,
)

