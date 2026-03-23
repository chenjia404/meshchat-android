package com.github.com.chenjia404.meshchat.feature.directchat.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.com.chenjia404.meshchat.R

private val CancelThresholdDp = 72.dp

/**
 * 语音模式「按住说话」：长按后触发录音，上滑取消（Voice.md 5.3 / 8）
 *
 * 使用 [detectDragGesturesAfterLongPress]：`onDragStart` 在长按时触发，用于开始录音。
 */
@Composable
fun HoldToTalkButton(
    enabled: Boolean,
    pressedVisual: Boolean,
    willCancelVisual: Boolean,
    onLongPressStarted: () -> Unit,
    onCancelZoneChanged: (Boolean) -> Unit,
    onGestureEnd: (cancelledBySwipe: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val cancelPx = with(density) { CancelThresholdDp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(cancelPx) {
                        var anchorY = 0f
                        var inCancelZone = false
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                anchorY = offset.y
                                inCancelZone = false
                                onLongPressStarted()
                            },
                            onDrag = { change, _ ->
                                val dy = anchorY - change.position.y
                                inCancelZone = dy > cancelPx
                                onCancelZoneChanged(inCancelZone)
                            },
                            onDragEnd = {
                                onGestureEnd(inCancelZone)
                            },
                            onDragCancel = {
                                onGestureEnd(true)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .background(
                color = when {
                    !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    willCancelVisual -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    pressedVisual -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                shape = RoundedCornerShape(8.dp),
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.hold_to_talk),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}
