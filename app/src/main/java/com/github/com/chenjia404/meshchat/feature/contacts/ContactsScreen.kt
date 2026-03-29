package com.github.com.chenjia404.meshchat.feature.contacts

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.ui.SectionTitle
import com.github.com.chenjia404.meshchat.core.util.shouldShowInIncomingPendingList
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
    /** 个人简介（与列表 subtitle 同源，列表空时 subtitle 可能为 updatedAt） */
    val bio: String,
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
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private fun formatNetworkError(e: Throwable): String =
        when (e) {
            is SocketTimeoutException -> appContext.getString(R.string.error_network_timeout)
            is UnknownHostException -> appContext.getString(R.string.error_unknown_host)
            else -> e.message?.takeIf { it.isNotBlank() } ?: appContext.getString(R.string.error_request_failed)
        }
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
                    bio = it.bio,
                    avatarUrl = attachmentUrlBuilder.avatarUrl(it.avatar),
                    blocked = it.blocked,
                    conversationId = conversationMap[it.peerId]?.conversationId,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsUiState())

    init {
        viewModelScope.launch {
            // 网络/服务端异常或空 body 时不应导致进程崩溃
            runCatching { contactsRepository.refreshContacts() }
            runCatching { contactsRepository.refreshRequests() }
            runCatching { profileRepository.refreshMyProfile() }
        }
    }

    /**
     * @param onFinished 是否成功（用于添加好友页仅在成功时清空输入；主线程回调）
     */
    fun sendRequest(peerId: String, introText: String, onFinished: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { contactsRepository.sendFriendRequest(peerId, introText) }
                .onSuccess {
                    Toast.makeText(appContext, appContext.getString(R.string.toast_friend_request_sent), Toast.LENGTH_SHORT).show()
                    onFinished?.invoke(true)
                }
                .onFailure { e ->
                    Toast.makeText(appContext, formatNetworkError(e), Toast.LENGTH_LONG).show()
                    onFinished?.invoke(false)
                }
        }
    }

    fun accept(requestId: String) {
        viewModelScope.launch {
            runCatching { contactsRepository.acceptRequest(requestId) }
                .onSuccess {
                    Toast.makeText(appContext, appContext.getString(R.string.toast_accepted), Toast.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    Toast.makeText(appContext, formatNetworkError(e), Toast.LENGTH_LONG).show()
                }
        }
    }

    fun reject(requestId: String) {
        viewModelScope.launch {
            runCatching { contactsRepository.rejectRequest(requestId) }
                .onFailure { e ->
                    Toast.makeText(appContext, formatNetworkError(e), Toast.LENGTH_LONG).show()
                }
        }
    }

    fun toggleBlocked(item: ContactUiModel) {
        viewModelScope.launch {
            runCatching { contactsRepository.setBlocked(item.peerId, !item.blocked) }
                .onFailure { e ->
                    Toast.makeText(appContext, formatNetworkError(e), Toast.LENGTH_LONG).show()
                }
        }
    }

    fun deleteContact(peerId: String) {
        viewModelScope.launch {
            runCatching { contactsRepository.deleteContact(peerId) }
                .onFailure { e ->
                    Toast.makeText(appContext, formatNetworkError(e), Toast.LENGTH_LONG).show()
                }
        }
    }
}

@Composable
fun ContactsScreen(
    onContactClick: (String) -> Unit,
    onAddFriendClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreatePublicChannelClick: () -> Unit,
    onSubscribePublicChannelClick: () -> Unit,
    onJoinSuperGroupClick: () -> Unit,
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
                text = stringResource(R.string.friend_requests_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.cd_add))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contacts_add_friend)) },
                        onClick = {
                            expanded = false
                            onAddFriendClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contacts_create_group)) },
                        onClick = {
                            expanded = false
                            onCreateGroupClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contacts_create_public_channel)) },
                        onClick = {
                            expanded = false
                            onCreatePublicChannelClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contacts_subscribe_public_channel)) },
                        onClick = {
                            expanded = false
                            onSubscribePublicChannelClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.contacts_join_super_group)) },
                        onClick = {
                            expanded = false
                            onJoinSuperGroupClick()
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
                        title = stringResource(R.string.empty_friend_requests_title),
                        body = stringResource(R.string.empty_friend_requests_body),
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
                            Button(onClick = { viewModel.accept(item.requestId) }) { Text(stringResource(R.string.accept)) }
                            Button(onClick = { viewModel.reject(item.requestId) }) { Text(stringResource(R.string.reject)) }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.contacts_section_title)) }
            if (uiState.contacts.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.empty_contacts_title),
                        body = stringResource(R.string.empty_contacts_body),
                    )
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
