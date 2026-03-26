package com.github.com.chenjia404.meshchat.feature.publicchannel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageBubble
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageUiModel
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType
import com.github.com.chenjia404.meshchat.core.util.attachmentSubtitle
import com.github.com.chenjia404.meshchat.core.util.requiresDownloadPipeForForward
import com.github.com.chenjia404.meshchat.core.util.copyChatMessageToClipboard
import com.github.com.chenjia404.meshchat.core.util.resolvePublicChannelRenderType
import com.github.com.chenjia404.meshchat.domain.model.PublicChannelMessage
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.domain.usecase.ForwardMessageUseCase
import com.github.com.chenjia404.meshchat.feature.directchat.voice.ChatInputMode
import com.github.com.chenjia404.meshchat.feature.directchat.voice.DirectChatInputBar
import com.github.com.chenjia404.meshchat.feature.directchat.voice.HoldToTalkButton
import com.github.com.chenjia404.meshchat.feature.directchat.voice.VoiceRecordOverlay
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetPickerDialog
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetRowItem
import com.github.com.chenjia404.meshchat.feature.forward.ForwardUploadOverlay
import com.github.com.chenjia404.meshchat.feature.forward.toForwardDestination
import com.github.com.chenjia404.meshchat.service.audio.ChatVoiceInlinePlayer
import com.github.com.chenjia404.meshchat.service.audio.VoiceRecorder
import com.github.com.chenjia404.meshchat.service.download.ApkInstallHelper
import com.github.com.chenjia404.meshchat.service.download.FileDownloadService
import com.github.com.chenjia404.meshchat.service.download.PublicDownloadResult
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PublicChannelChatUiState(
    val channelId: String = "",
    val title: String = "",
    val avatarUrl: String? = null,
    val isOwner: Boolean = false,
    val messages: List<ChatMessageUiModel> = emptyList(),
)

