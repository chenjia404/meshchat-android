package com.github.com.chenjia404.meshchat.feature.groupchat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageBubble
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageUiModel
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.service.audio.ChatVoiceInlinePlayer
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType
import com.github.com.chenjia404.meshchat.core.util.attachmentSubtitle
import com.github.com.chenjia404.meshchat.core.util.requiresDownloadPipeForForward
import com.github.com.chenjia404.meshchat.core.util.copyChatMessageToClipboard
import com.github.com.chenjia404.meshchat.core.util.resolveRenderType
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.domain.usecase.ForwardMessageUseCase
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetPickerDialog
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetRowItem
import com.github.com.chenjia404.meshchat.feature.forward.ForwardUploadOverlay
import com.github.com.chenjia404.meshchat.feature.forward.toForwardDestination
import com.github.com.chenjia404.meshchat.service.download.ApkInstallHelper
import com.github.com.chenjia404.meshchat.service.download.FileDownloadService
import com.github.com.chenjia404.meshchat.service.download.PublicDownloadResult
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack

data class GroupChatUiState(
    val groupId: String = "",
    val title: String = "",
    val avatarUrl: String? = null,
    val messages: List<ChatMessageUiModel> = emptyList(),
)

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val groupRepository: GroupRepository,
    profileRepository: ProfileRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val uriFileResolver: UriFileResolver,
    private val fileDownloadService: FileDownloadService,
    private val forwardMessageUseCase: ForwardMessageUseCase,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val uiState: StateFlow<GroupChatUiState> = combine(
        groupRepository.observeGroup(groupId),
        groupRepository.observeMessages(groupId),
        profileRepository.myProfile,
    ) { group, messages, profile ->
        val myPeerId = profile?.peerId
        GroupChatUiState(
            groupId = groupId,
            title = group?.title ?: groupId,
            avatarUrl = attachmentUrlBuilder.avatarUrl(group?.avatar),
            messages = messages.map { message ->
                val renderType = resolveRenderType(message.msgType, message.mimeType, message.fileName)
                ChatMessageUiModel(
                    id = message.msgId,
                    title = message.senderPeerId,
                    subtitle = attachmentSubtitle(appContext.resources, renderType, message.mimeType, message.msgType),
                    isMine = myPeerId != null && myPeerId == message.senderPeerId,
                    renderType = renderType,
                    text = message.plaintext.orEmpty(),
                    fileName = message.fileName,
                    remoteUrl = if (renderType == AttachmentRenderType.TEXT || renderType == AttachmentRenderType.SYSTEM) {
                        null
                    } else {
                        attachmentUrlBuilder.groupFileUrl(groupId, message.msgId)
                    },
                    timestamp = message.createdAt,
                    state = message.state,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupChatUiState())

    private val _forwardAttachmentUploading = MutableStateFlow(false)
    val forwardAttachmentUploading: StateFlow<Boolean> = _forwardAttachmentUploading.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.refreshGroup(groupId)
            groupRepository.refreshMessages(groupId)
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { groupRepository.sendText(groupId, text) }
    }

    fun sendAttachment(uri: Uri) {
        viewModelScope.launch {
            val file = uriFileResolver.copyToCache(uri)
            groupRepository.sendFile(groupId, file)
        }
    }

    fun revoke(msgId: String) {
        viewModelScope.launch { groupRepository.revokeMessage(groupId, msgId) }
    }

    fun sync(fromPeerId: String) {
        if (fromPeerId.isBlank()) return
        viewModelScope.launch { groupRepository.syncGroup(groupId, fromPeerId) }
    }

    fun download(message: ChatMessageUiModel) {
        val url = message.remoteUrl ?: return
        val fileName = message.fileName ?: appContext.getString(R.string.default_attachment_filename)
        viewModelScope.launch {
            runCatching {
                when (val result = fileDownloadService.downloadToPublicDownloads(url, fileName)) {
                    is PublicDownloadResult.Success -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                appContext.getString(R.string.toast_saved_to_downloads),
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (fileName.endsWith(".apk", ignoreCase = true)) {
                                ApkInstallHelper.tryStartInstall(appContext, result.uri)
                            }
                        }
                    }
                    PublicDownloadResult.QueuedSystemDownload -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                appContext.getString(R.string.toast_queued_system_download),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        e.message ?: appContext.getString(R.string.error_download_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun forwardMessage(
        message: ChatMessageUiModel,
        selectedTargets: List<ForwardTargetRowItem>,
        onDone: (Boolean, String?) -> Unit,
    ) {
        val destinations = selectedTargets.mapNotNull { it.toForwardDestination() }
        if (destinations.isEmpty()) {
            onDone(false, appContext.getString(R.string.forward_no_target))
            return
        }
        val useAppScope = message.renderType.requiresDownloadPipeForForward()
        val scope = if (useAppScope) applicationScope else viewModelScope
        scope.launch {
            if (useAppScope) {
                _forwardAttachmentUploading.value = true
            }
            try {
                runCatching {
                    forwardMessageUseCase.forward(
                        renderType = message.renderType,
                        plainText = message.text,
                        subtitle = message.subtitle,
                        fileName = message.fileName,
                        downloadUrl = message.remoteUrl,
                        destinations = destinations,
                    )
                }.onSuccess {
                    withContext(Dispatchers.Main) {
                        onDone(true, null)
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        onDone(false, e.message ?: appContext.getString(R.string.error_forward_failed))
                    }
                }
            } finally {
                if (useAppScope) {
                    _forwardAttachmentUploading.value = false
                }
            }
        }
    }
}

@Composable
fun GroupChatScreen(
    onBackClick: () -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    onOpenAudio: (String, String) -> Unit,
    viewModel: GroupChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val forwardUploading by viewModel.forwardAttachmentUploading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var messageToForward by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var input by remember { mutableStateOf("") }
    var syncPeerId by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::sendAttachment)
    }

    DisposableEffect(Unit) {
        onDispose { ChatVoiceInlinePlayer.stop() }
    }

    var previousLastMessageId by remember(uiState.groupId) { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.messages.lastOrNull()?.id, uiState.messages.size) {
        val list = uiState.messages
        val last = list.lastOrNull() ?: run {
            previousLastMessageId = null
            return@LaunchedEffect
        }
        val id = last.id
        if (previousLastMessageId == id) return@LaunchedEffect
        val oldId = previousLastMessageId
        previousLastMessageId = id
        if (!last.isMine) return@LaunchedEffect
        when {
            oldId == null && list.size == 1 -> listState.animateScrollToItem(0)
            oldId != null && oldId != id -> listState.animateScrollToItem(list.lastIndex)
        }
    }

    if (messageToForward != null) {
        ForwardTargetPickerDialog(
            excludeDirectConversationId = null,
            excludeGroupId = uiState.groupId,
            onDismiss = { messageToForward = null },
            onConfirm = { rows ->
                val msg = messageToForward
                messageToForward = null
                if (msg != null) {
                    viewModel.forwardMessage(msg, rows) { ok, err ->
                        if (ok) {
                            Toast.makeText(context, context.getString(R.string.toast_forwarded), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                err ?: context.getString(R.string.error_forward_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            AvatarImage(uiState.title.ifBlank { stringResource(R.string.group_short_name) }, uiState.avatarUrl, Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(uiState.title.ifBlank { stringResource(R.string.group_chat_title) }, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.group_chat_subtitle_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = syncPeerId,
            onValueChange = { syncPeerId = it },
            label = { Text(stringResource(R.string.label_sync_peer_id)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.sync(syncPeerId) },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.group_sync_button))
        }
        if (uiState.messages.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_group_messages_title),
                body = stringResource(R.string.empty_group_messages_body),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(uiState.messages, key = { it.id }) { item ->
                    ChatMessageBubble(
                        message = item,
                        showSenderName = true,
                        onOpenAttachment = { message ->
                            when (message.renderType) {
                                AttachmentRenderType.IMAGE -> message.remoteUrl?.let {
                                    onOpenImage(it, message.fileName ?: context.getString(R.string.fallback_title_image))
                                }
                                AttachmentRenderType.VIDEO -> message.remoteUrl?.let {
                                    onOpenVideo(it, message.fileName ?: context.getString(R.string.fallback_title_video))
                                }
                                AttachmentRenderType.AUDIO -> message.remoteUrl?.let {
                                    onOpenAudio(it, message.fileName ?: context.getString(R.string.fallback_title_audio))
                                }
                                AttachmentRenderType.FILE -> viewModel.download(message)
                                else -> Unit
                            }
                        },
                        onCopy = { m ->
                            copyChatMessageToClipboard(context, m)
                            Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                        },
                        onForward = { messageToForward = it },
                        onRevoke = { viewModel.revoke(it.id) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.attach)) }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.input_group_message)) },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                viewModel.sendText(input)
                input = ""
            }) {
                Text(stringResource(R.string.send))
            }
        }
        }
        ForwardUploadOverlay(visible = forwardUploading)
    }
}
