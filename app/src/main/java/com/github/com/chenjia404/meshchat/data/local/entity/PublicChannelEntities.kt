package com.github.com.chenjia404.meshchat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "public_channels",
    indices = [Index(value = ["lastActivitySortMillis"], name = "idx_public_channels_sort")],
)
data class PublicChannelEntity(
    @PrimaryKey val channelId: String,
    val ownerPeerId: String,
    val name: String,
    val bio: String,
    val avatarUrl: String?,
    val lastSeq: Long,
    /** 用于与会话列表合并排序（毫秒） */
    val lastActivitySortMillis: Long,
    val lastPreview: String,
)

@Entity(
    tableName = "public_channel_messages",
    primaryKeys = ["channelId", "messageId"],
    indices = [
        Index(value = ["channelId", "messageId"], name = "idx_pcm_channel_msg"),
    ],
)
data class PublicChannelMessageEntity(
    val channelId: String,
    val messageId: Long,
    val messageType: String,
    val text: String,
    val fileName: String?,
    val mimeType: String?,
    val fileUrl: String?,
    /** 附件 blob，与 cid 等价；展示时优先 `{base}ipfs/{blobId}` */
    val blobId: String?,
    val isDeleted: Boolean,
    val authorPeerId: String,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
)
