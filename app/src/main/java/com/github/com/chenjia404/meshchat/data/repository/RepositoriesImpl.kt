package com.github.com.chenjia404.meshchat.data.repository

import androidx.room.Room
import androidx.room.withTransaction
import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import com.github.com.chenjia404.meshchat.data.local.db.AppDatabase
import com.github.com.chenjia404.meshchat.data.local.db.PublicChannelDatabase
import com.github.com.chenjia404.meshchat.data.mapper.toDomain
import com.github.com.chenjia404.meshchat.data.mapper.toEntity
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatApi
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
import com.github.com.chenjia404.meshchat.data.remote.dto.UpdateProfileBodyDto
import com.github.com.chenjia404.meshchat.data.remote.ws.MeshChatSocket
import com.github.com.chenjia404.meshchat.domain.model.ChatEvent
import com.github.com.chenjia404.meshchat.domain.model.Contact
import com.github.com.chenjia404.meshchat.domain.model.DirectConversation
import com.github.com.chenjia404.meshchat.domain.model.DirectMessage
import com.github.com.chenjia404.meshchat.domain.model.FriendRequest
import com.github.com.chenjia404.meshchat.domain.model.Group
import com.github.com.chenjia404.meshchat.domain.model.GroupMessage
import com.github.com.chenjia404.meshchat.domain.model.Profile
import com.github.com.chenjia404.meshchat.domain.repository.ChatEventsRepository
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.domain.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
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
    }

    override suspend fun uploadAvatar(file: File) = withContext(ioDispatcher) {
        val avatar = MultipartBody.Part.createFormData(
            name = "avatar",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        database.profileDao().upsert(api.uploadAvatar(avatar).toDomain().toEntity())
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
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GroupRepository {
    override val groups: Flow<List<Group>> = database.groupDao().observeGroups().map { list -> list.map { it.toDomain() } }

    override fun observeGroup(groupId: String): Flow<Group?> = database.groupDao().observeGroup(groupId).map { it?.toDomain() }

    override fun observeMessages(groupId: String): Flow<List<GroupMessage>> =
        database.groupMessageDao().observeMessages(groupId).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshGroups() = withContext(ioDispatcher) {
        val items = api.getGroups().body().orEmpty().map { it.toDomain().toEntity() }
        database.withTransaction {
            database.groupDao().clearAll()
            database.groupDao().upsertAll(items)
        }
    }

    override suspend fun refreshGroup(groupId: String) = withContext(ioDispatcher) {
        database.groupDao().upsert(api.getGroup(groupId).group.toDomain().toEntity())
    }

    override suspend fun refreshMessages(groupId: String) = withContext(ioDispatcher) {
        val items = api.getGroupMessages(groupId).orEmpty().map { it.toDomain().toEntity() }
        database.withTransaction {
            database.groupMessageDao().clearGroup(groupId)
            database.groupMessageDao().upsertAll(items)
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
        database.groupDao().upsert(api.leave(groupId, GroupReasonBodyDto(reason)).toDomain().toEntity())
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
        database.groupMessageDao().upsert(api.sendGroupMessage(groupId, SendTextBodyDto(text)).toDomain().toEntity())
        refreshGroups()
    }

    override suspend fun sendFile(groupId: String, file: File) = withContext(ioDispatcher) {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull()),
        )
        database.groupMessageDao().upsert(api.sendGroupFile(groupId, part).toDomain().toEntity())
        refreshGroups()
    }

    override suspend fun revokeMessage(groupId: String, msgId: String) = withContext(ioDispatcher) {
        api.revokeGroupMessage(groupId, msgId)
        refreshMessages(groupId)
    }

    override suspend fun syncGroup(groupId: String, fromPeerId: String) = withContext(ioDispatcher) {
        api.syncGroup(groupId, GroupSyncBodyDto(fromPeerId))
        Unit
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun start() {
        socket.start { eventDto ->
            runCatching { handleSocketEvent(eventDto) }
        }
    }

    suspend fun refreshAll() {
        profileRepository.refreshMyProfile()
        contactsRepository.refreshContacts()
        contactsRepository.refreshRequests()
        directChatRepository.refreshConversations()
        groupRepository.refreshGroups()
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
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: android.content.Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "meshchat.db",
    ).fallbackToDestructiveMigration().build()

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
