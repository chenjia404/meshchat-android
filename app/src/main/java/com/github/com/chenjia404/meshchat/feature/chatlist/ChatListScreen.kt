package com.github.com.chenjia404.meshchat.feature.chatlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.core.util.formatChatTime
import com.github.com.chenjia404.meshchat.core.util.renderConversationPreview
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ChatListItemUiModel(
    val conversationId: String,
    val title: String,
    val preview: String,
    val avatarUrl: String?,
    val unreadCount: Int,
    val timestamp: String,
)

data class ChatListUiState(
    val items: List<ChatListItemUiModel> = emptyList(),
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    directChatRepository: DirectChatRepository,
    contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
) : ViewModel() {
    val uiState: StateFlow<ChatListUiState> = combine(
        directChatRepository.conversations,
        contactsRepository.contacts,
    ) { conversations, contacts ->
        conversations to contacts
    }.flatMapLatest { (conversations, contacts) ->
        if (conversations.isEmpty()) {
            flowOf(ChatListUiState())
        } else {
            combine(conversations.map { conversation ->
                directChatRepository.observeLatestMessage(conversation.conversationId)
            }) { latestMessages ->
                val contactMap = contacts.associateBy { it.peerId }
                ChatListUiState(
                    items = conversations.mapIndexed { index, conversation ->
                        val contact = contactMap[conversation.peerId]
                        val latestMessage = latestMessages[index]
                        val title = contact?.remoteNickname
                            ?.takeIf { it.isNotBlank() }
                            ?: contact?.nickname?.takeIf { it.isNotBlank() }
                            ?: conversation.peerId
                        ChatListItemUiModel(
                            conversationId = conversation.conversationId,
                            title = title,
                            preview = renderConversationPreview(
                                msgType = latestMessage?.msgType,
                                plaintext = latestMessage?.plaintext,
                                mimeType = latestMessage?.mimeType,
                                fileName = latestMessage?.fileName,
                            ).ifBlank { contact?.bio.orEmpty() },
                            avatarUrl = attachmentUrlBuilder.avatarUrl(contact?.avatar),
                            unreadCount = conversation.unreadCount,
                            timestamp = formatChatTime(
                                latestMessage?.createdAt ?: conversation.lastMessageAt ?: conversation.updatedAt,
                            ),
                        )
                    },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState())
}

@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.items.isEmpty()) {
        EmptyState(
            title = "还没有单聊会话",
            body = "先在联系人页发送好友请求，接受后这里会出现会话。",
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(uiState.items, key = { _, item -> item.conversationId }) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConversationClick(item.conversationId) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AvatarImage(
                    title = item.title,
                    avatarUrl = item.avatarUrl,
                    modifier = Modifier.size(58.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        item.preview.ifBlank { " " },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        item.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.unreadCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = item.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (index < uiState.items.lastIndex) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
