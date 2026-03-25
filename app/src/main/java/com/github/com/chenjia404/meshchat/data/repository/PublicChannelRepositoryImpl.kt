package com.github.com.chenjia404.meshchat.data.repository

import androidx.room.withTransaction
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import com.github.com.chenjia404.meshchat.data.local.db.PublicChannelDatabase
import com.github.com.chenjia404.meshchat.data.local.entity.PublicChannelEntity
import com.github.com.chenjia404.meshchat.data.local.entity.PublicChannelMessageEntity
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatApi
import com.github.com.chenjia404.meshchat.data.remote.dto.CreatePublicChannelBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelAvatarDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelSubscribeBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelSummaryDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelUpsertMessageBodyDto
import com.github.com.chenjia404.meshchat.domain.model.PublicChannel
import com.github.com.chenjia404.meshchat.domain.model.PublicChannelDetail
import com.github.com.chenjia404.meshchat.domain.model.PublicChannelMessage
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import java.io.File
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class DefaultPublicChannelRepository @Inject constructor(
    private val api: MeshChatApi,
    private val database: PublicChannelDatabase,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PublicChannelRepository {

    private val dao get() = database.publicChannelDao()

    override val channels: Flow<List<PublicChannel>> =
        dao.observeChannels().map { list -> list.map { it.toDomain() } }

    override fun observeChannel(channelId: String): Flow<PublicChannel?> =
        dao.observeChannel(channelId).map { it?.toDomain() }

    override fun observeMessages(channelId: String): Flow<List<PublicChannelMessage>> =
        dao.observeMessages(channelId).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshSubscriptions() = withContext(ioDispatcher) {
        val items = api.listPublicChannelSubscriptions().orEmpty()
        database.withTransaction {
            for (summary in items) {
                upsertSummaryFromDto(summary, preservePreviewFromDb = true)
            }
            for (summary in items) {
                runCatching { refreshMessagesInternal(summary.profile.channelId) }
            }
        }
    }

    override suspend fun refreshChannel(channelId: String) = withContext(ioDispatcher) {
        val summary = api.getPublicChannel(channelId)
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = true)
        }
        refreshMessagesInternal(channelId)
    }

    override suspend fun refreshMessages(channelId: String) = withContext(ioDispatcher) {
        refreshMessagesInternal(channelId)
    }

    override suspend fun createChannel(name: String, bio: String): String = withContext(ioDispatcher) {
        val trimmedName = name.trim()
        val summary = api.createPublicChannel(
            CreatePublicChannelBodyDto(
                name = trimmedName,
                bio = bio.trim(),
                avatar = PublicChannelAvatarDto(),
            ),
        )
        val channelId = summary.profile.channelId
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = false)
        }
        runCatching { refreshMessagesInternal(channelId) }
        channelId
    }

    override suspend fun subscribe(channelId: String) = withContext(ioDispatcher) {
        val id = channelId.trim()
        api.subscribePublicChannel(id, PublicChannelSubscribeBodyDto(lastSeenSeq = 0L))
        val summary = api.getPublicChannel(id)
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = false)
        }
        refreshMessagesInternal(id)
    }

    override suspend fun unsubscribe(channelId: String) = withContext(ioDispatcher) {
        val id = channelId.trim()
        api.unsubscribePublicChannel(id)
        database.withTransaction {
            dao.deleteMessages(id)
            dao.deleteChannel(id)
        }
    }

    override suspend fun sendText(channelId: String, text: String) = withContext(ioDispatcher) {
        val msg = api.createPublicChannelMessage(
            channelId.trim(),
            PublicChannelUpsertMessageBodyDto(
                messageType = "text",
                text = text.trim(),
                files = emptyList(),
            ),
        )
        dao.upsertMessages(listOf(messageDtoToEntity(msg)))
        val summary = api.getPublicChannel(channelId.trim())
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = false)
        }
        recomputeChannelMeta(channelId.trim())
    }

    override suspend fun sendFile(channelId: String, file: File, caption: String) = withContext(ioDispatcher) {
        val id = channelId.trim()
        val mime = URLConnection.guessContentTypeFromName(file.name)
            ?: "application/octet-stream"
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody(mime.toMediaTypeOrNull()),
        )
        val msg = api.createPublicChannelFileMessage(
            id,
            part,
            caption.trim().toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull()),
            mime.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull()),
        )
        dao.upsertMessages(listOf(messageDtoToEntity(msg)))
        val summary = api.getPublicChannel(id)
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = false)
        }
        recomputeChannelMeta(id)
    }

    override suspend fun getChannelDetail(channelId: String) = withContext(ioDispatcher) {
        summaryDtoToDetail(api.getPublicChannel(channelId.trim()))
    }

    override suspend fun updateChannelProfile(channelId: String, name: String, bio: String) = withContext(ioDispatcher) {
        val id = channelId.trim()
        val current = api.getPublicChannel(id)
        val keepAvatar = current.profile.avatar ?: PublicChannelAvatarDto()
        val summary = api.updatePublicChannel(
            id,
            CreatePublicChannelBodyDto(
                name = name.trim(),
                bio = bio.trim(),
                avatar = keepAvatar,
            ),
        )
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = true)
        }
    }

    override suspend fun uploadChannelAvatar(channelId: String, file: File) = withContext(ioDispatcher) {
        val id = channelId.trim()
        val part = MultipartBody.Part.createFormData(
            name = "avatar",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        val summary = api.uploadPublicChannelAvatar(id, part)
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = true)
        }
    }

    override suspend fun revokeMessage(channelId: String, messageId: Long) = withContext(ioDispatcher) {
        val id = channelId.trim()
        val msg = api.revokePublicChannelMessage(id, messageId)
        dao.upsertMessages(listOf(messageDtoToEntity(msg)))
        val summary = api.getPublicChannel(id)
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = false)
        }
        recomputeChannelMeta(id)
    }

    override suspend fun updateMessageText(channelId: String, messageId: Long, text: String) = withContext(ioDispatcher) {
        val id = channelId.trim()
        val msg = api.updatePublicChannelMessage(
            id,
            messageId,
            PublicChannelUpsertMessageBodyDto(
                messageType = "text",
                text = text.trim(),
                files = emptyList(),
            ),
        )
        dao.upsertMessages(listOf(messageDtoToEntity(msg)))
        val summary = api.getPublicChannel(id)
        database.withTransaction {
            upsertSummaryFromDto(summary, preservePreviewFromDb = false)
        }
        recomputeChannelMeta(id)
    }

    private fun summaryDtoToDetail(dto: PublicChannelSummaryDto): PublicChannelDetail {
        val p = dto.profile
        val h = dto.head
        val s = dto.sync
        val av = p.avatar
        val blobOrCid = av?.blobId?.takeIf { it.isNotBlank() } ?: av?.cid?.takeIf { it.isNotBlank() }
        val avatarUrl = av?.url?.takeIf { it.isNotBlank() }?.let { attachmentUrlBuilder.absoluteUrlOrNull(it) }
            ?: attachmentUrlBuilder.ipfsBlobAbsoluteUrl(blobOrCid)
        return PublicChannelDetail(
            channelId = p.channelId,
            ownerPeerId = p.ownerPeerId,
            name = p.name,
            bio = p.bio,
            avatarUrl = avatarUrl,
            profileVersion = p.profileVersion,
            ownerVersion = h.ownerVersion,
            lastSeq = h.lastSeq,
            lastMessageId = h.lastMessageId,
            createdAtEpoch = p.createdAt,
            updatedAtEpoch = p.updatedAt,
            subscribed = s.subscribed,
            lastSeenSeq = s.lastSeenSeq,
            lastSyncedSeq = s.lastSyncedSeq,
        )
    }

    private suspend fun refreshMessagesInternal(channelId: String) {
        val page = api.getPublicChannelMessages(channelId, beforeMessageId = null, limit = 50)
        val items = page.items.orEmpty().sortedBy { it.messageId }
        database.withTransaction {
            dao.deleteMessages(channelId)
            if (items.isNotEmpty()) {
                dao.upsertMessages(items.map { messageDtoToEntity(it) })
            }
        }
        recomputeChannelMeta(channelId)
    }

    private suspend fun upsertSummaryFromDto(dto: PublicChannelSummaryDto, preservePreviewFromDb: Boolean) {
        val id = dto.profile.channelId
        val existing = if (preservePreviewFromDb) dao.getChannel(id) else null
        val previewSeed = existing?.lastPreview?.takeIf { it.isNotBlank() } ?: dto.profile.bio
        val entity = summaryToEntity(dto, previewSeed)
        dao.upsertChannel(entity)
    }

    private suspend fun recomputeChannelMeta(channelId: String) {
        val latest = dao.latestMessage(channelId) ?: return
        val ch = dao.getChannel(channelId) ?: return
        val preview = previewTextForMessage(latest)
        val sortMillis = maxOf(ch.lastActivitySortMillis, latest.updatedAtEpoch * 1000)
        dao.upsertChannel(
            ch.copy(
                lastPreview = preview,
                lastActivitySortMillis = sortMillis,
                lastSeq = ch.lastSeq,
            ),
        )
    }

    private fun previewTextForMessage(m: PublicChannelMessageEntity): String {
        if (m.isDeleted) return "该消息已删除"
        return when (m.messageType.lowercase()) {
            "text", "system" -> m.text
            "deleted" -> "该消息已删除"
            "image" -> "[图片]"
            "video" -> "[视频]"
            "audio" -> "[语音]"
            "file" -> m.fileName?.let { "[文件] $it" } ?: "[文件]"
            else -> m.text.ifBlank { m.messageType }
        }
    }

    private fun summaryToEntity(dto: PublicChannelSummaryDto, lastPreviewFallback: String): PublicChannelEntity {
        val p = dto.profile
        val h = dto.head
        val av = p.avatar
        val blobOrCid = av?.blobId?.takeIf { it.isNotBlank() } ?: av?.cid?.takeIf { it.isNotBlank() }
        val avatarUrl = av?.url?.takeIf { it.isNotBlank() }?.let { attachmentUrlBuilder.absoluteUrlOrNull(it) }
            ?: attachmentUrlBuilder.ipfsBlobAbsoluteUrl(blobOrCid)
        val sortMillis = maxOf(p.updatedAt, h.updatedAt).coerceAtLeast(1L) * 1000
        return PublicChannelEntity(
            channelId = p.channelId,
            ownerPeerId = p.ownerPeerId,
            name = p.name,
            bio = p.bio,
            avatarUrl = avatarUrl,
            lastSeq = h.lastSeq,
            lastActivitySortMillis = sortMillis,
            lastPreview = lastPreviewFallback,
        )
    }

    private fun messageDtoToEntity(dto: PublicChannelMessageDto): PublicChannelMessageEntity {
        val first = dto.content.files?.firstOrNull()
        val blobOrCid = first?.blobId?.takeIf { !it.isNullOrBlank() } ?: first?.cid?.takeIf { !it.isNullOrBlank() }
        return PublicChannelMessageEntity(
            channelId = dto.channelId,
            messageId = dto.messageId,
            messageType = dto.messageType,
            text = dto.content.text.orEmpty(),
            fileName = first?.fileName?.takeIf { it.isNotBlank() },
            mimeType = first?.mimeType,
            fileUrl = first?.url?.takeIf { !it.isNullOrBlank() },
            blobId = blobOrCid,
            isDeleted = dto.isDeleted,
            authorPeerId = dto.authorPeerId,
            createdAtEpoch = dto.createdAt,
            updatedAtEpoch = dto.updatedAt,
        )
    }

    private fun PublicChannelEntity.toDomain() = PublicChannel(
        channelId = channelId,
        ownerPeerId = ownerPeerId,
        name = name,
        bio = bio,
        avatarUrl = avatarUrl,
        lastSeq = lastSeq,
        lastActivitySortMillis = lastActivitySortMillis,
        lastPreview = lastPreview,
    )

    private fun PublicChannelMessageEntity.toDomain() = PublicChannelMessage(
        channelId = channelId,
        messageId = messageId,
        messageType = messageType,
        text = text,
        fileName = fileName,
        mimeType = mimeType,
        fileUrl = fileUrl,
        blobId = blobId,
        isDeleted = isDeleted,
        authorPeerId = authorPeerId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
    )
}
