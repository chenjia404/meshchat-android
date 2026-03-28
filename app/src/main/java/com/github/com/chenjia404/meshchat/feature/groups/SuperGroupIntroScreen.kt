package com.github.com.chenjia404.meshchat.feature.groups

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperGroupIntroScreen(
    onBackClick: () -> Unit,
    onLeftGroup: () -> Unit = {},
    viewModel: SuperGroupIntroViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showInvitePicker by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::setPendingAvatarUri) }

    if (showInvitePicker) {
        SuperGroupInviteContactPickerDialog(
            onDismiss = { showInvitePicker = false },
            onConfirm = { peerIds ->
                showInvitePicker = false
                viewModel.inviteMembers(peerIds) { ok, err ->
                    if (ok) {
                        Toast.makeText(context, context.getString(R.string.super_group_invite_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            err ?: context.getString(R.string.super_group_invite_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.leaving) showLeaveConfirm = false },
            title = { Text(stringResource(R.string.super_group_intro_leave_confirm_title)) },
            text = { Text(stringResource(R.string.super_group_intro_leave_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveGroup { ok, err ->
                            showLeaveConfirm = false
                            if (ok) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.super_group_intro_leave_success),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onLeftGroup()
                            } else {
                                Toast.makeText(
                                    context,
                                    err ?: context.getString(R.string.error_request_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    enabled = !state.leaving,
                ) {
                    if (state.leaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.super_group_intro_leave_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLeaveConfirm = false },
                    enabled = !state.leaving,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.super_group_intro_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (state.canEditGroupInfo && !state.loading && state.error == null) {
                        if (state.editing) {
                            TextButton(
                                onClick = { viewModel.cancelEdit() },
                                enabled = !state.saving,
                            ) {
                                Text(stringResource(R.string.super_group_intro_cancel))
                            }
                            TextButton(
                                onClick = {
                                    viewModel.saveEdits { ok, err ->
                                        if (ok) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.super_group_intro_save_success),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                err ?: context.getString(R.string.super_group_intro_save_failed),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                },
                                enabled = !state.saving,
                            ) {
                                if (state.saving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(stringResource(R.string.super_group_intro_save))
                                }
                            }
                        } else {
                            TextButton(onClick = { viewModel.enterEdit() }) {
                                Text(stringResource(R.string.super_group_intro_edit))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Button(onClick = { viewModel.load() }) {
                        Text(stringResource(R.string.retry))
                    }
                    if (state.canLeave) {
                        Spacer(modifier = Modifier.size(24.dp))
                        OutlinedButton(
                            onClick = { showLeaveConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.leaving,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(stringResource(R.string.super_group_intro_leave_group))
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val avatarClick = {
                            if (state.editing) {
                                avatarPicker.launch(arrayOf("image/*"))
                            }
                        }
                        BoxAvatarOrPending(
                            editing = state.editing,
                            title = if (state.editing) state.draftTitle else state.title,
                            avatarUrl = state.avatarUrl,
                            pendingUri = state.pendingAvatarUri,
                            onAvatarClick = avatarClick,
                        )
                        if (state.editing) {
                            OutlinedTextField(
                                value = state.draftTitle,
                                onValueChange = viewModel::updateDraftTitle,
                                label = { Text(stringResource(R.string.super_group_intro_label_title)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !state.saving,
                            )
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    state.title.ifBlank { state.groupId },
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                    }
                    if (state.editing) {
                        Spacer(modifier = Modifier.size(8.dp))
                        TextButton(
                            onClick = { avatarPicker.launch(arrayOf("image/*")) },
                            enabled = !state.saving,
                        ) {
                            Text(stringResource(R.string.super_group_intro_change_avatar))
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(R.string.super_group_intro_about_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.editing) {
                        OutlinedTextField(
                            value = state.draftAbout,
                            onValueChange = viewModel::updateDraftAbout,
                            label = { Text(stringResource(R.string.super_group_intro_about_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            minLines = 3,
                            enabled = !state.saving,
                        )
                    } else {
                        Text(
                            state.about?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.super_group_intro_about_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    val visEffective =
                        if (state.editing) state.draftMemberListVisibility
                        else state.memberListVisibility ?: SuperGroupServerSettings.MEMBER_LIST_VISIBLE
                    val joinEffective =
                        if (state.editing) state.draftJoinMode
                        else state.joinMode ?: SuperGroupServerSettings.JOIN_INVITE_ONLY
                    Text(
                        stringResource(R.string.super_group_intro_member_list_visibility_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.editing) {
                        SuperGroupMemberListVisibilityDropdown(
                            selected = visEffective,
                            onSelect = viewModel::updateDraftMemberListVisibility,
                            enabled = !state.saving,
                        )
                    } else {
                        Text(
                            memberListVisibilityDisplayText(visEffective),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        stringResource(R.string.super_group_intro_join_mode_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.editing) {
                        SuperGroupJoinModeDropdown(
                            selected = joinEffective,
                            onSelect = viewModel::updateDraftJoinMode,
                            enabled = !state.saving,
                        )
                    } else {
                        Text(
                            joinModeDisplayText(joinEffective),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.canInvite) {
                        Spacer(modifier = Modifier.size(24.dp))
                        Button(
                            onClick = { showInvitePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.editing,
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.super_group_invite_friends))
                        }
                    }
                    if (state.canLeave) {
                        Spacer(modifier = Modifier.size(16.dp))
                        OutlinedButton(
                            onClick = { showLeaveConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.editing && !state.leaving,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(stringResource(R.string.super_group_intro_leave_group))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun memberListVisibilityDisplayText(apiValue: String): String = when (apiValue) {
    SuperGroupServerSettings.MEMBER_LIST_HIDDEN -> stringResource(R.string.super_group_member_visibility_hidden)
    else -> stringResource(R.string.super_group_member_visibility_visible)
}

@Composable
private fun joinModeDisplayText(apiValue: String): String = when (apiValue) {
    SuperGroupServerSettings.JOIN_OPEN -> stringResource(R.string.super_group_join_mode_open)
    else -> stringResource(R.string.super_group_join_mode_invite_only)
}

@Composable
private fun SuperGroupMemberListVisibilityDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = memberListVisibilityDisplayText(selected),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.super_group_member_visibility_visible)) },
                onClick = {
                    onSelect(SuperGroupServerSettings.MEMBER_LIST_VISIBLE)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.super_group_member_visibility_hidden)) },
                onClick = {
                    onSelect(SuperGroupServerSettings.MEMBER_LIST_HIDDEN)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun SuperGroupJoinModeDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = joinModeDisplayText(selected),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.super_group_join_mode_invite_only)) },
                onClick = {
                    onSelect(SuperGroupServerSettings.JOIN_INVITE_ONLY)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.super_group_join_mode_open)) },
                onClick = {
                    onSelect(SuperGroupServerSettings.JOIN_OPEN)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun BoxAvatarOrPending(
    editing: Boolean,
    title: String,
    avatarUrl: String?,
    pendingUri: Uri?,
    onAvatarClick: () -> Unit,
) {
    val mod = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .then(
            if (editing) {
                Modifier.clickable(onClick = onAvatarClick)
            } else {
                Modifier
            },
        )
    when {
        pendingUri != null -> {
            AsyncImage(
                model = pendingUri,
                contentDescription = title,
                modifier = mod,
                contentScale = ContentScale.Crop,
            )
        }
        else -> {
            AvatarImage(
                title = title.ifBlank { "?" },
                avatarUrl = avatarUrl,
                modifier = mod,
            )
        }
    }
}
