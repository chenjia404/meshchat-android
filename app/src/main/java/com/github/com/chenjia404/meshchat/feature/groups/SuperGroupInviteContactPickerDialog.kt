package com.github.com.chenjia404.meshchat.feature.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage

data class SuperGroupInviteContactRow(
    val peerId: String,
    val title: String,
    val avatarUrl: String?,
)

@Composable
fun SuperGroupInviteContactPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    viewModel: SuperGroupInviteContactPickerViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        selected = emptySet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.super_group_invite_pick_contacts)) },
        text = {
            if (rows.isEmpty()) {
                Text(
                    stringResource(R.string.super_group_invite_no_direct_contacts),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(rows, key = { it.peerId }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = row.peerId in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + row.peerId else selected - row.peerId
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
                                    row.peerId,
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
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
            ) {
                Text(stringResource(R.string.super_group_invite_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
