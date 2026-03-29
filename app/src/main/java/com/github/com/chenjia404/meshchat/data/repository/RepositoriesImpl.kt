package com.github.com.chenjia404.meshchat.data.repository

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.core.util.MeshchatHttpErrors
import com.github.com.chenjia404.meshchat.core.util.superGroupApiRoot
import com.github.com.chenjia404.meshchat.core.util.joinSuperGroup
import com.github.com.chenjia404.meshchat.core.util.meGroups
import com.github.com.chenjia404.meshchat.core.util.normalizeMeshChatServerBaseUrl
import com.github.com.chenjia404.meshchat.core.util.parseSuperGroupInviteUrl
import com.github.com.chenjia404.meshchat.core.util.superGroupDetail
import com.github.com.chenjia404.meshchat.core.util.superGroupIpfsAdd
import com.github.com.chenjia404.meshchat.core.util.userProfileByPeerId
import com.github.com.chenjia404.meshchat.core.util.superGroupLeave
import com.github.com.chenjia404.meshchat.core.util.superGroupMembersInvite
import com.github.com.chenjia404.meshchat.core.util.superGroupMembersList
import com.github.com.chenjia404.meshchat.core.util.superGroupMessages
import com.github.com.chenjia404.meshchat.core.util.superGroupPostMessage
import com.github.com.chenjia404.meshchat.core.util.superGroupRegisterFile
import com.github.com.chenjia404.meshchat.core.util.superGroupRetract
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import com.github.com.chenjia404.meshchat.core.util.toSuperGroupBaseHttpUrl
import com.github.com.chenjia404.meshchat.data.local.db.AppDatabase
import com.github.com.chenjia404.meshchat.data.local.db.PublicChannelDatabase
import com.github.com.chenjia404.meshchat.core.util.looksLikeVoiceAttachment
import com.github.com.chenjia404.meshchat.data.mapper.toDomain
import com.github.com.chenjia404.meshchat.data.mapper.toEntity
import com.github.com.chenjia404.meshchat.data.mapper.toGroupEntity
import com.github.com.chenjia404.meshchat.data.mapper.toGroupMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupEntity
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatApi
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatServerDirectApi
import com.github.com.chenjia404.meshchat.data.remote.dto.ChatEventDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ContactBlockBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ContactNicknameBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.CreateGroupBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.DirectMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.DirectMessagesPageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupControllerBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupInviteBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupReasonBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupRemoveBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupSyncBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupTitleBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.RetentionBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SendFriendRequestBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SendTextBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerGroupDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerInviteMembersBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerPatchUserProfileBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerPostMessageBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerRegisterFileBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.UpdateProfileBodyDto
import com.github.com.chenjia404.meshchat.data.remote.ws.MeshChatServerWebSocket
import com.github.com.chenjia404.meshchat.data.remote.ws.MeshChatSocket
import com.github.com.chenjia404.meshchat.service.notification.LocalChatNotifier
import com.github.com.chenjia404.meshchat.domain.model.ChatEvent
import com.github.com.chenjia404.meshchat.domain.model.Contact
import com.github.com.chenjia404.meshchat.domain.model.DirectConversation
import com.github.com.chenjia404.meshchat.domain.model.DirectMessage
import com.github.com.chenjia404.meshchat.domain.model.FriendRequest
import com.github.com.chenjia404.meshchat.domain.model.Group
import com.github.com.chenjia404.meshchat.domain.model.GroupMessage
import com.github.com.chenjia404.meshchat.domain.model.Profile
import com.github.com.chenjia404.meshchat.domain.model.SuperGroupIntroSnapshot
import com.github.com.chenjia404.meshchat.domain.repository.ChatEventsRepository
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.domain.repository.SettingsRepository
import com.github.com.chenjia404.meshchat.service.meshchat.MeshChatServerSessionManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.net.URLConnection
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@Singleton
class DefaultSettingsRepository @Inject constructor(
    private val settingsStore: SettingsStore,
) : SettingsRepository {
    override val baseUrl: Flow<String> = settingsStore.baseUrlFlow
    override suspend fun setBaseUrl(value: String) = settingsStore.setBaseUrl(value)
    override fun currentBaseUrl(): String = settingsStore.currentBaseUrl()
}

