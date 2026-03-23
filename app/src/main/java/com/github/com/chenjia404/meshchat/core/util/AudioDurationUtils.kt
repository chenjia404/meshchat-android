package com.github.com.chenjia404.meshchat.core.util

import android.media.MediaMetadataRetriever
import java.util.HashMap
import kotlin.math.roundToInt

/**
 * 从远端语音 URL 解析时长（秒），与 Quark [ChatVoiceDuration.loadDurationSecondsAsync] 行为一致。
 */
fun loadAudioDurationSecondsFromUrl(url: String): Int {
    if (url.isBlank()) return 0
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(url, HashMap())
        val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val ms = d?.toLongOrNull() ?: 0L
        (ms / 1000.0).roundToInt().coerceIn(0, 3600)
    } catch (_: Exception) {
        0
    } finally {
        runCatching { retriever.release() }
    }
}
