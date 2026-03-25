package com.github.com.chenjia404.meshchat.domain.model

/** 私聊/群聊消息方向，与接口字段 `direction` 一致：`inbound` / `outbound` */
object MessageDirection {
    const val INBOUND = "inbound"
    const val OUTBOUND = "outbound"
}

data class Profile(
    val peerId: String,
    val nickname: String,
    val bio: String,
    val avatar: String?,
    val avatarCid: String?,
    val chatKexPub: String?,
    val createdAt: String,
)

data class Contact(
    val peerId: String,
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

data class FriendRequest(
    val requestId: String,
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

data class DirectConversation(
    val conversationId: String,
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

data class DirectMessage(
    val msgId: String,
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

data class Group(
    val groupId: String,
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
)

data class GroupDeliverySummary(
    val total: Int,
    val pending: Int,
    val sentToTransport: Int,
    val queuedForRetry: Int,
    val deliveredRemote: Int,
    val failed: Int,
)

data class GroupMessage(
    val msgId: String,
    val groupId: String,
    val epoch: Long?,
    val senderPeerId: String,
    val senderSeq: Long?,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val signature: String?,
    val state: String,
    val deliverySummary: GroupDeliverySummary?,
    val createdAt: String,
)

data class ChatEvent(
    val id: Long = 0L,
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
    val deliverySummary: GroupDeliverySummary?,
)

data class PublicChannel(
    val channelId: String,
    val ownerPeerId: String,
    val name: String,
    val bio: String,
    val avatarUrl: String?,
    val lastSeq: Long,
    val lastActivitySortMillis: Long,
    val lastPreview: String,
)

/** 公开频道简介页：接口聚合 profile / head / sync */
data class PublicChannelDetail(
    val channelId: String,
    val ownerPeerId: String,
    val name: String,
    val bio: String,
    val avatarUrl: String?,
    val profileVersion: Long,
    val ownerVersion: Long,
    val lastSeq: Long,
    val lastMessageId: Long,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    val subscribed: Boolean,
    val lastSeenSeq: Long,
    val lastSyncedSeq: Long,
)

data class PublicChannelMessage(
    val channelId: String,
    val messageId: Long,
    val messageType: String,
    val text: String,
    val fileName: String?,
    val mimeType: String?,
    val fileUrl: String?,
    val blobId: String?,
    val isDeleted: Boolean,
    val authorPeerId: String,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
)

