package com.github.com.chenjia404.meshchat.service.storage

import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatAttachmentUrlBuilder @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    fun avatarUrl(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return "${settingsStore.currentBaseUrl()}api/v1/chat/avatars/$name"
    }

    fun directFileUrl(conversationId: String, msgId: String): String {
        return "${settingsStore.currentBaseUrl()}api/v1/chat/conversations/$conversationId/messages/$msgId/file"
    }

    fun groupFileUrl(groupId: String, msgId: String): String {
        return "${settingsStore.currentBaseUrl()}api/v1/groups/$groupId/messages/$msgId/file"
    }
}

