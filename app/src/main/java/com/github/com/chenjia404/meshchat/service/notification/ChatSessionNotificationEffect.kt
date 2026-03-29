package com.github.com.chenjia404.meshchat.service.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors

/**
 * 进入聊天页：登记当前前台会话（用于抑制同会话通知）、清除该会话通知；离开时解除登记。
 */
@Composable
fun ChatSessionNotificationEffect(session: ActiveChatSession) {
    val appContext = LocalContext.current.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, ChatNotificationEntryPoint::class.java)
    }
    val sessionStore = entryPoint.chatActiveSessionStore()
    val notifier = entryPoint.localChatNotifier()

    DisposableEffect(session) {
        sessionStore.setSession(session)
        when (session) {
            ActiveChatSession.None -> Unit
            is ActiveChatSession.Direct -> notifier.cancelForDirect(session.conversationId)
            is ActiveChatSession.Group -> notifier.cancelForGroup(session.groupId)
            is ActiveChatSession.PublicChannel -> notifier.cancelForPublicChannel(session.channelId)
        }
        onDispose {
            sessionStore.clearIfCurrent(session)
        }
    }
}
