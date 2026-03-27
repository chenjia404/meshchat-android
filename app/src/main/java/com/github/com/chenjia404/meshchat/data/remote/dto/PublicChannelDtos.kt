package com.github.com.chenjia404.meshchat.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PublicChannelAvatarDto(
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("sha256") val sha256: String? = null,
    /** 与 [cid] 等价，优先走本地 `/ipfs/{blob_id}` */
    @SerializedName("blob_id") val blobId: String? = null,
    @SerializedName("cid") val cid: String? = null,
    @SerializedName("url") val url: String? = null,
)

data class CreatePublicChannelBodyDto(
    @SerializedName("name") val name: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("avatar") val avatar: PublicChannelAvatarDto = PublicChannelAvatarDto(),
)

data class PublicChannelProfileDto(
    @SerializedName("channel_id") val channelId: String,
    @SerializedName("owner_peer_id") val ownerPeerId: String,
    @SerializedName("owner_version") val ownerVersion: Long,
    @SerializedName("name") val name: String,
    @SerializedName("avatar") val avatar: PublicChannelAvatarDto? = null,
    @SerializedName("bio") val bio: String,
    @SerializedName("profile_version") val profileVersion: Long,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("signature") val signature: String? = null,
)

data class PublicChannelHeadDto(
    @SerializedName("channel_id") val channelId: String,
    @SerializedName("owner_peer_id") val ownerPeerId: String,
    @SerializedName("owner_version") val ownerVersion: Long,
    @SerializedName("last_message_id") val lastMessageId: Long,
    @SerializedName("profile_version") val profileVersion: Long,
    @SerializedName("last_seq") val lastSeq: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("signature") val signature: String? = null,
)

data class PublicChannelSyncStateDto(
    @SerializedName("channel_id") val channelId: String,
    @SerializedName("last_seen_seq") val lastSeenSeq: Long,
    @SerializedName("last_synced_seq") val lastSyncedSeq: Long,
    @SerializedName("latest_loaded_message_id") val latestLoadedMessageId: Long,
    @SerializedName("oldest_loaded_message_id") val oldestLoadedMessageId: Long,
    @SerializedName("subscribed") val subscribed: Boolean,
    @SerializedName("updated_at") val updatedAt: Long,
    /** 订阅列表等接口返回的本地未读数，用于会话角标 */
    @SerializedName("unread_count") val unreadCount: Int? = null,
)

data class PublicChannelSummaryDto(
    @SerializedName("profile") val profile: PublicChannelProfileDto,
    @SerializedName("head") val head: PublicChannelHeadDto,
    @SerializedName("sync") val sync: PublicChannelSyncStateDto,
)

data class PublicChannelSubscribeBodyDto(
    @SerializedName("peer_id") val peerId: String? = null,
    @SerializedName("peer_ids") val peerIds: List<String>? = null,
    @SerializedName("last_seen_seq") val lastSeenSeq: Long = 0L,
)

data class PublicChannelFileDto(
    @SerializedName("file_id") val fileId: String? = null,
    @SerializedName("file_name") val fileName: String = "",
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("sha256") val sha256: String? = null,
    /** 与 [cid] 等价，优先走本地 `/ipfs/{blob_id}` */
    @SerializedName("blob_id") val blobId: String? = null,
    @SerializedName("cid") val cid: String? = null,
    @SerializedName("url") val url: String? = null,
)

data class PublicChannelMessageContentDto(
    @SerializedName("text") val text: String? = null,
    @SerializedName("files") val files: List<PublicChannelFileDto>? = null,
)

data class PublicChannelMessageDto(
    @SerializedName("channel_id") val channelId: String,
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("seq") val seq: Long,
    @SerializedName("owner_version") val ownerVersion: Long,
    @SerializedName("creator_peer_id") val creatorPeerId: String,
    @SerializedName("author_peer_id") val authorPeerId: String,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("is_deleted") val isDeleted: Boolean,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("content") val content: PublicChannelMessageContentDto,
    @SerializedName("signature") val signature: String? = null,
)

data class PublicChannelMessagesPageDto(
    @SerializedName("channel_id") val channelId: String,
    @SerializedName("items") val items: List<PublicChannelMessageDto>?,
)

data class PublicChannelUpsertMessageBodyDto(
    @SerializedName("message_type") val messageType: String? = "text",
    @SerializedName("text") val text: String,
    @SerializedName("files") val files: List<PublicChannelFileDto> = emptyList(),
)

data class PublicChannelSubscribeResultDto(
    @SerializedName("profile") val profile: PublicChannelProfileDto,
    @SerializedName("head") val head: PublicChannelHeadDto,
    @SerializedName("messages") val messages: List<PublicChannelMessageDto>?,
    @SerializedName("providers") val providers: List<Any>? = null,
)
