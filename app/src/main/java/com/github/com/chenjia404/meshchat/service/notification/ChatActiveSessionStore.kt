package com.github.com.chenjia404.meshchat.service.notification

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 当前用户正在查看的聊天会话（用于前台时抑制同会话通知）。
 */
sealed class ActiveChatSession {
    data object None : ActiveChatSession()
    data class Direct(val conversationId: String) : ActiveChatSession()
    data class Group(val groupId: String) : ActiveChatSession()
    data class PublicChannel(val channelId: String) : ActiveChatSession()
}

@Singleton
class ChatActiveSessionStore @Inject constructor() {
    private val _session = MutableStateFlow<ActiveChatSession>(ActiveChatSession.None)
    val session: StateFlow<ActiveChatSession> = _session.asStateFlow()

    fun setSession(s: ActiveChatSession) {
        _session.value = s
    }

    /** 仅当当前仍为 [expected] 时清空，避免快速切换会话时误清。 */
    fun clearIfCurrent(expected: ActiveChatSession) {
        if (_session.value == expected) {
            _session.value = ActiveChatSession.None
        }
    }
}
