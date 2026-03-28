package com.github.com.chenjia404.meshchat.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SuperGroupInviteContactPickerViewModel @Inject constructor(
    private val directChatRepository: DirectChatRepository,
    private val contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
) : ViewModel() {

    fun refresh() {
        viewModelScope.launch {
            directChatRepository.refreshConversations()
            contactsRepository.refreshContacts()
        }
    }

    val rows: StateFlow<List<SuperGroupInviteContactRow>> = combine(
        directChatRepository.conversations,
        contactsRepository.contacts,
    ) { conversations, contacts ->
        val map = contacts.associateBy { it.peerId }
        conversations.mapNotNull { c ->
            val ct = map[c.peerId]
            if (ct?.blocked == true) return@mapNotNull null
            val title = ct?.remoteNickname?.takeIf { it.isNotBlank() }
                ?: ct?.nickname?.takeIf { it.isNotBlank() }
                ?: c.peerId
            val sortKey = c.lastMessageAt ?: c.updatedAt
            SuperGroupInviteContactRow(
                peerId = c.peerId,
                title = title,
                avatarUrl = attachmentUrlBuilder.avatarUrl(ct?.avatar),
            ) to sortKey
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
