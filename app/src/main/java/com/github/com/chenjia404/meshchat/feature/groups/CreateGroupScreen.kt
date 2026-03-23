package com.github.com.chenjia404.meshchat.feature.groups

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.com.chenjia404.meshchat.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState

@Composable
fun CreateGroupScreen(
    onBackClick: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val contacts by viewModel.createGroupContacts.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var selectedPeerIds by remember { mutableStateOf(setOf<String>()) }

    fun togglePeer(peerId: String) {
        selectedPeerIds =
            if (peerId in selectedPeerIds) selectedPeerIds - peerId else selectedPeerIds + peerId
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.create_group_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.create_group_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.label_group_title)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.select_members), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "暂无好友",
                    body = "添加联系人后，可在此多选成员创建群聊。",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                items(contacts, key = { it.peerId }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { togglePeer(row.peerId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = row.peerId in selectedPeerIds,
                            onCheckedChange = { togglePeer(row.peerId) },
                        )
                        AvatarImage(row.title, row.avatarUrl, Modifier.size(46.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(row.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                row.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (title.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.toast_enter_group_title), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedPeerIds.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.toast_select_at_least_one), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.createGroup(title, selectedPeerIds.toList())
                title = ""
                selectedPeerIds = emptySet()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.create_group_button))
        }
    }
}
