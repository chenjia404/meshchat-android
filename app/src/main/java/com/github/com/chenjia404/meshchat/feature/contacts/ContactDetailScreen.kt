package com.github.com.chenjia404.meshchat.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState

@Composable
fun ContactDetailScreen(
    peerId: String,
    onBackClick: () -> Unit,
    onConversationClick: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val item = uiState.contacts.firstOrNull { it.peerId == peerId }

    if (item == null) {
        if (uiState.contacts.isEmpty()) {
            EmptyState(title = "加载中", body = "正在同步联系人信息，请稍候。")
        } else {
            EmptyState(title = "联系人不存在", body = "可能已被删除或 peerId 不正确。")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
            AvatarImage(item.title, item.avatarUrl, Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text("个人资料", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = item.peerId,
                onValueChange = {},
                label = { Text("peer_id") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (item.peerId.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(item.peerId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = item.peerId.isNotBlank(),
            ) {
                Text("复制 peer_id")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (item.conversationId != null) {
                Button(
                    onClick = { onConversationClick(item.conversationId) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("聊天") }
            } else {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("聊天（未建立会话）") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.toggleBlocked(item) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (item.blocked) "解除封锁" else "封锁") }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.deleteContact(item.peerId) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("删除") }
        }
    }
}

