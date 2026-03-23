package com.github.com.chenjia404.meshchat.feature.forward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.github.com.chenjia404.meshchat.domain.model.DirectConversation
import com.github.com.chenjia404.meshchat.domain.model.Group
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ForwardTargetRowItem(
    /** 格式：`d:` + conversationId 或 `g:` + groupId */
    val id: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String?,
    /** 用于排序（ISO 时间字符串，越大越新） */
    private val sortKey: String,
) {
    companion object {
        fun direct(
            conversation: DirectConversation,
            title: String,
            avatarUrl: String?,
        ): ForwardTargetRowItem {
            val t = conversation.lastMessageAt ?: conversation.updatedAt
            return ForwardTargetRowItem(
                id = "d:${conversation.conversationId}",
                title = title,
                subtitle = "私聊",
                avatarUrl = avatarUrl,
                sortKey = t,
            )
        }

        fun group(group: Group, avatarUrl: String?): ForwardTargetRowItem {
            val t = group.lastMessageAt ?: group.updatedAt
            return ForwardTargetRowItem(
                id = "g:${group.groupId}",
                title = group.title.ifBlank { group.groupId },
                subtitle = "群聊",
                avatarUrl = avatarUrl,
                sortKey = t,
            )
        }
    }

    fun compareToNewerFirst(other: ForwardTargetRowItem): Int =
        other.sortKey.compareTo(sortKey)
}

@HiltViewModel
class ForwardTargetPickerViewModel @Inject constructor(
    private val directChatRepository: DirectChatRepository,
    private val groupRepository: GroupRepository,
    contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
) : ViewModel() {

    /** 打开转发弹窗时拉取最新会话与群组列表 */
    fun refreshTargets() {
        viewModelScope.launch {
            directChatRepository.refreshConversations()
            groupRepository.refreshGroups()
        }
    }

    val rows: StateFlow<List<ForwardTargetRowItem>> = combine(
        directChatRepository.conversations,
        groupRepository.groups,
        contactsRepository.contacts,
    ) { conversations, groups, contacts ->
        val contactMap = contacts.associateBy { it.peerId }
        val directRows = conversations.map { c ->
            val contact = contactMap[c.peerId]
            val title = contact?.remoteNickname?.takeIf { it.isNotBlank() }
                ?: contact?.nickname?.takeIf { it.isNotBlank() }
                ?: c.peerId
            ForwardTargetRowItem.direct(
                conversation = c,
                title = title,
                avatarUrl = attachmentUrlBuilder.avatarUrl(contact?.avatar),
            )
        }
        val groupRows = groups.map { g ->
            ForwardTargetRowItem.group(g, attachmentUrlBuilder.avatarUrl(g.avatar))
        }
        (directRows + groupRows).sortedWith { a, b -> a.compareToNewerFirst(b) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
