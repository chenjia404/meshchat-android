package com.github.com.chenjia404.meshchat.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.com.chenjia404.meshchat.core.util.loadAudioDurationSecondsFromUrl
import com.github.com.chenjia404.meshchat.service.audio.ChatVoiceInlinePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Quark 风格语音条：播放/暂停图标 + 波形占位条 + 时长（秒）。
 * 点击由外层气泡 [pointerInput] 统一触发 [ChatVoiceInlinePlayer.toggle]。
 */
@Composable
fun ChatVoiceMessageBar(
    message: ChatMessageUiModel,
    onBubble: Color,
    onBubbleMuted: Color,
    modifier: Modifier = Modifier,
) {
    val url = message.remoteUrl ?: return
    var durationSec by remember(url) { mutableIntStateOf(0) }
    LaunchedEffect(url) {
        durationSec = withContext(Dispatchers.IO) { loadAudioDurationSecondsFromUrl(url) }
    }
    var playing by remember(url) { mutableStateOf(false) }
    DisposableEffect(url) {
        val sync = {
            playing = ChatVoiceInlinePlayer.isPlayingUrl(url)
        }
        ChatVoiceInlinePlayer.addStateListener(sync)
        sync()
        onDispose {
            ChatVoiceInlinePlayer.removeStateListener(sync)
        }
    }

    val dynamicMinWidth = 120.dp + (min(durationSec, 60) * 2).dp

    // 与 Quark item_chat_message_voice：整体 LTR，避免 RTL 下图标顺序错乱
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Row(
            modifier = modifier
                .widthIn(min = dynamicMinWidth)
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放",
                tint = onBubble,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 波形占位（Quark 48dp×4dp）
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 4.dp)
                    .background(
                        color = onBubbleMuted.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatVoiceDurationLabel(durationSec),
                color = onBubble,
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatVoiceDurationLabel(seconds: Int): String {
    val s = seconds.coerceIn(0, 3600)
    return "${s}″"
}
