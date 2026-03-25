package com.github.com.chenjia404.meshchat.feature.forward

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.com.chenjia404.meshchat.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.domain.usecase.ForwardDestination

fun ForwardTargetRowItem.toForwardDestination(): ForwardDestination? = when {
    id.startsWith("d:") -> ForwardDestination.Direct(id.removePrefix("d:"))
    id.startsWith("g:") -> ForwardDestination.Group(id.removePrefix("g:"))
    id.startsWith("pc:") -> ForwardDestination.PublicChannel(id.removePrefix("pc:"))
    else -> null
}

@Composable
fun ForwardTargetPickerDialog(
    excludeDirectConversationId: String?,
    excludeGroupId: String?,
    excludePublicChannelId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<ForwardTargetRowItem>) -> Unit,
    viewModel: ForwardTargetPickerViewModel = hiltViewModel(),
) {
    val allRows by viewModel.rows.collectAsStateWithLifecycle()
    val rows = remember(allRows, excludeDirectConversationId, excludeGroupId, excludePublicChannelId) {
        allRows.filter { row ->
            val exD = excludeDirectConversationId?.let { "d:$it" }
            val exG = excludeGroupId?.let { "g:$it" }
            val exP = excludePublicChannelId?.let { "pc:$it" }
            row.id != exD && row.id != exG && row.id != exP
        }
    }
    var selected by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(excludeDirectConversationId, excludeGroupId, excludePublicChannelId) {
        selected = emptySet()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshTargets()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("转发到") },
        text = {
            if (rows.isEmpty()) {
                Text(
                    "暂无其他会话，请先创建私聊或加入群聊。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(rows, key = { it.id }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = row.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + row.id else selected - row.id
                                },
                            )
                            AvatarImage(
                                title = row.title,
                                avatarUrl = row.avatarUrl,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(
                                        when {
                                            row.isPublicChannel -> R.string.forward_target_public_channel
                                            row.isGroup -> R.string.forward_target_group
                                            else -> R.string.forward_target_direct
                                        },
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = rows.filter { it.id in selected }
                    onConfirm(chosen)
                },
                enabled = selected.isNotEmpty(),
            ) {
                Text(stringResource(R.string.forward))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
