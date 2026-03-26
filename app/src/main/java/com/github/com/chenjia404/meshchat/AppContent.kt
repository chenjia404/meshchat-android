package com.github.com.chenjia404.meshchat

import androidx.compose.runtime.Composable
import com.github.com.chenjia404.meshchat.core.ui.MeshChatTheme
import com.github.com.chenjia404.meshchat.navigation.MeshChatNavHost
import com.github.com.chenjia404.meshchat.share.IncomingShareManager

val AppContent: @Composable (IncomingShareManager) -> Unit = { incomingShareManager ->
    MeshChatTheme {
        MeshChatNavHost(incomingShareManager = incomingShareManager)
    }
}
