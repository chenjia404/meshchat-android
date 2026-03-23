package com.github.com.chenjia404.meshchat.feature.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.core.util.formatConversationListRelativeTime
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

/** Quark 风格：未读为红底白字正圆角标（固定直径保证圆形，非椭圆），超过 99 显示 99+ */
@Composable
private fun ConversationUnreadBadge(count: Int) {
    if (count <= 0) return
    val label = if (count > 99) "99+" else count.toString()
    val bubbleRed = Color(0xFFFF3B30)
    // 宽高一致 + CircleShape；直径略大，留出内边距，数字不易显得贴顶/偏上
    val diameter = when (label.length) {
        1 -> 22.dp
        2 -> 24.dp
        else -> 28.dp // "99+"
    }
    val fontSize = when (label.length) {
        3 -> 9.sp
        else -> 11.sp
    }
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(bubbleRed),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            // 粗体数字视觉重心略偏上，轻微下移对齐圆心；不用 fillMaxSize 避免测量区导致的「顶格」感
            modifier = Modifier.offset(y = 1.5.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

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
                            timestamp = formatConversationListRelativeTime(
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
    /** 第二个参数为进入会话时的未读条数，用于私聊列表首次定位到首条未读 */
    onConversationClick: (conversationId: String, entryUnread: Int) -> Unit,
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
                    .clickable { onConversationClick(item.conversationId, item.unreadCount) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                // 顶部对齐：昵称首行与时间同一水平线（避免 CenterVertically 导致与预览两行整体居中后时间偏下）
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AvatarImage(
                    title = item.title,
                    avatarUrl = item.avatarUrl,
                    modifier = Modifier.size(58.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        Text(
                            item.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.preview.ifBlank { " " },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        ConversationUnreadBadge(count = item.unreadCount)
                    }
                }
            }
            if (index < uiState.items.lastIndex) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
