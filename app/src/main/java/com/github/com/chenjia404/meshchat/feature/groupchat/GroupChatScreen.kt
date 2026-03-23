package com.github.com.chenjia404.meshchat.feature.groupchat

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType
import com.github.com.chenjia404.meshchat.core.util.copyChatMessageToClipboard
import com.github.com.chenjia404.meshchat.core.util.resolveRenderType
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.domain.usecase.ForwardMessageUseCase
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetPickerDialog
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetRowItem
import com.github.com.chenjia404.meshchat.feature.forward.toForwardDestination
import com.github.com.chenjia404.meshchat.service.download.ApkInstallHelper
import com.github.com.chenjia404.meshchat.service.download.FileDownloadService
import com.github.com.chenjia404.meshchat.service.download.PublicDownloadResult
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
                    subtitle = when (renderType) {
                        AttachmentRenderType.IMAGE -> "图片"
                        AttachmentRenderType.VIDEO -> "视频"
                        AttachmentRenderType.AUDIO -> "语音"
                        AttachmentRenderType.FILE -> message.mimeType ?: "文件"
                        AttachmentRenderType.SYSTEM -> message.msgType
                        AttachmentRenderType.TEXT -> ""
                    },
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
        val fileName = message.fileName ?: "meshchat-attachment"
        viewModelScope.launch {
            runCatching {
                when (val result = fileDownloadService.downloadToPublicDownloads(url, fileName)) {
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
fun GroupChatScreen(
    onBackClick: () -> Unit,
    onOpenImage: (String, String) -> Unit,
    onOpenVideo: (String, String) -> Unit,
    onOpenAudio: (String, String) -> Unit,
    viewModel: GroupChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            AvatarImage(uiState.title.ifBlank { "群" }, uiState.avatarUrl, Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(uiState.title.ifBlank { "群聊" }, style = MaterialTheme.typography.titleLarge)
                Text("meshproxy group chat", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = syncPeerId,
            onValueChange = { syncPeerId = it },
            label = { Text("同步来源 PeerID") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.sync(syncPeerId) },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text("群同步")
        }
        if (uiState.messages.isEmpty()) {
            EmptyState(title = "暂无群消息", body = "发送一条文本或文件消息开始群聊。")
        } else {
            LazyColumn(
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("附件") }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入群消息") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                viewModel.sendText(input)
                input = ""
            }) {
                Text("发送")
            }
        }
    }
}
