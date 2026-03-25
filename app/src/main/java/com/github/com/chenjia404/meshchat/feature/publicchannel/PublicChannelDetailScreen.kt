package com.github.com.chenjia404.meshchat.feature.publicchannel

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.domain.model.PublicChannelDetail
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicChannelDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: PublicChannelDetail? = null,
    val isOwner: Boolean = false,
    val editing: Boolean = false,
    val draftName: String = "",
    val draftBio: String = "",
    val saving: Boolean = false,
    val avatarUploading: Boolean = false,
    val unsubscribing: Boolean = false,
)

@HiltViewModel
class PublicChannelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val publicChannelRepository: PublicChannelRepository,
    private val profileRepository: ProfileRepository,
    private val uriFileResolver: UriFileResolver,
) : ViewModel() {
    private val channelId: String = checkNotNull(savedStateHandle["channelId"])

    private val _uiState = MutableStateFlow(PublicChannelDetailUiState())
    val uiState: StateFlow<PublicChannelDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val detail = publicChannelRepository.getChannelDetail(channelId)
                val myId = profileRepository.myProfile.first()?.peerId
                val isOwner = myId != null && myId == detail.ownerPeerId
                _uiState.update {
                    it.copy(
                        loading = false,
                        detail = detail,
                        draftName = detail.name,
                        draftBio = detail.bio,
                        isOwner = isOwner,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(loading = false, error = e.message ?: "error")
                }
            }
        }
    }

    fun setDraftName(value: String) {
        _uiState.update { it.copy(draftName = value) }
    }

    fun setDraftBio(value: String) {
        _uiState.update { it.copy(draftBio = value) }
    }

    fun setEditing(value: Boolean) {
        val d = _uiState.value.detail
        _uiState.update {
            if (value && d != null) {
                it.copy(editing = true, draftName = d.name, draftBio = d.bio)
            } else {
                it.copy(
                    editing = false,
                    draftName = d?.name ?: it.draftName,
                    draftBio = d?.bio ?: it.draftBio,
                )
            }
        }
    }

    fun save(onDone: (Boolean, String?) -> Unit) {
        val s = _uiState.value
        if (!s.isOwner || s.saving) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            runCatching {
                publicChannelRepository.updateChannelProfile(channelId, s.draftName, s.draftBio)
                val detail = publicChannelRepository.getChannelDetail(channelId)
                _uiState.update {
                    it.copy(
                        saving = false,
                        editing = false,
                        detail = detail,
                        draftName = detail.name,
                        draftBio = detail.bio,
                    )
                }
                onDone(true, null)
            }.onFailure { e ->
                _uiState.update { it.copy(saving = false) }
                onDone(false, e.message)
            }
        }
    }

    fun uploadAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(avatarUploading = true) }
            runCatching {
                val file = uriFileResolver.copyToCache(uri)
                publicChannelRepository.uploadChannelAvatar(channelId, file)
                val detail = publicChannelRepository.getChannelDetail(channelId)
                _uiState.update {
                    it.copy(avatarUploading = false, detail = detail)
                }
                Toast.makeText(appContext, appContext.getString(R.string.public_channel_toast_avatar_updated), Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                _uiState.update { it.copy(avatarUploading = false) }
                Toast.makeText(
                    appContext,
                    e.message ?: appContext.getString(R.string.error_request_failed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    fun unsubscribe(onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(unsubscribing = true) }
            runCatching {
                publicChannelRepository.unsubscribe(channelId)
                _uiState.update { it.copy(unsubscribing = false) }
                onDone(true, null)
            }.onFailure { e ->
                _uiState.update { it.copy(unsubscribing = false) }
                onDone(false, e.message)
            }
        }
    }
}

private fun formatEpochSeconds(epochSeconds: Long): String {
    return runCatching {
        val z = ZoneId.systemDefault()
        Instant.ofEpochSecond(epochSeconds).atZone(z).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }.getOrDefault(epochSeconds.toString())
}

@Composable
fun PublicChannelDetailScreen(
    onBackClick: () -> Unit,
    onUnsubscribed: () -> Unit = {},
    viewModel: PublicChannelDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showUnsubscribeConfirm by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::uploadAvatarFromUri) }

    if (showUnsubscribeConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsubscribeConfirm = false },
            title = { Text(stringResource(R.string.public_channel_unsubscribe_confirm_title)) },
            text = { Text(stringResource(R.string.public_channel_unsubscribe_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsubscribeConfirm = false
                        viewModel.unsubscribe { ok, err ->
                            if (ok) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.public_channel_toast_unsubscribed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onUnsubscribed()
                            } else {
                                Toast.makeText(
                                    context,
                                    err ?: context.getString(R.string.error_request_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    enabled = !state.unsubscribing,
                ) {
                    Text(stringResource(R.string.public_channel_unsubscribe_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsubscribeConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                stringResource(R.string.public_channel_profile_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.isOwner && !state.editing && state.detail != null) {
                TextButton(onClick = { viewModel.setEditing(true) }) {
                    Text(stringResource(R.string.public_channel_action_edit))
                }
            }
        }

        when {
            state.loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        title = stringResource(R.string.public_channel_profile_load_error),
                        body = state.error ?: "",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) {
                        Text(stringResource(R.string.public_channel_action_retry))
                    }
                }
            }
            state.detail != null -> {
                PublicChannelDetailContent(
                    detail = state.detail!!,
                    isOwner = state.isOwner,
                    editing = state.editing,
                    saving = state.saving,
                    avatarUploading = state.avatarUploading,
                    unsubscribing = state.unsubscribing,
                    draftName = state.draftName,
                    draftBio = state.draftBio,
                    onDraftName = viewModel::setDraftName,
                    onDraftBio = viewModel::setDraftBio,
                    onCancelEdit = { viewModel.setEditing(false) },
                    onPickAvatar = { avatarPicker.launch(arrayOf("image/*")) },
                    onUnsubscribeClick = { showUnsubscribeConfirm = true },
                    onSave = {
                        viewModel.save { ok, err ->
                            if (ok) {
                                Toast.makeText(context, context.getString(R.string.public_channel_toast_saved), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    err ?: context.getString(R.string.error_request_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    onCopyUuid = {
                        clipboard.setText(AnnotatedString(state.detail!!.channelId))
                        Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                    },
                    onCopyOwner = {
                        clipboard.setText(AnnotatedString(state.detail!!.ownerPeerId))
                        Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun PublicChannelDetailContent(
    detail: PublicChannelDetail,
    isOwner: Boolean,
    editing: Boolean,
    saving: Boolean,
    avatarUploading: Boolean,
    unsubscribing: Boolean,
    draftName: String,
    draftBio: String,
    onDraftName: (String) -> Unit,
    onDraftBio: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onPickAvatar: () -> Unit,
    onUnsubscribeClick: () -> Unit,
    onSave: () -> Unit,
    onCopyUuid: () -> Unit,
    onCopyOwner: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AvatarImage(
                title = detail.name.ifBlank { "?" },
                avatarUrl = detail.avatarUrl,
                modifier = Modifier.size(88.dp),
            )
            if (avatarUploading) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            }
        }
        if (isOwner) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickAvatar,
                enabled = !avatarUploading && !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.public_channel_change_avatar))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = detail.channelId,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_channel_uuid)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCopyUuid, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.public_channel_copy_uuid))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = detail.ownerPeerId,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_owner_peer_id)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCopyOwner, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.public_channel_copy_owner))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.label_public_channel_meta), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(
                R.string.public_channel_meta_line,
                detail.profileVersion,
                detail.ownerVersion,
                detail.lastSeq,
                detail.lastMessageId,
                if (detail.subscribed) {
                    stringResource(R.string.public_channel_subscribed_yes)
                } else {
                    stringResource(R.string.public_channel_subscribed_no)
                },
                detail.lastSeenSeq,
                detail.lastSyncedSeq,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.public_channel_times,
                formatEpochSeconds(detail.createdAtEpoch),
                formatEpochSeconds(detail.updatedAtEpoch)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))
        if (isOwner && editing) {
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftName,
                label = { Text(stringResource(R.string.label_channel_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = draftBio,
                onValueChange = onDraftBio,
                label = { Text(stringResource(R.string.label_channel_bio)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !saving && draftName.isNotBlank(),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        stringResource(
                            if (saving) {
                                R.string.public_channel_saving
                            } else {
                                R.string.public_channel_action_save
                            },
                        ),
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = detail.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_channel_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = detail.bio,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_channel_bio)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }

        if (!isOwner && detail.subscribed) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onUnsubscribeClick,
                enabled = !unsubscribing && !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (unsubscribing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.public_channel_unsubscribe))
            }
        }
    }
}
