package com.github.com.chenjia404.meshchat.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.com.chenjia404.meshchat.data.local.dao.ChatEventDao
import com.github.com.chenjia404.meshchat.data.local.dao.ContactDao
import com.github.com.chenjia404.meshchat.data.local.dao.DirectConversationDao
import com.github.com.chenjia404.meshchat.data.local.dao.DirectMessageDao
import com.github.com.chenjia404.meshchat.data.local.dao.FriendRequestDao
import com.github.com.chenjia404.meshchat.data.local.dao.GroupDao
import com.github.com.chenjia404.meshchat.data.local.dao.GroupMessageDao
import com.github.com.chenjia404.meshchat.data.local.dao.ProfileDao
import com.github.com.chenjia404.meshchat.data.local.entity.ChatEventEntity
import com.github.com.chenjia404.meshchat.data.local.entity.ContactEntity
import com.github.com.chenjia404.meshchat.data.local.entity.DirectConversationEntity
import com.github.com.chenjia404.meshchat.data.local.entity.DirectMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.FriendRequestEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        FriendRequestEntity::class,
        ContactEntity::class,
        DirectConversationEntity::class,
        DirectMessageEntity::class,
        GroupEntity::class,
        GroupMessageEntity::class,
        ChatEventEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun contactDao(): ContactDao
    abstract fun directConversationDao(): DirectConversationDao
    abstract fun directMessageDao(): DirectMessageDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun chatEventDao(): ChatEventDao
}

