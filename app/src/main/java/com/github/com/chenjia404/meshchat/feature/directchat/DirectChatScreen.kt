package com.github.com.chenjia404.meshchat.feature.directchat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.os.VibrationEffect
import android.widget.Toast
import android.os.Vibrator
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.service.download.ApkInstallHelper
import com.github.com.chenjia404.meshchat.service.download.PublicDownloadResult
import com.github.com.chenjia404.meshchat.service.audio.ChatVoiceInlinePlayer
import com.github.com.chenjia404.meshchat.service.audio.VoiceRecorder
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageBubble
import com.github.com.chenjia404.meshchat.core.ui.ChatMessageUiModel
import com.github.com.chenjia404.meshchat.core.ui.EmptyState
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType
import com.github.com.chenjia404.meshchat.core.util.resolveRenderType
import com.github.com.chenjia404.meshchat.domain.model.MessageDirection
import com.github.com.chenjia404.meshchat.domain.repository.ContactsRepository
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.usecase.ForwardMessageUseCase
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetPickerDialog
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetRowItem
import com.github.com.chenjia404.meshchat.feature.forward.toForwardDestination
import com.github.com.chenjia404.meshchat.core.util.copyChatMessageToClipboard
import com.github.com.chenjia404.meshchat.feature.directchat.voice.ChatInputMode
import com.github.com.chenjia404.meshchat.feature.directchat.voice.DirectChatInputBar
import com.github.com.chenjia404.meshchat.feature.directchat.voice.HoldToTalkButton
import com.github.com.chenjia404.meshchat.feature.directchat.voice.VoiceRecordOverlay
import com.github.com.chenjia404.meshchat.core.util.formatRetentionDisplayShort
import com.github.com.chenjia404.meshchat.service.download.FileDownloadService
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown

data class DirectChatUiState(
    val conversationId: String = "",
    val title: String = "",
    /** 对方 PeerID，用于跳转联系人资料 */
    val peerId: String = "",
    val avatarUrl: String? = null,
    val retentionMinutes: Int = 0,
    val messages: List<ChatMessageUiModel> = emptyList(),
)

