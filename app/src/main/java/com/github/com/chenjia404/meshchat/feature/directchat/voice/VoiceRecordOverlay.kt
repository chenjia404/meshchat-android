package com.github.com.chenjia404.meshchat.feature.directchat.voice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.com.chenjia404.meshchat.R
import kotlin.math.min

/** 录音中浮层（Voice.md 4.3 / 9.2） */
@Composable
fun VoiceRecordOverlay(
    elapsedMs: Int,
    maxMs: Int,
    willCancel: Boolean,
    amplitude: Int,
    modifier: Modifier = Modifier,
) {
    val elapsedSec = elapsedMs / 1000
    val maxSec = maxMs / 1000
    val timeLine = "%02d:%02d / %02d:%02d".format(
        elapsedSec / 60,
        elapsedSec % 60,
        maxSec / 60,
        maxSec % 60,
    )
    val remainingSec = maxSec - elapsedSec
    val warnTime = remainingSec <= 10 && !willCancel

    Card(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (willCancel) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (willCancel) MaterialTheme.colorScheme.error else Color(0xFFE53935),
                            CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (willCancel) "松开手指，取消录音" else "正在录音",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (willCancel) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            VoiceWavePlaceholder(
                elapsedMs = elapsedMs,
                maxMs = maxMs,
                willCancel = willCancel,
                amplitude = amplitude,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = timeLine,
                fontSize = 18.sp,
                fontWeight = if (warnTime) FontWeight.Bold else FontWeight.Medium,
                color = if (warnTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (willCancel) " " else stringResource(R.string.voice_hint_swipe_cancel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VoiceWavePlaceholder(
    elapsedMs: Int,
    maxMs: Int,
    willCancel: Boolean,
    amplitude: Int,
) {
    val norm = (amplitude / 32768f).coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        repeat(12) { i ->
            val base = 0.25f + norm * 0.75f
            val wobble = kotlin.math.sin((i + phase * 12) * 0.8).toFloat().let { (it + 1f) / 2f }
            val hFrac = (base * wobble).coerceIn(0.15f, 1f)
            val alpha = if (willCancel) 0.35f else 1f
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((24 * hFrac).dp)
                    .alpha(alpha)
                    .background(
                        if (willCancel) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
    LinearProgressIndicator(
        progress = { min(1f, elapsedMs / maxMs.toFloat()) },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
    )
}
