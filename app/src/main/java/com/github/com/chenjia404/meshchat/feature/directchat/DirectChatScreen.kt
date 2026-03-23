package com.github.com.chenjia404.meshchat.feature.directchat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
import com.github.com.chenjia404.meshchat.service.download.FileDownloadService
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Mic

data class DirectChatUiState(
    val conversationId: String = "",
    val title: String = "",
    /** 对方 PeerID，用于跳转联系人资料 */
    val peerId: String = "",
    val avatarUrl: String? = null,
    val retentionMinutes: Int = 0,
    val messages: List<ChatMessageUiModel> = emptyList(),
)

private data class RetentionPreset(val label: String, val minutes: Int)

/** 弹窗内可选的保留时长（分钟），含「不自动删除」 */
private fun defaultRetentionPresets(): List<RetentionPreset> = listOf(
    RetentionPreset("不限制", 0),
    RetentionPreset("1 分钟", 1),
    RetentionPreset("5 分钟", 5),
    RetentionPreset("10 分钟", 10),
    RetentionPreset("30 分钟", 30),
    RetentionPreset("1 小时", 60),
    RetentionPreset("6 小时", 360),
    RetentionPreset("12 小时", 720),
    RetentionPreset("1 天", 24 * 60),
    RetentionPreset("7 天", 7 * 24 * 60),
    RetentionPreset("30 天", 30 * 24 * 60),
    RetentionPreset("6000 分钟", 6000),
)

private fun retentionPresetsForDialog(currentMinutes: Int): List<RetentionPreset> {
    val base = defaultRetentionPresets().toMutableList()
    if (currentMinutes > 0 && base.none { it.minutes == currentMinutes }) {
        base.add(0, RetentionPreset("${currentMinutes} 分钟（当前）", currentMinutes))
    }
    return base
}

@HiltViewModel
class DirectChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val directChatRepository: DirectChatRepository,
    contactsRepository: ContactsRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val uriFileResolver: UriFileResolver,
    private val fileDownloadService: FileDownloadService,
    private val forwardMessageUseCase: ForwardMessageUseCase,
) : ViewModel() {
    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

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
                val renderType = resolveRenderType(message.msgType, message.mimeType)
                ChatMessageUiModel(
                    id = message.msgId,
                    title = if (message.direction == MessageDirection.OUTBOUND) "我" else contact?.nickname?.ifBlank { message.senderPeerId }
                        ?: message.senderPeerId,
                    subtitle = when (renderType) {
                        AttachmentRenderType.IMAGE -> "图片"
                        AttachmentRenderType.VIDEO -> "视频"
                        AttachmentRenderType.AUDIO -> "音频"
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
        viewModelScope.launch { fileDownloadService.download(url, fileName) }
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
    val context = LocalContext.current
    var messageToForward by remember { mutableStateOf<ChatMessageUiModel?>(null) }
    val voiceRecorder = remember { VoiceRecorder(context) }
    var input by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要麦克风权限才能录音", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "已授予麦克风权限，请按住麦克风开始录音", Toast.LENGTH_SHORT).show()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::sendAttachment)
    }

    fun startVoiceRecording() {
        if (voiceRecorder.isRecording) return
        runCatching {
            voiceRecorder.start()
            isRecording = true
            recordingSeconds = 0
        }.onFailure {
            Toast.makeText(context, "录音启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceRecording() {
        val file = voiceRecorder.stop()
        isRecording = false
        recordingSeconds = 0
        if (file != null) {
            viewModel.sendAttachment(file)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingSeconds += 1
                if (recordingSeconds >= 60) {
                    stopVoiceRecording()
                    break
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { voiceRecorder.release() }
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
            Column(modifier = Modifier.weight(1f)) {
                // 顶栏主标题：对方昵称；无昵称时退回 peer_id /「单聊」
                Text(
                    text = uiState.title.ifBlank { uiState.peerId.ifBlank { "单聊" } },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            color = Color(0xFFF2F2F2),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (uiState.retentionMinutes <= 0) {
                        "保留时长：不限制"
                    } else {
                        "保留时长：${uiState.retentionMinutes} 分钟"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { showRetentionDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("修改", color = Color(0xFF3B5778), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (showRetentionDialog) {
            val presets = remember(uiState.retentionMinutes) {
                retentionPresetsForDialog(uiState.retentionMinutes)
            }
            AlertDialog(
                onDismissRequest = { showRetentionDialog = false },
                title = { Text("选择保留时长") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        presets.forEach { preset ->
                            val selected = preset.minutes == uiState.retentionMinutes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateRetention(preset.minutes)
                                        showRetentionDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRetentionDialog = false }) {
                        Text("取消")
                    }
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isRecording) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "录音中 ${formatRecordingTime(recordingSeconds)} · 点击麦克风结束发送",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { recordingSeconds / 60f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (uiState.messages.isEmpty()) {
            EmptyState(title = "暂无消息", body = "发送一条文本或文件消息开始聊天。")
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(uiState.messages, key = { it.id }) { item ->
                    ChatMessageBubble(
                        message = item,
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@detectTapGestures
                        }
                        if (isRecording) {
                            return@detectTapGestures
                        }
                        startVoiceRecording()
                        tryAwaitRelease()
                        if (isRecording) {
                            stopVoiceRecording()
                        }
                    })
                },
                onClick = { },
            ) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = "语音",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Outlined.AttachFile, contentDescription = "附件")
            }
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                enabled = !isRecording,
                placeholder = { Text("输入消息", color = Color(0xFF9C9DA0)) },
                singleLine = false,
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFDBDBDB),
                    unfocusedIndicatorColor = Color(0xFFDBDBDB),
                    disabledIndicatorColor = Color(0xFFE2E2E2),
                    cursorColor = Color(0xFF3B5778),
                    focusedTextColor = Color(0xFF333333),
                    unfocusedTextColor = Color(0xFF333333),
                ),
            )
            Button(
                onClick = {
                    viewModel.sendText(input)
                    input = ""
                },
                enabled = !isRecording && input.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B5778),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF3B5778).copy(alpha = 0.38f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text("发送")
            }
        }
    }
}

private fun formatRecordingTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safeSeconds / 60, safeSeconds % 60)
}
