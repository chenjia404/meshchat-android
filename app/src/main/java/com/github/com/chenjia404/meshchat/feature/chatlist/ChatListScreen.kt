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
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.res.stringResource
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.util.formatConversationListRelativeTime
import com.github.com.chenjia404.meshchat.core.util.renderConversationPreview
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

sealed class ChatListNavigateTarget {
    data class DirectChat(val conversationId: String, val entryUnread: Int) : ChatListNavigateTarget()
    data class PublicChannel(val channelId: String) : ChatListNavigateTarget()
    data class GroupChat(val groupId: String) : ChatListNavigateTarget()
}

data class ChatListMergedRow(
    val stableKey: String,
    val sortKeyMillis: Long,
    val title: String,
    /** 公开频道列表预览（私聊用下方 msg* 字段在 UI 层拼） */
    val publicPreview: String,
    val bioFallback: String,
    val timeDisplayRaw: String?,
    val avatarUrl: String?,
    val unreadCount: Int,
    val target: ChatListNavigateTarget,
    val latestMsgType: String? = null,
    val latestPlaintext: String? = null,
    val latestMimeType: String? = null,
    val latestFileName: String? = null,
)

data class ChatListUiState(
    val rows: List<ChatListMergedRow> = emptyList(),
)

/** Quark 风格：未读为红底白字正圆角标（固定直径保证圆形，非椭圆），超过 99 显示 99+ */
@Composable
private fun ConversationUnreadBadge(count: Int) {
    if (count <= 0) return
    val label = if (count > 99) "99+" else count.toString()
    val bubbleRed = Color(0xFFFF3B30)
    val diameter = when (label.length) {
        1 -> 22.dp
        2 -> 24.dp
        else -> 28.dp
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    directChatRepository: DirectChatRepository,
    contactsRepository: ContactsRepository,
    groupRepository: GroupRepository,
    publicChannelRepository: PublicChannelRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
) : ViewModel() {

    private fun String?.toSortMillis(): Long =
        runCatching {
            if (this.isNullOrBlank()) 0L else Instant.parse(trim()).toEpochMilli()
        }.getOrElse { 0L }

    /** 接口可能返回空字符串，需视为「无值」以便回退到会话/群组的 lastMessageAt。 */
    private fun String?.ifTimePresent(): String? = this?.takeIf { it.isNotBlank() }

    private val directRowsFlow = combine(
        directChatRepository.conversations,
        contactsRepository.contacts,
    ) { conversations, contacts ->
        conversations to contacts
    }.flatMapLatest { (conversations, contacts) ->
        if (conversations.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(conversations.map { directChatRepository.observeLatestMessage(it.conversationId) }) { latestMessages ->
                val contactMap = contacts.associateBy { it.peerId }
                conversations.mapIndexed { index, conversation ->
                    val contact = contactMap[conversation.peerId]
                    val latestMessage = latestMessages[index]
                    val title = contact?.remoteNickname
                        ?.takeIf { it.isNotBlank() }
                        ?: contact?.nickname?.takeIf { it.isNotBlank() }
                        ?: conversation.peerId
                    val timeRaw = latestMessage?.createdAt.ifTimePresent()
                        ?: conversation.lastMessageAt.ifTimePresent()
                        ?: conversation.updatedAt
                    ChatListMergedRow(
                        stableKey = "d:${conversation.conversationId}",
                        sortKeyMillis = timeRaw.toSortMillis(),
                        title = title,
                        publicPreview = "",
                        bioFallback = contact?.bio.orEmpty(),
                        timeDisplayRaw = timeRaw,
                        avatarUrl = attachmentUrlBuilder.avatarUrl(contact?.avatar),
                        unreadCount = conversation.unreadCount,
                        target = ChatListNavigateTarget.DirectChat(
                            conversation.conversationId,
                            conversation.unreadCount,
                        ),
                        latestMsgType = latestMessage?.msgType,
                        latestPlaintext = latestMessage?.plaintext,
                        latestMimeType = latestMessage?.mimeType,
                        latestFileName = latestMessage?.fileName,
                    )
                }
            }
        }
    }

    private val groupRowsFlow = groupRepository.groups.flatMapLatest { groups ->
        if (groups.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(groups.map { groupRepository.observeLatestGroupMessage(it.groupId) }) { latestMessages ->
                groups.mapIndexed { index, g ->
                    val latest = latestMessages[index]
                    val timeRaw = latest?.createdAt.ifTimePresent()
                        ?: g.lastMessageAt.ifTimePresent()
                        ?: g.updatedAt
                    ChatListMergedRow(
                        stableKey = "g:${g.groupId}",
                        sortKeyMillis = timeRaw.toSortMillis(),
                        title = g.title.ifBlank { g.groupId },
                        publicPreview = "",
                        bioFallback = "",
                        timeDisplayRaw = timeRaw,
                        avatarUrl = if (g.isSuperGroup) {
                            attachmentUrlBuilder.ipfsBlobAbsoluteUrl(g.avatar)
                        } else {
                            attachmentUrlBuilder.avatarUrl(g.avatar)
                        },
                        unreadCount = if (g.isSuperGroup) g.localUnreadCount else 0,
                        target = ChatListNavigateTarget.GroupChat(g.groupId),
                        latestMsgType = latest?.msgType,
                        latestPlaintext = latest?.plaintext,
                        latestMimeType = latest?.mimeType,
                        latestFileName = latest?.fileName,
                    )
                }
            }
        }
    }

    val uiState: StateFlow<ChatListUiState> = combine(
        directRowsFlow,
        groupRowsFlow,
        publicChannelRepository.channels,
    ) { directRows, groupRows, publicChannels ->
        val publicRows = publicChannels.map { ch ->
            val iso = Instant.ofEpochMilli(ch.lastActivitySortMillis).toString()
            ChatListMergedRow(
                stableKey = "p:${ch.channelId}",
                sortKeyMillis = ch.lastActivitySortMillis,
                title = ch.name.ifBlank { ch.channelId },
                publicPreview = ch.lastPreview,
                bioFallback = ch.bio,
                timeDisplayRaw = iso,
                avatarUrl = ch.avatarUrl,
                unreadCount = ch.unreadCount,
                target = ChatListNavigateTarget.PublicChannel(ch.channelId),
            )
        }
        ChatListUiState(
            rows = (directRows + groupRows + publicRows).sortedByDescending { it.sortKeyMillis },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState())
}

@Composable
fun ChatListScreen(
    onNavigate: (ChatListNavigateTarget) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val resources = LocalContext.current.resources
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.rows.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.empty_chat_list_title),
            body = stringResource(R.string.empty_chat_list_body),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(uiState.rows, key = { _, item -> item.stableKey }) { index, item ->
            val previewText = when (item.target) {
                is ChatListNavigateTarget.DirectChat ->
                    renderConversationPreview(
                        resources,
                        item.latestMsgType,
                        item.latestPlaintext,
                        item.latestMimeType,
                        item.latestFileName,
                    ).ifBlank { item.bioFallback }
                is ChatListNavigateTarget.PublicChannel ->
                    item.publicPreview.ifBlank { item.bioFallback }
                is ChatListNavigateTarget.GroupChat ->
                    renderConversationPreview(
                        resources,
                        item.latestMsgType,
                        item.latestPlaintext,
                        item.latestMimeType,
                        item.latestFileName,
                    ).ifBlank { item.bioFallback }
            }
            val timeText = formatConversationListRelativeTime(resources, item.timeDisplayRaw)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(item.target) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
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
                            timeText,
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
                            text = if (previewText.isBlank()) " " else previewText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        ConversationUnreadBadge(count = item.unreadCount)
                    }
                }
            }
            if (index < uiState.rows.lastIndex) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
