package com.github.com.chenjia404.meshchat.feature.share

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetPickerViewModel
import com.github.com.chenjia404.meshchat.share.IncomingShareManager

@Composable
fun ShareTargetsScreen(
    incomingShareManager: IncomingShareManager,
    onBack: () -> Unit,
    shareViewModel: ShareTargetsViewModel = hiltViewModel(),
    pickerViewModel: ForwardTargetPickerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val pending by incomingShareManager.pending.collectAsStateWithLifecycle()
    val rows by pickerViewModel.rows.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pickerViewModel.refreshTargets()
    }

    BackHandler {
        incomingShareManager.clear()
        onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = {
                    incomingShareManager.clear()
                    onBack()
                },
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.share_targets_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                pending?.let { p ->
                    val n = p.uris.size
                    Text(
                        stringResource(R.string.share_targets_subtitle, n),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(
                onClick = {
                    val chosen = rows.filter { it.id in selected }
                    if (chosen.isEmpty() || sending) return@TextButton
                    sending = true
                    shareViewModel.sendToTargets(chosen) { ok, err ->
                        sending = false
                        if (ok) {
                            Toast.makeText(context, context.getString(R.string.share_targets_toast_sent), Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(
                                context,
                                err ?: context.getString(R.string.error_request_failed),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
                enabled = selected.isNotEmpty() && !sending && pending != null,
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.share_targets_send))
                }
            }
        }

        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.share_targets_empty),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
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
    }
}
