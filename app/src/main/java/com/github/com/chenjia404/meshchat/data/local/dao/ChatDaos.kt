package com.github.com.chenjia404.meshchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.com.chenjia404.meshchat.data.local.entity.ChatEventEntity
import com.github.com.chenjia404.meshchat.data.local.entity.ContactEntity
import com.github.com.chenjia404.meshchat.data.local.entity.DirectConversationEntity
import com.github.com.chenjia404.meshchat.data.local.entity.DirectMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.FriendRequestEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getProfileOnce(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface FriendRequestDao {
    @Query("SELECT * FROM friend_requests ORDER BY updatedAt DESC")
    fun observeRequests(): Flow<List<FriendRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FriendRequestEntity>)

    @Query("DELETE FROM friend_requests")
    suspend fun clearAll()

    @Query("DELETE FROM friend_requests WHERE requestId = :requestId")
    suspend fun deleteById(requestId: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY nickname COLLATE NOCASE ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ContactEntity)

    @Query("DELETE FROM contacts")
    suspend fun clearAll()

    @Query("DELETE FROM contacts WHERE peerId = :peerId")
    suspend fun deleteById(peerId: String)
}

@Dao
interface DirectConversationDao {
    @Query("SELECT * FROM direct_conversations ORDER BY COALESCE(lastMessageAt, updatedAt) DESC")
    fun observeConversations(): Flow<List<DirectConversationEntity>>

    @Query("SELECT * FROM direct_conversations WHERE conversationId = :conversationId LIMIT 1")
    fun observeConversation(conversationId: String): Flow<DirectConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DirectConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DirectConversationEntity)

    @Query("DELETE FROM direct_conversations")
    suspend fun clearAll()

    @Query("DELETE FROM direct_conversations WHERE conversationId = :conversationId")
    suspend fun deleteById(conversationId: String)

    /** WebSocket retention_update：仅更新保留时长，避免整表 refresh */
    @Query(
        "UPDATE direct_conversations SET retentionMinutes = :minutes, updatedAt = :updatedAt " +
            "WHERE conversationId = :conversationId",
    )
    suspend fun updateRetentionMinutes(conversationId: String, minutes: Int, updatedAt: String)
}

@Dao
interface DirectMessageDao {
    @Query("SELECT * FROM direct_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<DirectMessageEntity>>

    @Query("SELECT * FROM direct_messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestMessage(conversationId: String): Flow<DirectMessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DirectMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DirectMessageEntity)

    @Query("DELETE FROM direct_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY COALESCE(lastMessageAt, updatedAt) DESC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    fun observeGroup(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupOnce(groupId: String): GroupEntity?

    @Query("SELECT groupId FROM groups WHERE isSuperGroup = 1")
    suspend fun getSuperGroupIds(): List<String>

    @Query(
        "SELECT DISTINCT superGroupApiBaseUrl FROM groups WHERE isSuperGroup = 1 " +
            "AND superGroupApiBaseUrl IS NOT NULL AND superGroupApiBaseUrl != ''",
    )
    suspend fun getDistinctSuperGroupApiBaseUrls(): List<String>

    @Query("SELECT * FROM groups WHERE isSuperGroup = 1")
    suspend fun getAllSuperGroupEntities(): List<GroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroupEntity)

    @Query("DELETE FROM groups")
    suspend fun clearAll()

    @Query("DELETE FROM groups WHERE isSuperGroup = 0")
    suspend fun deleteMeshProxyGroupsOnly()

    @Query("DELETE FROM groups WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query(
        "UPDATE groups SET retentionMinutes = :minutes, updatedAt = :updatedAt WHERE groupId = :groupId",
    )
    suspend fun updateRetentionMinutes(groupId: String, minutes: Int, updatedAt: String)
}

@Dao
interface GroupMessageDao {
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY createdAt ASC")
    fun observeMessages(groupId: String): Flow<List<GroupMessageEntity>>

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestMessage(groupId: String): Flow<GroupMessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GroupMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroupMessageEntity)

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: String)
}

@Dao
interface ChatEventDao {
    @Query("SELECT * FROM chat_events ORDER BY atUnixMillis DESC LIMIT 100")
    fun observeLatest(): Flow<List<ChatEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChatEventEntity)
}
