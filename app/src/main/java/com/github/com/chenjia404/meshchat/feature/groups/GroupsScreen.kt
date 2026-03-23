package com.github.com.chenjia404.meshchat.feature.groups

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupUiModel(
    val groupId: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String?,
)

data class GroupsUiState(
    val items: List<GroupUiModel> = emptyList(),
)

/** 创建群聊时从联系人多选成员 */
data class CreateGroupContactRow(
    val peerId: String,
    val title: String,
    val subtitle: String,
    val avatarUrl: String?,
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
) : ViewModel() {
    val uiState: StateFlow<GroupsUiState> = groupRepository.groups
        .map { groups ->
            GroupsUiState(
                items = groups.map {
                    GroupUiModel(
                        groupId = it.groupId,
                        title = it.title,
                        subtitle = "${it.state} · ${it.memberCount ?: 0}人",
                        avatarUrl = attachmentUrlBuilder.avatarUrl(it.avatar),
                    )
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    /** 未拉黑的好友，用于创建群时多选 */
    val createGroupContacts: StateFlow<List<CreateGroupContactRow>> =
        contactsRepository.contacts
            .map { contacts ->
                contacts
                    .filter { !it.blocked }
                    .map { c ->
                        CreateGroupContactRow(
                            peerId = c.peerId,
                            title = c.nickname.ifBlank { c.remoteNickname ?: c.peerId },
                            subtitle = c.peerId,
                            avatarUrl = attachmentUrlBuilder.avatarUrl(c.avatar),
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createGroup(title: String, memberPeerIds: List<String>) {
        viewModelScope.launch { groupRepository.createGroup(title, memberPeerIds) }
    }
}

@Composable
fun GroupsScreen(
    onGroupClick: (String) -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionTitle("群组列表")
        }
        if (uiState.items.isEmpty()) {
            item {
                EmptyState(title = "暂无群组", body = "创建一个群组后，这里会展示所有群聊。")
            }
        } else {
            items(uiState.items, key = { it.groupId }) { item ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { onGroupClick(item.groupId) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarImage(item.title, item.avatarUrl, Modifier.size(46.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
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

