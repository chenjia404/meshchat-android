package com.github.com.chenjia404.meshchat.service.notification

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatNotificationEntryPoint {
    fun chatActiveSessionStore(): ChatActiveSessionStore
    fun localChatNotifier(): LocalChatNotifier
    fun chatOpenNavigationBus(): ChatOpenNavigationBus
}
