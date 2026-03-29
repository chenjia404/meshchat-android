package com.github.com.chenjia404.meshchat.service.notification

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 自通知点击进入会话，由 [MeshChatNavHost] 消费并 [consumePending]。 */
sealed class ChatOpenRequest {
    data class DirectChat(val conversationId: String) : ChatOpenRequest()
    data class GroupChat(val groupId: String) : ChatOpenRequest()
    data class PublicChannel(val channelId: String) : ChatOpenRequest()
}

@Singleton
class ChatOpenNavigationBus @Inject constructor() {
    private val _pending = MutableStateFlow<ChatOpenRequest?>(null)
    val pending: StateFlow<ChatOpenRequest?> = _pending.asStateFlow()

    fun requestOpen(req: ChatOpenRequest) {
        _pending.value = req
    }

    fun consumePending() {
        _pending.value = null
    }
}
