package com.github.com.chenjia404.meshchat.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import com.github.com.chenjia404.meshchat.service.audio.ChatVoiceInlinePlayer
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.com.chenjia404.meshchat.core.util.formatChatMessageLineTime
import com.github.com.chenjia404.meshchat.core.util.formatMessageStateLabel
import com.github.com.chenjia404.meshchat.core.util.isFailedMessageState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType

/** 设计稿：己方深蓝气泡、对方浅灰气泡 */
private val ChatBubbleOutgoing = Color(0xFF3B5778)
private val ChatBubbleIncoming = Color(0xFFF2F2F2)
private val ChatMetaGray = Color(0xFF999999)
private val ChatBubbleOnOutgoing = Color.White
private val ChatBubbleOnIncoming = Color(0xFF333333)

data class ChatMessageUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val isMine: Boolean,
    val renderType: AttachmentRenderType,
    val text: String,
    val fileName: String?,
    val remoteUrl: String?,
    val timestamp: String,
    val state: String,
)

@Composable
fun AvatarImage(
    title: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = title,
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** 气泡上方一行：对方左昵称右时间；己方右对齐「时间 + 发送状态」；系统消息居中时间（己方同样可带状态） */
@Composable
private fun ChatMessageMetaRow(
    message: ChatMessageUiModel,
    showSenderName: Boolean,
) {
    val timeText = formatChatMessageLineTime(message.timestamp)
    val timeStyle = MaterialTheme.typography.labelSmall.copy(
        color = ChatMetaGray,
        fontSize = 11.sp,
    )
    val stateLabel = if (message.isMine) formatMessageStateLabel(message.state) else ""
    when (message.renderType) {
        AttachmentRenderType.SYSTEM -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = when {
                    message.isMine -> Arrangement.End
                    else -> Arrangement.Center
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = timeText, style = timeStyle)
                if (message.isMine && stateLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stateLabel,
                        color = if (isFailedMessageState(message.state)) Color(0xFFDE0000) else ChatMetaGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = when {
                    message.isMine -> Arrangement.End
                    showSenderName -> Arrangement.SpaceBetween
                    else -> Arrangement.Start
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!message.isMine && showSenderName) {
                    Text(
                        text = message.title,
                        style = timeStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                }
                if (message.isMine) {
                    Text(
                        text = timeText,
                        style = timeStyle,
                    )
                    if (stateLabel.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stateLabel,
                            color = if (isFailedMessageState(message.state)) Color(0xFFDE0000) else ChatMetaGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = timeText,
                        style = timeStyle,
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageUiModel,
    /** 为 true 时在对方消息上方显示发送者昵称（群聊）；单聊应传 false */
    showSenderName: Boolean = true,
    onOpenAttachment: (ChatMessageUiModel) -> Unit,
    onCopy: (ChatMessageUiModel) -> Unit,
    onForward: (ChatMessageUiModel) -> Unit,
    onRevoke: (ChatMessageUiModel) -> Unit,
) {
    val bubbleBg = if (message.isMine) ChatBubbleOutgoing else ChatBubbleIncoming
    val onBubble = if (message.isMine) ChatBubbleOnOutgoing else ChatBubbleOnIncoming
    val onBubbleMuted = if (message.isMine) onBubble.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurfaceVariant
    var menuExpanded by remember(message.id) { mutableStateOf(false) }

    // 使用 maxWidth 限制气泡最宽 82%；用 Box + CenterEnd/CenterStart 对齐，比 Row+End 更稳定
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val maxBubbleWidth = maxWidth * 0.82f
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        // 统一处理单击与长按：避免 Surface/图片上的 clickable 抢走长按，导致图片区无法弹出菜单
                        .pointerInput(message.id) {
                            detectTapGestures(
                                onTap = {
                                    val url = message.remoteUrl
                                    when {
                                        message.renderType == AttachmentRenderType.AUDIO && url != null ->
                                            ChatVoiceInlinePlayer.toggle(url)
                                        url != null -> onOpenAttachment(message)
                                    }
                                },
                                onLongPress = { menuExpanded = true },
                            )
                        },
                    horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
                ) {
            ChatMessageMetaRow(message = message, showSenderName = showSenderName)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bubbleBg,
                // 短消息随内容变窄；长消息在 max 内换行，配合 Column 的 End/Start 对齐实现靠右/靠左
                modifier = Modifier.widthIn(max = maxBubbleWidth),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    val textMax = maxBubbleWidth - 24.dp
                    when (message.renderType) {
                        AttachmentRenderType.TEXT -> Text(
                            text = message.text,
                            color = onBubble,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.widthIn(max = textMax),
                        )
                        AttachmentRenderType.SYSTEM -> Text(
                            text = message.text.ifBlank { message.subtitle },
                            color = onBubble,
                            modifier = Modifier.widthIn(max = textMax),
                        )
                        AttachmentRenderType.AUDIO -> {
                            ChatVoiceMessageBar(
                                message = message,
                                onBubble = onBubble,
                                onBubbleMuted = onBubbleMuted,
                            )
                        }
                        AttachmentRenderType.IMAGE,
                        AttachmentRenderType.VIDEO,
                        AttachmentRenderType.FILE,
                        -> {
                            if (message.renderType == AttachmentRenderType.IMAGE || message.renderType == AttachmentRenderType.VIDEO) {
                                ChatMessageMediaPreview(message, message.isMine)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = message.fileName ?: "附件",
                                fontWeight = FontWeight.SemiBold,
                                color = onBubble,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = message.subtitle, color = onBubbleMuted)
                        }
                    }
                }
            }
            }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("复制") },
                        onClick = {
                            menuExpanded = false
                            onCopy(message)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("转发") },
                        onClick = {
                            menuExpanded = false
                            onForward(message)
                        },
                    )
                    if (message.isMine) {
                        DropdownMenuItem(
                            text = { Text("撤回") },
                            onClick = {
                                menuExpanded = false
                                onRevoke(message)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageMediaPreview(message: ChatMessageUiModel, isMine: Boolean) {
    val remoteUrl = message.remoteUrl ?: return
    val context = LocalContext.current
    val playTint = if (isMine) Color.White else Color.White.copy(alpha = 0.92f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (message.renderType) {
            AttachmentRenderType.IMAGE -> {
                AsyncImage(
                    model = remoteUrl,
                    contentDescription = message.fileName ?: "图片预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            AttachmentRenderType.VIDEO -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(remoteUrl)
                        .videoFrameMillis(1000)
                        .build(),
                    contentDescription = message.fileName ?: "视频预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = playTint,
                    )
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
