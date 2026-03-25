package com.github.com.chenjia404.meshchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.com.chenjia404.meshchat.data.local.entity.PublicChannelEntity
import com.github.com.chenjia404.meshchat.data.local.entity.PublicChannelMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicChannelDao {
    @Query("SELECT * FROM public_channels ORDER BY lastActivitySortMillis DESC")
    fun observeChannels(): Flow<List<PublicChannelEntity>>

    @Query("SELECT * FROM public_channels WHERE channelId = :channelId LIMIT 1")
    fun observeChannel(channelId: String): Flow<PublicChannelEntity?>

    @Query("SELECT * FROM public_channels WHERE channelId = :channelId LIMIT 1")
    suspend fun getChannel(channelId: String): PublicChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(entity: PublicChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(items: List<PublicChannelEntity>)

    @Query("DELETE FROM public_channels WHERE channelId = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Query("SELECT * FROM public_channel_messages WHERE channelId = :channelId ORDER BY messageId ASC")
    fun observeMessages(channelId: String): Flow<List<PublicChannelMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(items: List<PublicChannelMessageEntity>)

    @Query("DELETE FROM public_channel_messages WHERE channelId = :channelId")
    suspend fun deleteMessages(channelId: String)

    @Query("SELECT * FROM public_channel_messages WHERE channelId = :channelId ORDER BY messageId DESC LIMIT 1")
    suspend fun latestMessage(channelId: String): PublicChannelMessageEntity?
}