@HiltViewModel
class DirectChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val directChatRepository: DirectChatRepository,
    contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val uriFileResolver: UriFileResolver,
    private val fileDownloadService: FileDownloadService,
    private val forwardMessageUseCase: ForwardMessageUseCase,
) : ViewModel() {
    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    /** 进入会话时导航传入的未读条数（会话列表带入）；联系人入口为 0 */
    val entryUnreadCount: Int = savedStateHandle.get<Int>("entryUnread") ?: 0

    val uiState: StateFlow<DirectChatUiState> = combine(
        directChatRepository.observeConversation(conversationId),
        directChatRepository.observeMessages(conversationId),
        contactsRepository.contacts,
    ) { conversation, messages, contacts ->
        val contact = contacts.firstOrNull { it.peerId == conversation?.peerId }
        DirectChatUiState(
            conversationId = conversationId,
            title = contact?.nickname?.ifBlank { conversation?.peerId.orEmpty() } ?: conversation?.peerId.orEmpty(),
            peerId = conversation?.peerId.orEmpty(),
            avatarUrl = attachmentUrlBuilder.avatarUrl(contact?.avatar),
            retentionMinutes = conversation?.retentionMinutes ?: contact?.retentionMinutes ?: 0,
            messages = messages.map { message ->
                val renderType = resolveRenderType(message.msgType, message.mimeType, message.fileName)
                ChatMessageUiModel(
                    id = message.msgId,
                    title = if (message.direction == MessageDirection.OUTBOUND) "我" else contact?.nickname?.ifBlank { message.senderPeerId }
                        ?: message.senderPeerId,
                    subtitle = when (renderType) {
                        AttachmentRenderType.IMAGE -> "图片"
                        AttachmentRenderType.VIDEO -> "视频"
                        AttachmentRenderType.AUDIO -> "语音"
                        AttachmentRenderType.FILE -> message.mimeType ?: "文件"
                        AttachmentRenderType.SYSTEM -> message.msgType
                        AttachmentRenderType.TEXT -> ""
                    },
                    isMine = message.direction == MessageDirection.OUTBOUND,
                    renderType = renderType,
                    text = message.plaintext.orEmpty(),
                    fileName = message.fileName,
                    remoteUrl = if (renderType == AttachmentRenderType.TEXT || renderType == AttachmentRenderType.SYSTEM) {
                        null
                    } else {
                        attachmentUrlBuilder.directFileUrl(message.conversationId, message.msgId)
                    },
                    timestamp = message.createdAt,
                    state = message.state,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectChatUiState())

    init {
        viewModelScope.launch {
            directChatRepository.refreshMessages(conversationId)
            directChatRepository.markRead(conversationId)
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { directChatRepository.sendText(conversationId, text) }
    }

    fun sendAttachment(uri: Uri) {
        viewModelScope.launch {
            val file = uriFileResolver.copyToCache(uri)
            directChatRepository.sendFile(conversationId, file)
        }
    }

    fun sendAttachment(file: File) {
        viewModelScope.launch { directChatRepository.sendFile(conversationId, file) }
    }

    fun revoke(msgId: String) {
        viewModelScope.launch { directChatRepository.revokeMessage(conversationId, msgId) }
    }

    fun updateRetention(retentionMinutes: Int) {
        viewModelScope.launch { directChatRepository.updateRetention(conversationId, retentionMinutes) }
    }

    fun download(message: ChatMessageUiModel) {
        val url = message.remoteUrl ?: return
        val fileName = message.fileName ?: "meshchat-attachment"
        viewModelScope.launch {
            runCatching {
                when (
                    val result = fileDownloadService.downloadToPublicDownloads(url, fileName)
                ) {
                    is PublicDownloadResult.Success -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                "已保存到「下载/MeshChat」",
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
                                "已加入系统下载，请在通知栏或「下载」中查看",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        e.message ?: "下载失败",
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
            onDone(false, "未选择会话")
            return
        }
        viewModelScope.launch {
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
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.message ?: "转发失败")
            }
        }
    }
}

@Composable
fun DirectChatScreen(
    onBackClick: () -> Unit,
    /** 点击顶栏头像，进入对方联系人资料（含 peer_id） */
    onOpenContactProfile: (peerId: String) -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    onOpenAudio: (String, String) -> Unit,
    viewModel: DirectChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entryUnreadCount = viewModel.entryUnreadCount
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var messageToForward by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    val voiceRecorder = remember { VoiceRecorder(context) }

    var input by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(ChatInputMode.TEXT) }
    var voiceOverlayVisible by remember { mutableStateOf(false) }
    var voiceWillCancel by remember { mutableStateOf(false) }
    var voicePressed by remember { mutableStateOf(false) }
    var voiceElapsedMs by remember { mutableIntStateOf(0) }
    var voiceAmplitude by remember { mutableIntStateOf(0) }
    /** 长按已成功开始录音 */
    var voiceRecordingStarted by remember { mutableStateOf(false) }
    var voiceRecordStartElapsed by remember { mutableLongStateOf(0L) }

    var showRetentionDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "请开启麦克风权限", Toast.LENGTH_SHORT).show()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::sendAttachment)
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

    fun vibrateShort() {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
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
            interruptVoiceSession("录音失败，请重试")
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
                    interruptVoiceSession("录音已中断")
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

    /** 与 Quark 一致：未读>0 时滚到首条未读（假定未读为时间序末尾 N 条）；否则滚到最新一条 */
    var didInitialUnreadScroll by remember(uiState.conversationId) { mutableStateOf(false) }
    LaunchedEffect(uiState.conversationId, uiState.messages.size, entryUnreadCount) {
        if (didInitialUnreadScroll || uiState.messages.isEmpty()) return@LaunchedEffect
        val n = uiState.messages.size
        val unread = entryUnreadCount
        val index = if (unread <= 0) {
            n - 1
        } else {
            val k = minOf(unread, n)
            maxOf(0, n - k)
        }
        listState.scrollToItem(index)
        didInitialUnreadScroll = true
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

    if (messageToForward != null) {
        ForwardTargetPickerDialog(
            excludeDirectConversationId = uiState.conversationId,
            excludeGroupId = null,
            onDismiss = { messageToForward = null },
            onConfirm = { rows ->
                val msg = messageToForward
                messageToForward = null
                if (msg != null) {
                    viewModel.forwardMessage(msg, rows) { ok, err ->
                        if (ok) {
                            Toast.makeText(context, "已转发", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, err ?: "转发失败", Toast.LENGTH_SHORT).show()
                        }
                    }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
            AvatarImage(
                title = uiState.title.ifBlank { "?" },
                avatarUrl = uiState.avatarUrl,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        enabled = uiState.peerId.isNotBlank(),
                        onClick = { onOpenContactProfile(uiState.peerId) },
                    ),
            )
            // 顶栏主标题：对方昵称；无昵称时退回 peer_id /「单聊」；保留时长靠右
            Text(
                text = uiState.title.ifBlank { uiState.peerId.ifBlank { "单聊" } },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "保留：" + formatRetentionDisplayShort(uiState.retentionMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF3B5778),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { showRetentionDialog = true },
            )
        }
        RetentionEditDialog(
            visible = showRetentionDialog,
            currentMinutes = uiState.retentionMinutes,
            onDismiss = { showRetentionDialog = false },
            onConfirm = { minutes ->
                viewModel.updateRetention(minutes)
                showRetentionDialog = false
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (uiState.messages.isEmpty()) {
                EmptyState(title = "暂无消息", body = "发送一条文本或文件消息开始聊天。")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.messages, key = { it.id }) { item ->
                        ChatMessageBubble(
                            message = item,
                            showSenderName = false,
                            onOpenAttachment = { message ->
                                when (message.renderType) {
                                    AttachmentRenderType.IMAGE -> message.remoteUrl?.let { onOpenImage(it, message.fileName ?: "图片") }
                                    AttachmentRenderType.VIDEO -> message.remoteUrl?.let { onOpenVideo(it, message.fileName ?: "视频") }
                                    AttachmentRenderType.AUDIO -> message.remoteUrl?.let { onOpenAudio(it, message.fileName ?: "音频") }
                                    AttachmentRenderType.FILE -> viewModel.download(message)
                                    else -> Unit
                                }
                            },
                            onCopy = { m ->
                                copyChatMessageToClipboard(context, m)
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            },
                            onForward = { messageToForward = it },
                            onRevoke = { viewModel.revoke(it.id) },
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
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "最新")
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
                            Toast.makeText(context, "请开启麦克风权限", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "录音失败，请重试", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "已取消录音", Toast.LENGTH_SHORT).show()
                            return@HoldToTalkButton
                        }
                        val file = voiceRecorder.stop()
                        when {
                            durationMs < 1000L -> {
                                file?.delete()
                                Toast.makeText(context, "录音时间太短", Toast.LENGTH_SHORT).show()
                            }
                            file != null -> viewModel.sendAttachment(file)
                            else -> Toast.makeText(context, "录音失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            },
        )
    }
}
