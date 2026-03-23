package com.github.com.chenjia404.meshchat.domain.repository

import com.github.com.chenjia404.meshchat.domain.model.ChatEvent
import com.github.com.chenjia404.meshchat.domain.model.Contact
import com.github.com.chenjia404.meshchat.domain.model.DirectConversation
import com.github.com.chenjia404.meshchat.domain.model.DirectMessage
import com.github.com.chenjia404.meshchat.domain.model.FriendRequest
import com.github.com.chenjia404.meshchat.domain.model.Group
import com.github.com.chenjia404.meshchat.domain.model.GroupMessage
import com.github.com.chenjia404.meshchat.domain.model.Profile
import java.io.File
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val baseUrl: Flow<String>
    suspend fun setBaseUrl(value: String)
    fun currentBaseUrl(): String
}

interface ProfileRepository {
    val myProfile: Flow<Profile?>
    suspend fun refreshMyProfile()
    suspend fun refreshProfile()
    suspend fun updateProfile(nickname: String, bio: String)
    suspend fun uploadAvatar(file: File)
}

interface ContactsRepository {
    val contacts: Flow<List<Contact>>
    val requests: Flow<List<FriendRequest>>
    suspend fun refreshContacts()
    suspend fun refreshRequests()
    suspend fun sendFriendRequest(toPeerId: String, introText: String)
    suspend fun acceptRequest(requestId: String)
    suspend fun rejectRequest(requestId: String)
    suspend fun deleteContact(peerId: String)
    suspend fun updateContactNickname(peerId: String, nickname: String)
    suspend fun setBlocked(peerId: String, blocked: Boolean)
}

interface DirectChatRepository {
    val conversations: Flow<List<DirectConversation>>
    fun observeMessages(conversationId: String): Flow<List<DirectMessage>>
    fun observeLatestMessage(conversationId: String): Flow<DirectMessage?>
    fun observeConversation(conversationId: String): Flow<DirectConversation?>
    suspend fun refreshConversations()
    suspend fun refreshMessages(conversationId: String, limit: Int? = null, offset: Int? = null)
    suspend fun sendText(conversationId: String, text: String)
    suspend fun sendFile(conversationId: String, file: File)
    suspend fun markRead(conversationId: String)
    suspend fun syncConversation(conversationId: String)
    suspend fun updateRetention(conversationId: String, retentionMinutes: Int)
    /** WS retention_update：仅更新本地会话保留时长 */
    suspend fun patchLocalRetentionMinutes(conversationId: String, retentionMinutes: Int)
    suspend fun revokeMessage(conversationId: String, msgId: String)
    suspend fun deleteConversation(conversationId: String)
}

interface GroupRepository {
    val groups: Flow<List<Group>>
    fun observeGroup(groupId: String): Flow<Group?>
    fun observeMessages(groupId: String): Flow<List<GroupMessage>>
    suspend fun refreshGroups()
    suspend fun refreshGroup(groupId: String)
    suspend fun refreshMessages(groupId: String)
    suspend fun createGroup(title: String, members: List<String>)
    suspend fun invite(groupId: String, peerId: String, role: String = "member", inviteText: String = "")
    suspend fun join(groupId: String)
    suspend fun leave(groupId: String, reason: String = "")
    suspend fun remove(groupId: String, peerId: String, reason: String = "")
    suspend fun updateTitle(groupId: String, title: String)
    suspend fun updateRetention(groupId: String, retentionMinutes: Int)
    /** WS retention_update：仅更新本地群保留时长 */
    suspend fun patchLocalRetentionMinutes(groupId: String, retentionMinutes: Int)
    suspend fun dissolve(groupId: String, reason: String = "")
    suspend fun changeController(groupId: String, peerId: String)
    suspend fun sendText(groupId: String, text: String)
    suspend fun sendFile(groupId: String, file: File)
    suspend fun revokeMessage(groupId: String, msgId: String)
    suspend fun syncGroup(groupId: String, fromPeerId: String)
}

interface ChatEventsRepository {
    val events: Flow<List<ChatEvent>>
    suspend fun persistEvent(event: ChatEvent)
}