@HiltViewModel
class PublicChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val publicChannelRepository: PublicChannelRepository,
    profileRepository: ProfileRepository,
    contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val uriFileResolver: UriFileResolver,
    private val fileDownloadService: FileDownloadService,
    private val forwardMessageUseCase: ForwardMessageUseCase,
) : ViewModel() {
    private val channelId: String = checkNotNull(savedStateHandle["channelId"])

    val uiState: StateFlow<PublicChannelChatUiState> = combine(
        publicChannelRepository.observeChannel(channelId),
        publicChannelRepository.observeMessages(channelId),
        profileRepository.myProfile,
        contactsRepository.contacts,
    ) { channel, messages, profile, contacts ->
        val ownerPeer = channel?.ownerPeerId.orEmpty()
        val myId = profile?.peerId
        val isOwner = myId != null && ownerPeer == myId
        val ownerContact = contacts.firstOrNull { it.peerId == ownerPeer }
        val ownerLabel = ownerContact?.remoteNickname?.takeIf { it.isNotBlank() }
            ?: ownerContact?.nickname?.takeIf { it.isNotBlank() }
            ?: ownerPeer
        PublicChannelChatUiState(
            channelId = channelId,
            title = channel?.name?.takeIf { it.isNotBlank() } ?: channelId,
            avatarUrl = channel?.avatarUrl,
            isOwner = isOwner,
            messages = messages.map { msg ->
                mapMessageToUi(
                    msg = msg,
                    myPeerId = myId,
                    ownerDisplayName = ownerLabel,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PublicChannelChatUiState())

    private val _forwardAttachmentUploading = MutableStateFlow(false)
    val forwardAttachmentUploading: StateFlow<Boolean> = _forwardAttachmentUploading.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                publicChannelRepository.syncChannelOnOpen(channelId)
            }
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { publicChannelRepository.sendText(channelId, text) }
                .onFailure { e ->
                    Toast.makeText(
                        appContext,
                        e.message ?: appContext.getString(R.string.error_request_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    fun sendAttachment(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val file = uriFileResolver.copyToCache(uri)
                publicChannelRepository.sendFile(channelId, file)
            }.onFailure { e ->
                Toast.makeText(
                    appContext,
                    e.message ?: appContext.getString(R.string.error_request_failed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    fun sendAttachment(file: File) {
        viewModelScope.launch {
            runCatching { publicChannelRepository.sendFile(channelId, file) }
                .onFailure { e ->
                    Toast.makeText(
                        appContext,
                        e.message ?: appContext.getString(R.string.error_request_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    fun revokeMessage(messageId: Long) {
        viewModelScope.launch {
            runCatching { publicChannelRepository.revokeMessage(channelId, messageId) }
                .onFailure { e ->
                    Toast.makeText(
                        appContext,
                        e.message ?: appContext.getString(R.string.error_request_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    fun updateMessageText(messageId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { publicChannelRepository.updateMessageText(channelId, messageId, text) }
                .onFailure { e ->
                    Toast.makeText(
                        appContext,
                        e.message ?: appContext.getString(R.string.error_request_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    fun download(message: ChatMessageUiModel) {
        val url = message.remoteUrl ?: return
        val fileName = message.fileName ?: appContext.getString(R.string.default_attachment_filename)
        viewModelScope.launch {
            runCatching {
                when (
                    val result = fileDownloadService.downloadToPublicDownloads(url, fileName)
                ) {
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

    private fun mapMessageToUi(
        msg: PublicChannelMessage,
        myPeerId: String?,
        ownerDisplayName: String,
    ): ChatMessageUiModel {
        val renderType = resolvePublicChannelRenderType(msg.messageType, msg.mimeType, msg.fileName)
        val isDeleted = msg.isDeleted || msg.messageType.equals("deleted", ignoreCase = true)
        val displayText = when {
            isDeleted -> appContext.getString(R.string.public_channel_message_deleted)
            else -> msg.text
        }
        val isMine = myPeerId != null && msg.authorPeerId == myPeerId
        val title = if (isMine) {
            appContext.getString(R.string.sender_me)
        } else {
            ownerDisplayName
        }
        val remoteUrl = when (renderType) {
            AttachmentRenderType.TEXT,
            AttachmentRenderType.SYSTEM,
            -> null
            else -> attachmentUrlBuilder.resolvePublicChannelMediaUrl(
                channelId,
                msg.messageId,
                msg.fileUrl,
                msg.blobId,
            )
        }
        return ChatMessageUiModel(
            id = "${channelId}_${msg.messageId}",
            title = title,
            subtitle = attachmentSubtitle(appContext.resources, renderType, msg.mimeType, msg.messageType),
            isMine = isMine,
            renderType = renderType,
            text = displayText,
            fileName = msg.fileName,
            remoteUrl = remoteUrl,
            timestamp = Instant.ofEpochSecond(msg.createdAtEpoch).toString(),
            state = "sent",
            isDeleted = isDeleted,
        )
    }
}

@Composable
fun PublicChannelScreen(
    onBackClick: () -> Unit,
    onOpenChannelProfile: (channelId: String) -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    onOpenAudio: (String, String) -> Unit,
    viewModel: PublicChannelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val forwardUploading by viewModel.forwardAttachmentUploading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var messageToForward by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var messageToEdit by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var editDraft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var inputMode by remember { mutableStateOf(ChatInputMode.TEXT) }
    var voiceOverlayVisible by remember { mutableStateOf(false) }
    var voiceWillCancel by remember { mutableStateOf(false) }
    var voicePressed by remember { mutableStateOf(false) }
    var voiceElapsedMs by remember { mutableIntStateOf(0) }
    var voiceAmplitude by remember { mutableIntStateOf(0) }
    var voiceRecordingStarted by remember { mutableStateOf(false) }
    var voiceRecordStartElapsed by remember { mutableLongStateOf(0L) }

    val voiceRecorder = remember { VoiceRecorder(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.mic_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::sendAttachment)
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun vibrateShort() {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun interruptVoiceSession(toast: String?) {
        if (!voiceRecorder.isRecording && !voiceOverlayVisible) return
        runCatching { voiceRecorder.cancel() }
        voiceOverlayVisible = false
        voiceRecordingStarted = false
        voiceWillCancel = false
        voicePressed = false
        voiceElapsedMs = 0
        voiceAmplitude = 0
        toast?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    DisposableEffect(voiceRecorder) {
        voiceRecorder.setOnMaxDurationReachedListener {
            val file = voiceRecorder.stop()
            voiceOverlayVisible = false
            voiceRecordingStarted = false
            voicePressed = false
            voiceWillCancel = false
            voiceElapsedMs = 0
            vibrateShort()
            file?.let { viewModel.sendAttachment(it) }
        }
        voiceRecorder.setOnErrorListener {
            interruptVoiceSession(context.getString(R.string.voice_record_failed_retry))
        }
        onDispose {
            voiceRecorder.setOnMaxDurationReachedListener(null)
            voiceRecorder.setOnErrorListener(null)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (voiceOverlayVisible || voiceRecorder.isRecording) {
                    interruptVoiceSession(context.getString(R.string.recording_interrupted))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    DisposableEffect(Unit) {
        onDispose { voiceRecorder.release() }
    }

    DisposableEffect(Unit) {
        onDispose { ChatVoiceInlinePlayer.stop() }
    }

    LaunchedEffect(voiceOverlayVisible) {
        if (!voiceOverlayVisible) return@LaunchedEffect
        while (true) {
            delay(100)
            if (!voiceOverlayVisible || !voiceRecorder.isRecording) break
            voiceElapsedMs = (SystemClock.elapsedRealtime() - voiceRecordStartElapsed).toInt().coerceIn(0, 60_000)
            voiceAmplitude = voiceRecorder.pollMaxAmplitude()
        }
    }

    var previousLastMessageId by remember(uiState.channelId) { mutableStateOf<String?>(null) }
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

    val showJumpLatestFab by remember {
        derivedStateOf {
            val n = uiState.messages.size
            if (n == 0) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf true
            lastVisible < n - 1
        }
    }

    messageToEdit?.let { editing ->
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = { Text(stringResource(R.string.edit_public_channel_message_title)) },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mid = editing.id.substringAfterLast('_').toLongOrNull()
                        if (mid != null && editDraft.isNotBlank()) {
                            viewModel.updateMessageText(mid, editDraft)
                        }
                        messageToEdit = null
                    },
                    enabled = editDraft.isNotBlank(),
                ) {
                    Text(stringResource(R.string.public_channel_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (messageToForward != null) {
        ForwardTargetPickerDialog(
            excludeDirectConversationId = null,
            excludeGroupId = null,
            excludePublicChannelId = uiState.channelId,
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
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenChannelProfile(uiState.channelId) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(
                    title = uiState.title.ifBlank { "?" },
                    avatarUrl = uiState.avatarUrl,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (uiState.messages.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_public_channel_messages_title),
                    body = stringResource(R.string.empty_public_channel_messages_body),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.messages, key = { it.id }) { item ->
                        ChatMessageBubble(
                            message = item,
                            showSenderName = false,
                            showRevokeMenu = uiState.isOwner,
                            isChannelOwner = uiState.isOwner,
                            onEdit = if (uiState.isOwner) {
                                { m ->
                                    messageToEdit = m
                                    editDraft = m.text
                                }
                            } else {
                                null
                            },
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
                            onRevoke = { m ->
                                val mid = m.id.substringAfterLast('_').toLongOrNull()
                                if (mid != null) viewModel.revokeMessage(mid)
                            },
                        )
                    }
                }
            }
            if (showJumpLatestFab && uiState.messages.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = {
                        val last = uiState.messages.lastIndex
                        scope.launch { listState.animateScrollToItem(last) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp),
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_scroll_latest))
                }
            }
            if (voiceOverlayVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    VoiceRecordOverlay(
                        elapsedMs = voiceElapsedMs,
                        maxMs = 60_000,
                        willCancel = voiceWillCancel,
                        amplitude = voiceAmplitude,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.isOwner) {
            DirectChatInputBar(
                inputMode = inputMode,
                onToggleInputMode = {
                    if (voiceOverlayVisible) return@DirectChatInputBar
                    inputMode = if (inputMode == ChatInputMode.TEXT) ChatInputMode.VOICE else ChatInputMode.TEXT
                },
                text = input,
                onTextChange = { input = it },
                onSendText = {
                    viewModel.sendText(input)
                    input = ""
                },
                onPickAttachment = { picker.launch(arrayOf("*/*")) },
                voiceOverlayVisible = voiceOverlayVisible,
                holdToTalkContent = { mod ->
                    HoldToTalkButton(
                        modifier = mod,
                        enabled = true,
                        pressedVisual = voicePressed && voiceOverlayVisible,
                        willCancelVisual = voiceWillCancel,
                        onLongPressStarted = {
                            if (!hasMicPermission()) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                Toast.makeText(context, context.getString(R.string.mic_permission_denied), Toast.LENGTH_SHORT).show()
                                return@HoldToTalkButton
                            }
                            runCatching {
                                voiceRecorder.start()
                                voiceRecordingStarted = true
                                voiceRecordStartElapsed = SystemClock.elapsedRealtime()
                                voiceOverlayVisible = true
                                voicePressed = true
                                voiceWillCancel = false
                                voiceElapsedMs = 0
                            }.onFailure {
                                voiceRecordingStarted = false
                                voiceOverlayVisible = false
                                Toast.makeText(context, context.getString(R.string.voice_record_failed_retry), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCancelZoneChanged = { voiceWillCancel = it },
                        onGestureEnd = { cancelledBySwipe ->
                            voicePressed = false
                            if (!voiceRecordingStarted) return@HoldToTalkButton
                            voiceRecordingStarted = false
                            voiceOverlayVisible = false
                            voiceWillCancel = false
                            val durationMs = (SystemClock.elapsedRealtime() - voiceRecordStartElapsed).coerceAtLeast(0L)
                            if (cancelledBySwipe) {
                                voiceRecorder.cancel()
                                Toast.makeText(context, context.getString(R.string.voice_record_cancelled), Toast.LENGTH_SHORT).show()
                                return@HoldToTalkButton
                            }
                            val file = voiceRecorder.stop()
                            when {
                                durationMs < 1000L -> {
                                    file?.delete()
                                    Toast.makeText(context, context.getString(R.string.voice_too_short), Toast.LENGTH_SHORT).show()
                                }
                                file != null -> viewModel.sendAttachment(file)
                                else -> Toast.makeText(context, context.getString(R.string.voice_record_failed_retry), Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                },
            )
        } else {
            Text(
                text = stringResource(R.string.public_channel_read_only_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        }
        ForwardUploadOverlay(visible = forwardUploading)
    }
}
