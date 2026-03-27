package com.github.com.chenjia404.meshchat.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.com.chenjia404.meshchat.data.local.dao.PublicChannelDao
import com.github.com.chenjia404.meshchat.data.local.entity.PublicChannelEntity
import com.github.com.chenjia404.meshchat.data.local.entity.PublicChannelMessageEntity

@Database(
    entities = [
        PublicChannelEntity::class,
        PublicChannelMessageEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class PublicChannelDatabase : RoomDatabase() {
    abstract fun publicChannelDao(): PublicChannelDao
}