@Singleton
class DefaultProfileRepository @Inject constructor(
    private val api: MeshChatApi,
    private val database: AppDatabase,
    private val groupRepository: GroupRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {
    override val myProfile: Flow<Profile?> = database.profileDao().observeProfile().map { it?.toDomain() }

    override suspend fun refreshMyProfile() = withContext(ioDispatcher) {
        val dto = api.getMe() ?: return@withContext
        database.profileDao().upsert(dto.toDomain().toEntity())
    }

    override suspend fun refreshProfile() = withContext(ioDispatcher) {
        val dto = api.getProfile() ?: return@withContext
        database.profileDao().upsert(dto.toDomain().toEntity())
    }

    override suspend fun updateProfile(nickname: String, bio: String) = withContext(ioDispatcher) {
        database.profileDao().upsert(api.updateProfile(UpdateProfileBodyDto(nickname, bio)).toDomain().toEntity())
        runCatching { groupRepository.pushLocalProfileToAllKnownMeshChatServers() }
        Unit
    }

    override suspend fun uploadAvatar(file: File) = withContext(ioDispatcher) {
        val avatar = MultipartBody.Part.createFormData(
            name = "avatar",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        database.profileDao().upsert(api.uploadAvatar(avatar).toDomain().toEntity())
        runCatching { groupRepository.pushLocalProfileToAllKnownMeshChatServers() }
        Unit
    }
}

@Singleton
class DefaultContactsRepository @Inject constructor(
    private val api: MeshChatApi,
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ContactsRepository {
    override val contacts: Flow<List<Contact>> = database.contactDao().observeContacts().map { list -> list.map { it.toDomain() } }
    override val requests: Flow<List<FriendRequest>> = database.friendRequestDao().observeRequests().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshContacts() = withContext(ioDispatcher) {
        val contacts = api.getContacts().orEmpty().map { it.toDomain().toEntity() }
        database.withTransaction {
            database.contactDao().clearAll()
            database.contactDao().upsertAll(contacts)
        }
    }

    override suspend fun refreshRequests() = withContext(ioDispatcher) {
        val requests = api.getRequests().orEmpty().map { it.toDomain().toEntity() }
        database.withTransaction {
            database.friendRequestDao().clearAll()
            database.friendRequestDao().upsertAll(requests)
        }
    }

    override suspend fun sendFriendRequest(toPeerId: String, introText: String) = withContext(ioDispatcher) {
        database.friendRequestDao().upsertAll(
            listOf(api.sendRequest(SendFriendRequestBodyDto(toPeerId, introText)).toDomain().toEntity()),
        )
    }

    override suspend fun acceptRequest(requestId: String) = withContext(ioDispatcher) {
        database.withTransaction {
            database.directConversationDao().upsert(api.acceptRequest(requestId).toDomain().toEntity())
            database.friendRequestDao().deleteById(requestId)
        }
        refreshContacts()
    }

    override suspend fun rejectRequest(requestId: String) = withContext(ioDispatcher) {
        api.rejectRequest(requestId)
        database.friendRequestDao().deleteById(requestId)
    }

    override suspend fun deleteContact(peerId: String) = withContext(ioDispatcher) {
        api.deleteContact(peerId)
        database.contactDao().deleteById(peerId)
    }

    override suspend fun updateContactNickname(peerId: String, nickname: String) = withContext(ioDispatcher) {
        database.contactDao().upsert(api.updateContactNickname(peerId, ContactNicknameBodyDto(nickname)).toDomain().toEntity())
    }

    override suspend fun setBlocked(peerId: String, blocked: Boolean) = withContext(ioDispatcher) {
        database.contactDao().upsert(api.setBlocked(peerId, ContactBlockBodyDto(blocked)).toDomain().toEntity())
    }
}

@Singleton
class DefaultDirectChatRepository @Inject constructor(
    private val api: MeshChatApi,
    private val gson: Gson,
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DirectChatRepository {
    override val conversations: Flow<List<DirectConversation>> =
        database.directConversationDao().observeConversations().map { list -> list.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<DirectMessage>> =
        database.directMessageDao().observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    override fun observeLatestMessage(conversationId: String): Flow<DirectMessage?> =
        database.directMessageDao().observeLatestMessage(conversationId).map { it?.toDomain() }

    override fun observeConversation(conversationId: String): Flow<DirectConversation?> =
        database.directConversationDao().observeConversation(conversationId).map { it?.toDomain() }

    override suspend fun refreshConversations() = withContext(ioDispatcher) {
        val items = api.getConversations().orEmpty().map { it.toDomain().toEntity() }
        database.withTransaction {
            database.directConversationDao().clearAll()
            database.directConversationDao().upsertAll(items)
        }
    }

    override suspend fun refreshMessages(conversationId: String, limit: Int?, offset: Int?) = withContext(ioDispatcher) {
        val messages = parseDirectMessages(api.getConversationMessages(conversationId, limit, offset))
        database.withTransaction {
            if (limit == null && offset == null) {
                database.directMessageDao().clearConversation(conversationId)
            }
            database.directMessageDao().upsertAll(messages.map { it.toDomain().toEntity() })
        }
    }

    override suspend fun sendText(conversationId: String, text: String) = withContext(ioDispatcher) {
        database.directMessageDao().upsert(api.sendConversationMessage(conversationId, SendTextBodyDto(text)).toDomain().toEntity())
        refreshConversations()
    }

    override suspend fun sendFile(conversationId: String, file: File) = withContext(ioDispatcher) {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        database.directMessageDao().upsert(api.sendConversationFile(conversationId, part).toDomain().toEntity())
        refreshConversations()
    }

    override suspend fun markRead(conversationId: String) = withContext(ioDispatcher) {
        database.directConversationDao().upsert(api.markConversationRead(conversationId).toDomain().toEntity())
    }

    override suspend fun syncConversation(conversationId: String) = withContext(ioDispatcher) {
        api.syncConversation(conversationId)
        Unit
    }

    override suspend fun updateRetention(conversationId: String, retentionMinutes: Int) = withContext(ioDispatcher) {
        database.directConversationDao().upsert(
            api.updateConversationRetention(conversationId, RetentionBodyDto(retentionMinutes)).toDomain().toEntity(),
        )
    }

    override suspend fun patchLocalRetentionMinutes(conversationId: String, retentionMinutes: Int) =
        withContext(ioDispatcher) {
            val updatedAt = java.time.Instant.now().toString()
            database.directConversationDao().updateRetentionMinutes(conversationId, retentionMinutes, updatedAt)
        }

    override suspend fun revokeMessage(conversationId: String, msgId: String) = withContext(ioDispatcher) {
        api.revokeConversationMessage(conversationId, msgId)
        refreshMessages(conversationId)
    }

    override suspend fun deleteConversation(conversationId: String) = withContext(ioDispatcher) {
        api.deleteConversation(conversationId)
        database.directConversationDao().deleteById(conversationId)
    }

    private fun parseDirectMessages(element: JsonElement): List<DirectMessageDto> {
        if (element.isJsonNull) return emptyList()
        return if (element.isJsonArray) {
            gson.fromJson(
                element,
                Array<DirectMessageDto>::class.java,
            ).toList()
        } else {
            gson.fromJson(element, DirectMessagesPageDto::class.java)?.messages.orEmpty()
        }
    }
}

@Singleton
class DefaultGroupRepository @Inject constructor(
    private val api: MeshChatApi,
    private val meshChatServerDirectApi: MeshChatServerDirectApi,
    private val meshChatServerSession: MeshChatServerSessionManager,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GroupRepository {
    private fun superGroupBaseOrThrow(group: GroupEntity?): HttpUrl {
        val s = group?.superGroupApiBaseUrl?.takeIf { it.isNotBlank() }
            ?: error("missing_super_group_base")
        return s.toSuperGroupBaseHttpUrl()
    }
    override val groups: Flow<List<Group>> = database.groupDao().observeGroups().map { list -> list.map { it.toDomain() } }

    override fun observeGroup(groupId: String): Flow<Group?> = database.groupDao().observeGroup(groupId).map { it?.toDomain() }

    override fun observeMessages(groupId: String): Flow<List<GroupMessage>> =
        database.groupMessageDao().observeMessages(groupId).map { list -> list.map { it.toDomain() } }

    override fun observeLatestGroupMessage(groupId: String): Flow<GroupMessage?> =
        database.groupMessageDao().observeLatestMessage(groupId).map { it?.toDomain() }

    override suspend fun refreshGroups() = withContext(ioDispatcher) {
        val items = api.getGroups().body().orEmpty().map { it.toDomain().toEntity() }
        database.withTransaction {
            database.groupDao().deleteMeshProxyGroupsOnly()
            database.groupDao().upsertAll(items)
        }
    }

    override suspend fun refreshGroup(groupId: String) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) {
            val base = superGroupBaseOrThrow(existing)
            meshChatServerSession.ensureSession(base)
            val g = meshChatServerDirectApi.getGroup(base.superGroupDetail(groupId).toString())
            val baseStr = existing.superGroupApiBaseUrl?.takeIf { it.isNotBlank() }
                ?: settingsStore.currentBaseUrl()
            database.groupDao().upsert(
                g.toGroupEntity(baseStr, localUnreadCount = existing?.localUnreadCount ?: 0),
            )
        } else {
            database.groupDao().upsert(api.getGroup(groupId).group.toDomain().toEntity())
        }
    }

    override suspend fun refreshMessages(groupId: String) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) {
            val base = superGroupBaseOrThrow(existing)
            meshChatServerSession.ensureSession(base)
            val items = meshChatServerDirectApi.getMessages(
                base.superGroupMessages(groupId, beforeSeq = null, limit = 100).toString(),
            ).orEmpty()
                .map { it.toGroupMessageEntity() }
            database.withTransaction {
                database.groupMessageDao().clearGroup(groupId)
                database.groupMessageDao().upsertAll(items)
            }
        } else {
            val items = api.getGroupMessages(groupId).orEmpty().map { it.toDomain().toEntity() }
            database.withTransaction {
                database.groupMessageDao().clearGroup(groupId)
                database.groupMessageDao().upsertAll(items)
            }
        }
    }

    override suspend fun createGroup(title: String, members: List<String>) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.createGroup(CreateGroupBodyDto(title, members)).toDomain().toEntity())
        refreshGroups()
    }

    override suspend fun invite(groupId: String, peerId: String, role: String, inviteText: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.invite(groupId, GroupInviteBodyDto(peerId, role, inviteText)).toDomain().toEntity())
    }

    override suspend fun join(groupId: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.join(groupId).toDomain().toEntity())
    }

    override suspend fun leave(groupId: String, reason: String) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) {
            leaveSuperGroupBestEffortRemoteThenAlwaysDeleteLocal(groupId, existing)
        } else {
            database.groupDao().upsert(api.leave(groupId, GroupReasonBodyDto(reason)).toDomain().toEntity())
        }
    }

    /**
     * 超级群退出：尽力 [postLeaveGroup]；无论远端是否成功（含 502、鉴权失败），均删除本地群与消息，避免无法退出。
     */
    private suspend fun leaveSuperGroupBestEffortRemoteThenAlwaysDeleteLocal(
        groupId: String,
        existing: GroupEntity,
    ) {
        val baseStr = existing.superGroupApiBaseUrl?.takeIf { it.isNotBlank() }
        if (baseStr != null) {
            runCatching {
                val base = baseStr.toSuperGroupBaseHttpUrl()
                meshChatServerSession.ensureSession(base)
                meshChatServerDirectApi.postLeaveGroup(base.superGroupLeave(groupId).toString())
            }.onFailure { e ->
                MeshchatHttpErrors.log("leaveSuperGroup", e)
            }
        }
        database.withTransaction {
            database.groupMessageDao().clearGroup(groupId)
            database.groupDao().deleteByGroupId(groupId)
        }
    }

    override suspend fun remove(groupId: String, peerId: String, reason: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.remove(groupId, GroupRemoveBodyDto(peerId, reason)).toDomain().toEntity())
    }

    override suspend fun updateTitle(groupId: String, title: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.updateTitle(groupId, GroupTitleBodyDto(title)).toDomain().toEntity())
    }

    override suspend fun updateRetention(groupId: String, retentionMinutes: Int) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.updateGroupRetention(groupId, RetentionBodyDto(retentionMinutes)).toDomain().toEntity())
    }

    override suspend fun patchLocalRetentionMinutes(groupId: String, retentionMinutes: Int) = withContext(ioDispatcher) {
        val updatedAt = java.time.Instant.now().toString()
        database.groupDao().updateRetentionMinutes(groupId, retentionMinutes, updatedAt)
    }

    override suspend fun dissolve(groupId: String, reason: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.dissolve(groupId, GroupReasonBodyDto(reason)).toDomain().toEntity())
    }

    override suspend fun changeController(groupId: String, peerId: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.changeController(groupId, GroupControllerBodyDto(peerId)).toDomain().toEntity())
    }

    override suspend fun sendText(groupId: String, text: String) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) {
            val base = superGroupBaseOrThrow(existing)
            meshChatServerSession.ensureSession(base)
            val body = MeshChatServerPostMessageBodyDto(
                contentType = "text",
                payload = gson.toJsonTree(mapOf("text" to text)),
            )
            val msg = meshChatServerDirectApi.postMessage(base.superGroupPostMessage(groupId).toString(), body)
            msg.sender?.id?.let { settingsStore.setMeshChatServerUserId(it) }
            database.groupMessageDao().upsert(msg.toGroupMessageEntity())
            refreshGroup(groupId)
        } else {
            database.groupMessageDao().upsert(api.sendGroupMessage(groupId, SendTextBodyDto(text)).toDomain().toEntity())
            refreshGroups()
        }
    }

    override suspend fun sendFile(groupId: String, file: File) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) {
            sendSuperGroupFile(groupId, file)
            refreshGroup(groupId)
        } else {
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
            )
            database.groupMessageDao().upsert(api.sendGroupFile(groupId, part).toDomain().toEntity())
            refreshGroups()
        }
    }

    private suspend fun sendSuperGroupFile(groupId: String, file: File) {
        val gEntity = database.groupDao().getGroupOnce(groupId) ?: error("group_not_found")
        val base = superGroupBaseOrThrow(gEntity)
        meshChatServerSession.ensureSession(base)
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        val ipfs = meshChatServerDirectApi.postIpfsAdd(base.superGroupIpfsAdd().toString(), part)
        val cid = ipfs.cid ?: error("ipfs_no_cid")
        val size = ipfs.size ?: file.length()
        val mime = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        meshChatServerDirectApi.postRegisterFile(
            base.superGroupRegisterFile().toString(),
            MeshChatServerRegisterFileBodyDto(
                cid = cid,
                mimeType = mime,
                size = size,
                fileName = file.name,
            ),
        )
        val contentType = when {
            mime.startsWith("image/") -> "image"
            mime.startsWith("video/") -> "video"
            looksLikeVoiceAttachment(mime, file.name) -> "voice"
            mime.startsWith("audio/") -> "file"
            else -> "file"
        }
        val payload = buildSuperGroupMediaPayload(contentType, cid, mime, size, file.name)
        val body = MeshChatServerPostMessageBodyDto(
            contentType = contentType,
            payload = payload,
        )
        val msg = meshChatServerDirectApi.postMessage(base.superGroupPostMessage(groupId).toString(), body)
        msg.sender?.id?.let { settingsStore.setMeshChatServerUserId(it) }
        database.groupMessageDao().upsert(msg.toGroupMessageEntity())
    }

    private fun buildSuperGroupMediaPayload(
        contentType: String,
        cid: String,
        mime: String,
        size: Long,
        fileName: String,
    ): JsonElement {
        val o = JsonObject()
        o.addProperty("cid", cid)
        o.addProperty("mime_type", mime)
        o.addProperty("size", size)
        when (contentType) {
            "image", "video" -> {
                o.addProperty("caption", "")
                o.addProperty("thumbnail_cid", "")
            }
            "voice" -> {
                o.addProperty("duration", 0)
                o.addProperty("waveform", "")
            }
            else -> {
                o.addProperty("file_name", fileName)
                o.addProperty("caption", "")
            }
        }
        return o
    }

    override suspend fun revokeMessage(groupId: String, msgId: String) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) {
            val base = superGroupBaseOrThrow(existing)
            meshChatServerSession.ensureSession(base)
            meshChatServerDirectApi.postRetract(base.superGroupRetract(groupId, msgId).toString())
        } else {
            api.revokeGroupMessage(groupId, msgId)
        }
        refreshMessages(groupId)
    }

    override suspend fun syncGroup(groupId: String, fromPeerId: String) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId)
        if (existing?.isSuperGroup == true) return@withContext
        api.syncGroup(groupId, GroupSyncBodyDto(fromPeerId))
        Unit
    }

    /** 与 [syncSuperGroupsFromAllKnownServers] 共用：DataStore 已记录基址 + 本地超级群去重基址。 */
    private suspend fun collectKnownMeshChatServerBases(): Set<String> {
        val known = LinkedHashSet<String>()
        settingsStore.getMeshChatJoinedServerBases().forEach { known.add(normalizeMeshChatServerBaseUrl(it)) }
        // 登录 meshchat-server 时写入的 JWT API 根（与「已加入」集合可能不同步；库被清空后仍须能拉取）
        val (_, jwtApiBase) = settingsStore.getMeshChatServerJwtPair()
        if (!jwtApiBase.isNullOrBlank()) {
            known.add(normalizeMeshChatServerBaseUrl(jwtApiBase))
        }
        database.groupDao().getDistinctSuperGroupApiBaseUrls().forEach { url ->
            if (url.isNotBlank()) {
                known.add(normalizeMeshChatServerBaseUrl(url))
                settingsStore.addMeshChatJoinedServerBase(url)
            }
        }
        return known
    }

    /**
     * 将本地 [profiles] 同步到指定 meshchat-server（[PATCH /users/{peer_id}/profile]）。
     * 路径 [peer_id] 须与 JWT 登录身份一致；[ensureSession] 失败或 PATCH 失败仅打日志。
     */
    private suspend fun pushLocalProfileToMeshChatServer(base: HttpUrl) {
        val profile = database.profileDao().getProfileOnce() ?: return
        val peerId = profile.peerId.trim()
        if (peerId.isBlank()) return
        val body = MeshChatServerPatchUserProfileBodyDto(
            displayName = profile.nickname.trim().ifBlank { peerId },
            avatarCid = profile.avatarCid?.takeIf { it.isNotBlank() },
            bio = profile.bio,
            status = "active",
        )
        runCatching {
            meshChatServerSession.ensureSession(base)
            meshChatServerDirectApi.patchUserProfileByPeerId(
                base.userProfileByPeerId(peerId).toString(),
                body,
            )
        }.onFailure { t: Throwable ->
            MeshchatHttpErrors.log("pushLocalProfileToMeshChatServer(${base.superGroupApiRoot()})", t)
        }
    }

    override suspend fun pushLocalProfileToAllKnownMeshChatServers() = withContext(ioDispatcher) {
        val known = collectKnownMeshChatServerBases()
        if (known.isEmpty()) return@withContext
        for (baseStr in known) {
            pushLocalProfileToMeshChatServer(baseStr.toSuperGroupBaseHttpUrl())
        }
    }

    override suspend fun joinSuperGroupByInviteUrl(rawUrl: String): String = withContext(ioDispatcher) {
        try {
            val parsed = parseSuperGroupInviteUrl(rawUrl)
            val base = parsed.serverBaseHttpUrl
            val baseStr = base.toString()
            meshChatServerSession.ensureSession(base)
            val member = meshChatServerDirectApi.postJoinGroup(base.joinSuperGroup(parsed.groupId).toString())
            member.user?.id?.let { settingsStore.setMeshChatServerUserId(it) }
            pushLocalProfileToMeshChatServer(base)
            val g = meshChatServerDirectApi.getGroup(base.superGroupDetail(parsed.groupId).toString())
            database.groupDao().upsert(g.toGroupEntity(baseStr, localUnreadCount = 0))
            settingsStore.addMeshChatJoinedServerBase(baseStr)
            refreshMessages(parsed.groupId)
            parsed.groupId
        } catch (e: Exception) {
            MeshchatHttpErrors.log("joinSuperGroupByInviteUrl", e)
            throw e
        }
    }

    override suspend fun loadSuperGroupIntroSnapshot(groupId: String): SuperGroupIntroSnapshot? = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId) ?: return@withContext null
        if (!existing.isSuperGroup) return@withContext null
        val base = superGroupBaseOrThrow(existing)
        meshChatServerSession.ensureSession(base)
        val g = meshChatServerDirectApi.getGroup(base.superGroupDetail(groupId).toString())
        val members = meshChatServerDirectApi.getGroupMembers(base.superGroupMembersList(groupId).toString()).orEmpty()
        val myId = settingsStore.meshChatServerUserIdFlow.value
        val canInvite = myId != null && members.any { m ->
            m.user?.id == myId && (m.role == "owner" || m.role == "admin")
        }
        SuperGroupIntroSnapshot(
            title = g.title,
            about = g.about,
            avatarCid = g.avatarCid,
            memberListVisibility = g.memberListVisibility,
            joinMode = g.joinMode,
            canInvite = canInvite,
            canEditGroupInfo = canInvite,
        )
    }

    override suspend fun uploadSuperGroupAvatarToIpfs(groupId: String, file: File): String = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId) ?: error("group_not_found")
        if (!existing.isSuperGroup) error("not_super_group")
        val base = superGroupBaseOrThrow(existing)
        meshChatServerSession.ensureSession(base)
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        val ipfs = meshChatServerDirectApi.postIpfsAdd(base.superGroupIpfsAdd().toString(), part)
        val cid = ipfs.cid ?: error("ipfs_no_cid")
        val size = ipfs.size ?: file.length()
        val mime = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        meshChatServerDirectApi.postRegisterFile(
            base.superGroupRegisterFile().toString(),
            MeshChatServerRegisterFileBodyDto(
                cid = cid,
                mimeType = mime,
                size = size,
                fileName = file.name,
            ),
        )
        cid
    }

    override suspend fun updateSuperGroupInfo(
        groupId: String,
        title: String,
        about: String,
        avatarCid: String?,
        memberListVisibility: String?,
        joinMode: String?,
    ) = withContext(ioDispatcher) {
        val existing = database.groupDao().getGroupOnce(groupId) ?: error("group_not_found")
        if (!existing.isSuperGroup) return@withContext
        val base = superGroupBaseOrThrow(existing)
        meshChatServerSession.ensureSession(base)
        val body = JsonObject()
        body.addProperty("title", title)
        body.addProperty("about", about)
        if (avatarCid != null) {
            body.addProperty("avatar_cid", avatarCid)
        }
        if (memberListVisibility != null) {
            body.addProperty("member_list_visibility", memberListVisibility)
        }
        if (joinMode != null) {
            body.addProperty("join_mode", joinMode)
        }
        val g = meshChatServerDirectApi.patchGroup(base.superGroupDetail(groupId).toString(), body)
        val baseUrl = existing.superGroupApiBaseUrl.orEmpty()
        database.groupDao().upsert(
            g.toGroupEntity(baseUrl, localUnreadCount = existing.localUnreadCount),
        )
        refreshGroup(groupId)
    }

    override suspend fun inviteSuperGroupMembersByPeerIds(groupId: String, peerIds: List<String>) = withContext(ioDispatcher) {
        val distinct = peerIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.isEmpty()) return@withContext
        val existing = database.groupDao().getGroupOnce(groupId) ?: error("group_not_found")
        val base = superGroupBaseOrThrow(existing)
        meshChatServerSession.ensureSession(base)
        meshChatServerDirectApi.postInviteMembers(
            base.superGroupMembersInvite(groupId).toString(),
            MeshChatServerInviteMembersBodyDto(peerIds = distinct),
        )
        refreshGroup(groupId)
    }

    override suspend fun syncSuperGroupsFromAllKnownServers() = withContext(ioDispatcher) {
        val known = collectKnownMeshChatServerBases()
        if (known.isEmpty()) return@withContext

        for (baseStr in known) {
            runCatching {
                val base = baseStr.toSuperGroupBaseHttpUrl()
                meshChatServerSession.ensureSession(base)
                val merged = mutableListOf<MeshChatServerGroupDto>()
                var offset = 0
                val pageSize = 100
                while (true) {
                    val page = meshChatServerDirectApi.getMeGroups(
                        base.meGroups(limit = pageSize, offset = offset).toString(),
                    ).orEmpty()
                    if (page.isEmpty()) break
                    merged.addAll(page)
                    if (page.size < pageSize) break
                    offset += pageSize
                }
                val baseNorm = normalizeMeshChatServerBaseUrl(baseStr)
                database.withTransaction {
                    for (dto in merged) {
                        val prev = database.groupDao().getGroupOnce(dto.groupId)
                        database.groupDao().upsert(
                            dto.toGroupEntity(baseNorm, localUnreadCount = prev?.localUnreadCount ?: 0),
                        )
                    }
                    val localForServer = database.groupDao().getAllSuperGroupEntities()
                        .filter { normalizeMeshChatServerBaseUrl(it.superGroupApiBaseUrl.orEmpty()) == baseNorm }
                    val remoteIds = merged.map { it.groupId }.toSet()
                    for (entity in localForServer) {
                        if (entity.groupId !in remoteIds) {
                            database.groupMessageDao().clearGroup(entity.groupId)
                            database.groupDao().deleteByGroupId(entity.groupId)
                        }
                    }
                }
            }.onFailure { e ->
                MeshchatHttpErrors.log("syncSuperGroupsFromAllKnownServers($baseStr)", e)
            }
        }
    }

    override suspend fun clearSuperGroupLocalUnread(groupId: String) = withContext(ioDispatcher) {
        database.groupDao().clearSuperGroupLocalUnread(groupId)
    }
}

