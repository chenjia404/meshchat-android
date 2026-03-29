package com.github.com.chenjia404.meshchat.feature.groupchat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.github.com.chenjia404.meshchat.service.audio.VoiceRecorder
import com.github.com.chenjia404.meshchat.feature.directchat.voice.ChatInputMode
import com.github.com.chenjia404.meshchat.feature.directchat.voice.DirectChatInputBar
import com.github.com.chenjia404.meshchat.feature.directchat.voice.HoldToTalkButton
import com.github.com.chenjia404.meshchat.feature.directchat.voice.VoiceRecordOverlay
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType
import com.github.com.chenjia404.meshchat.core.util.attachmentSubtitle
import com.github.com.chenjia404.meshchat.core.util.MeshchatHttpErrors
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack

data class GroupChatUiState(
    val groupId: String = "",
    val title: String = "",
    val avatarUrl: String? = null,
    val isSuperGroup: Boolean = false,
    val messages: List<ChatMessageUiModel> = emptyList(),
    /** 进入页面时拉取群/消息失败（如网关 502），不抛异常，供界面提示与重试 */
    val refreshError: String? = null,
)

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val groupRepository: GroupRepository,
    profileRepository: ProfileRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val settingsStore: SettingsStore,
    private val uriFileResolver: UriFileResolver,
    private val fileDownloadService: FileDownloadService,
    private val forwardMessageUseCase: ForwardMessageUseCase,
) : ViewModel() {
    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _refreshError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupChatUiState> = combine(
        groupRepository.observeGroup(groupId),
        groupRepository.observeMessages(groupId),
        profileRepository.myProfile,
        settingsStore.meshChatServerUserIdFlow,
        _refreshError,
    ) { group, messages, profile, meshUserId, refreshErr ->
        val myPeerId = profile?.peerId
        GroupChatUiState(
            groupId = groupId,
            title = group?.title ?: groupId,
            isSuperGroup = group?.isSuperGroup == true,
            avatarUrl = if (group?.isSuperGroup == true) {
                attachmentUrlBuilder.ipfsBlobAbsoluteUrl(group.avatar)
            } else {
                attachmentUrlBuilder.avatarUrl(group?.avatar)
            },
            messages = messages.map { message ->
                val renderType = resolveRenderType(message.msgType, message.mimeType, message.fileName)
                val superGroup = group?.isSuperGroup == true
                val mine = when {
                    superGroup -> meshUserId != null && message.senderPeerId == "meshchat_user_$meshUserId"
                    else -> myPeerId != null && myPeerId == message.senderPeerId
                }
                ChatMessageUiModel(
                    id = message.msgId,
                    title = message.senderLabel?.takeIf { it.isNotBlank() } ?: message.senderPeerId,
                    subtitle = attachmentSubtitle(appContext.resources, renderType, message.mimeType, message.msgType),
                    isMine = mine,
                    renderType = renderType,
                    text = message.plaintext.orEmpty(),
                    fileName = message.fileName,
                    remoteUrl = when {
                        renderType == AttachmentRenderType.TEXT || renderType == AttachmentRenderType.SYSTEM -> null
                        superGroup && !message.fileCid.isNullOrBlank() ->
                            attachmentUrlBuilder.ipfsGatewayUrlWithFilename(message.fileCid, message.fileName)
                        else -> attachmentUrlBuilder.groupFileUrl(groupId, message.msgId)
                    },
                    timestamp = message.createdAt,
                    state = message.state,
                    senderPeerId = message.senderPeerId.takeIf { it.isNotBlank() },
                )
            },
            refreshError = refreshErr,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupChatUiState())

    private val _forwardAttachmentUploading = MutableStateFlow(false)
    val forwardAttachmentUploading: StateFlow<Boolean> = _forwardAttachmentUploading.asStateFlow()

    init {
        viewModelScope.launch { refreshOnOpen() }
    }

    /** 打开群聊时同步远端；网络错误（如 502）只记状态，不崩溃。 */
    private suspend fun refreshOnOpen() {
        _refreshError.value = null
        runCatching {
            groupRepository.refreshGroup(groupId)
            groupRepository.refreshMessages(groupId)
        }.onFailure { e ->
            MeshchatHttpErrors.log("GroupChatViewModel.refreshOnOpen", e)
            _refreshError.value =
                MeshchatHttpErrors.messageForToast(e)
                    ?: appContext.getString(R.string.error_request_failed)
        }
    }

    fun retryRefresh() {
        viewModelScope.launch { refreshOnOpen() }
    }

    fun sendText(text: String) {
        if (text.isEmpty()) return
        viewModelScope.launch { groupRepository.sendText(groupId, text) }
    }

    fun sendAttachment(uri: Uri) {
        viewModelScope.launch {
            val file = uriFileResolver.copyToCache(uri)
            groupRepository.sendFile(groupId, file)
        }
    }

    fun sendAttachment(file: File) {
        viewModelScope.launch { groupRepository.sendFile(groupId, file) }
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
    onOpenSuperGroupIntro: () -> Unit = {},
    onOpenContactProfile: (peerId: String) -> Unit = {},
    viewModel: GroupChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val forwardUploading by viewModel.forwardAttachmentUploading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var messageToForward by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    var input by remember { mutableStateOf("") }
    var syncPeerId by remember { mutableStateOf("") }
    val voiceRecorder = remember { VoiceRecorder(context) }
    var inputMode by remember { mutableStateOf(ChatInputMode.TEXT) }
    var voiceOverlayVisible by remember { mutableStateOf(false) }
    var voiceWillCancel by remember { mutableStateOf(false) }
    var voicePressed by remember { mutableStateOf(false) }
    var voiceElapsedMs by remember { mutableIntStateOf(0) }
    var voiceAmplitude by remember { mutableIntStateOf(0) }
    var voiceRecordingStarted by remember { mutableStateOf(false) }
    var voiceRecordStartElapsed by remember { mutableLongStateOf(0L) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::sendAttachment)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.mic_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

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
        onDispose { ChatVoiceInlinePlayer.stop() }
    }

    LaunchedEffect(voiceOverlayVisible, uiState.isSuperGroup) {
        if (!uiState.isSuperGroup || !voiceOverlayVisible) return@LaunchedEffect
        while (true) {
            delay(100)
            if (!voiceOverlayVisible || !voiceRecorder.isRecording) break
            val elapsed = SystemClock.elapsedRealtime() - voiceRecordStartElapsed
            voiceElapsedMs = elapsed.toInt().coerceIn(0, 60_000)
            voiceAmplitude = voiceRecorder.pollMaxAmplitude()
        }
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        enabled = uiState.isSuperGroup,
                        onClick = onOpenSuperGroupIntro,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarImage(
                    uiState.title.ifBlank { stringResource(R.string.group_short_name) },
                    uiState.avatarUrl,
                    Modifier.size(40.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        uiState.title.ifBlank { stringResource(R.string.group_chat_title) },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (!uiState.isSuperGroup) {
                        Text(
                            stringResource(R.string.group_chat_subtitle_tagline),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        val syncErr = uiState.refreshError
        if (syncErr != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = syncErr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.retryRefresh() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        if (!uiState.isSuperGroup) {
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
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.messages.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_group_messages_title),
                    body = stringResource(R.string.empty_group_messages_body),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
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
                            onSenderProfileClick = { peerId ->
                                if (!peerId.startsWith("meshchat_user_")) {
                                    onOpenContactProfile(peerId)
                                }
                            },
                        )
                    }
                }
            }
            if (uiState.isSuperGroup && voiceOverlayVisible) {
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
        if (uiState.isSuperGroup) {
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
                            val durationMs: Long =
                                (SystemClock.elapsedRealtime() - voiceRecordStartElapsed).coerceAtLeast(0L)
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
                    singleLine = false,
                    maxLines = 6,
                )
                Button(onClick = {
                    viewModel.sendText(input)
                    input = ""
                }) {
                    Text(stringResource(R.string.send))
                }
            }
        }
        }
        ForwardUploadOverlay(visible = forwardUploading)
    }
}
