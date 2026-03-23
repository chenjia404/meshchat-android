package com.github.com.chenjia404.meshchat.feature.directchat.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.com.chenjia404.meshchat.R

/**
 * 聊天页底部输入条：文字/语音模式切换 + 输入区 + 附件 + 发送（Voice.md 4）
 */
@Composable
fun DirectChatInputBar(
    inputMode: ChatInputMode,
    onToggleInputMode: () -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    onSendText: () -> Unit,
    onPickAttachment: () -> Unit,
    voiceOverlayVisible: Boolean,
    holdToTalkContent: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = onToggleInputMode,
            enabled = !voiceOverlayVisible,
        ) {
            Icon(
                imageVector = if (inputMode == ChatInputMode.TEXT) Icons.Outlined.Mic else Icons.Outlined.Keyboard,
                contentDescription = if (inputMode == ChatInputMode.TEXT) "切换到语音" else "切换到文字",
            )
        }
        when (inputMode) {
            ChatInputMode.TEXT -> {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    enabled = !voiceOverlayVisible,
                    placeholder = { Text(stringResource(R.string.input_message_hint), color = Color(0xFF9C9DA0)) },
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
            }
            ChatInputMode.VOICE -> {
                holdToTalkContent(Modifier.weight(1f))
            }
        }
        IconButton(
            onClick = onPickAttachment,
            enabled = !voiceOverlayVisible,
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = stringResource(R.string.cd_attach))
        }
        Button(
            onClick = onSendText,
            enabled = !voiceOverlayVisible && inputMode == ChatInputMode.TEXT && text.isNotBlank(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B5778),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF3B5778).copy(alpha = 0.38f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.send))
        }
    }
}
