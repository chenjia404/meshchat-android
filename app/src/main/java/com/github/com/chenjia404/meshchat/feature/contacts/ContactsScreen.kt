package com.github.com.chenjia404.meshchat.feature.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.core.ui.SectionTitle
import com.github.com.chenjia404.meshchat.core.util.shouldShowInIncomingPendingList
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactUiModel(
    val peerId: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String?,
    val blocked: Boolean,
    val conversationId: String?,
)

data class FriendRequestUiModel(
    val requestId: String,
    val title: String,
    val body: String,
    val avatarUrl: String?,
)

data class ContactsUiState(
    val requests: List<FriendRequestUiModel> = emptyList(),
    val contacts: List<ContactUiModel> = emptyList(),
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    directChatRepository: DirectChatRepository,
    profileRepository: ProfileRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
) : ViewModel() {
    val uiState: StateFlow<ContactsUiState> = combine(
        contactsRepository.requests,
        contactsRepository.contacts,
        directChatRepository.conversations,
        profileRepository.myProfile,
    ) { requests, contacts, conversations, profile ->
        val myPeerId = profile?.peerId
        val conversationMap = conversations.associateBy { it.peerId }
        val pendingIncoming = requests.filter { it.shouldShowInIncomingPendingList(myPeerId) }
        ContactsUiState(
            requests = pendingIncoming.map {
                FriendRequestUiModel(
                    requestId = it.requestId,
                    title = it.nickname.ifBlank { it.fromPeerId },
                    body = it.introText.ifBlank { it.state },
                    avatarUrl = attachmentUrlBuilder.avatarUrl(it.avatar),
                )
            },
            contacts = contacts.map {
                ContactUiModel(
                    peerId = it.peerId,
                    title = it.nickname.ifBlank { it.remoteNickname ?: it.peerId },
                    subtitle = it.bio.ifBlank { it.updatedAt },
                    avatarUrl = attachmentUrlBuilder.avatarUrl(it.avatar),
                    blocked = it.blocked,
                    conversationId = conversationMap[it.peerId]?.conversationId,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    init {
        viewModelScope.launch {
            contactsRepository.refreshContacts()
            contactsRepository.refreshRequests()
            profileRepository.refreshMyProfile()
        }
    }

    fun sendRequest(peerId: String, introText: String) {
        viewModelScope.launch { contactsRepository.sendFriendRequest(peerId, introText) }
    }

    fun accept(requestId: String) {
        viewModelScope.launch { contactsRepository.acceptRequest(requestId) }
    }

    fun reject(requestId: String) {
        viewModelScope.launch { contactsRepository.rejectRequest(requestId) }
    }

    fun toggleBlocked(item: ContactUiModel) {
        viewModelScope.launch { contactsRepository.setBlocked(item.peerId, !item.blocked) }
    }

    fun deleteContact(peerId: String) {
        viewModelScope.launch { contactsRepository.deleteContact(peerId) }
    }
}

@Composable
fun ContactsScreen(
    onContactClick: (String) -> Unit,
    onAddFriendClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    // 标题行放在 LazyColumn 外，避免列表项内 DropdownMenu 的 Popup 锚点错乱（曾表现为出现在左下角）
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "好友请求",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "添加")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("添加好友") },
                        onClick = {
                            expanded = false
                            onAddFriendClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("创建群聊") },
                        onClick = {
                            expanded = false
                            onCreateGroupClick()
                        },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (uiState.requests.isEmpty()) {
                item {
                    EmptyState(
                        title = "暂无好友请求",
                        body = "收到新的 friend_request 事件后，这里会自动刷新。",
                    )
                }
            } else {
                items(uiState.requests, key = { it.requestId }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarImage(item.title, item.avatarUrl, Modifier.size(44.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { viewModel.accept(item.requestId) }) { Text("接受") }
                            Button(onClick = { viewModel.reject(item.requestId) }) { Text("拒绝") }
                        }
                    }
                }
            }

            item { SectionTitle("联系人") }
            if (uiState.contacts.isEmpty()) {
                item {
                    EmptyState(title = "暂无联系人", body = "发送请求并被接受后，联系人会显示在这里。")
                }
            } else {
                itemsIndexed(uiState.contacts, key = { _, item -> item.peerId }) { index, item ->
                    // 联系人列表相邻项只用一条分隔线，不再用 vertical padding 留间距
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactClick(item.peerId) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarImage(item.title, item.avatarUrl, Modifier.size(44.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