@Singleton
class DefaultChatEventsRepository @Inject constructor(
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ChatEventsRepository {
    override val events: Flow<List<ChatEvent>> = database.chatEventDao().observeLatest().map { list -> list.map { it.toDomain() } }

    override suspend fun persistEvent(event: ChatEvent) = withContext(ioDispatcher) {
        database.chatEventDao().insert(event.toEntity())
    }
}

@Singleton
class AppCoordinator @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val contactsRepository: ContactsRepository,
    private val directChatRepository: DirectChatRepository,
    private val groupRepository: GroupRepository,
    private val publicChannelRepository: PublicChannelRepository,
    private val chatEventsRepository: ChatEventsRepository,
    private val socket: MeshChatSocket,
    private val meshChatServerWebSocket: MeshChatServerWebSocket,
    private val localChatNotifier: LocalChatNotifier,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun start() {
        socket.start { eventDto ->
            runCatching { handleSocketEvent(eventDto) }
        }
        meshChatServerWebSocket.start()
    }

    suspend fun refreshAll() {
        profileRepository.refreshMyProfile()
        runCatching { groupRepository.pushLocalProfileToAllKnownMeshChatServers() }
        contactsRepository.refreshContacts()
        contactsRepository.refreshRequests()
        directChatRepository.refreshConversations()
        groupRepository.refreshGroups()
        runCatching { groupRepository.syncSuperGroupsFromAllKnownServers() }
        runCatching { publicChannelRepository.refreshSubscriptions() }
    }

    private suspend fun handleSocketEvent(eventDto: ChatEventDto) {
        val event = eventDto.toDomain()
        chatEventsRepository.persistEvent(event)
        when (event.type) {
            "retention_update" -> {
                val minutes = eventDto.retentionMinutes ?: return
                when (event.kind) {
                    "group" -> {
                        val gid = eventDto.groupId ?: eventDto.conversationId ?: return
                        groupRepository.patchLocalRetentionMinutes(gid, minutes)
                    }
                    else -> {
                        val cid = eventDto.conversationId ?: return
                        directChatRepository.patchLocalRetentionMinutes(cid, minutes)
                    }
                }
            }

            "message", "message_state" -> {
                if (eventDto.type == "message") {
                    runCatching { localChatNotifier.onMeshProxySocketMessage(eventDto) }
                }
                when (event.kind) {
                    "group" -> {
                        event.conversationId?.let { groupId ->
                            groupRepository.refreshMessages(groupId)
                            groupRepository.refreshGroup(groupId)
                        }
                        groupRepository.refreshGroups()
                    }

                    else -> {
                        event.conversationId?.let { conversationId ->
                            directChatRepository.refreshMessages(conversationId)
                        }
                        directChatRepository.refreshConversations()
                    }
                }
            }

            "friend_request" -> {
                contactsRepository.refreshRequests()
                contactsRepository.refreshContacts()
                directChatRepository.refreshConversations()
            }

            "conversation_deleted" -> directChatRepository.refreshConversations()
            "contact_deleted" -> contactsRepository.refreshContacts()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    abstract fun bindSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository

    @Binds
    abstract fun bindContactsRepository(impl: DefaultContactsRepository): ContactsRepository

    @Binds
    abstract fun bindDirectChatRepository(impl: DefaultDirectChatRepository): DirectChatRepository

    @Binds
    abstract fun bindGroupRepository(impl: DefaultGroupRepository): GroupRepository

    @Binds
    abstract fun bindChatEventsRepository(impl: DefaultChatEventsRepository): ChatEventsRepository

    @Binds
    abstract fun bindPublicChannelRepository(impl: DefaultPublicChannelRepository): PublicChannelRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * 仅增加 [com.github.com.chenjia404.meshchat.data.local.entity.GroupEntity.groupAbout]，
     * 避免版本升级时 [fallbackToDestructiveMigration] 清空整库导致超级群等本地数据丢失。
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE groups ADD COLUMN groupAbout TEXT NOT NULL DEFAULT ''",
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE groups ADD COLUMN localUnreadCount INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: android.content.Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "meshchat.db",
    )
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun providePublicChannelDatabase(
        @ApplicationContext context: android.content.Context,
    ): PublicChannelDatabase = Room.databaseBuilder(
        context,
        PublicChannelDatabase::class.java,
        "public_channels.db",
    ).fallbackToDestructiveMigration().build()
}
