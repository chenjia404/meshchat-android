package com.github.com.chenjia404.meshchat.service.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 单例内联播放语音 URL（与 Quark [com.quarkpay.wallet.utils.ChatVoicePlayer] 一致）：
 * 点击另一条会切换；再次点击同一条则暂停。
 */
object ChatVoiceInlinePlayer {
    private var player: MediaPlayer? = null
    private var playingUrl: String? = null
    private val stateListeners = CopyOnWriteArrayList<() -> Unit>()

    fun addStateListener(listener: () -> Unit) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener)
        }
    }

    fun removeStateListener(listener: () -> Unit) {
        stateListeners.remove(listener)
    }

    private fun notifyStateChanged() {
        stateListeners.forEach { runCatching { it() } }
    }

    fun isPlayingUrl(url: String): Boolean {
        return url.isNotEmpty() && url == playingUrl && player?.isPlaying == true
    }

    fun toggle(url: String) {
        if (url.isBlank()) {
            notifyStateChanged()
            return
        }
        if (isPlayingUrl(url)) {
            stopInternal()
            notifyStateChanged()
            return
        }
        stopInternal()
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            playingUrl = url
            player = mp
            mp.setOnPreparedListener { m ->
                try {
                    m.start()
                    notifyStateChanged()
                } catch (_: IllegalStateException) {
                    stopInternal()
                    notifyStateChanged()
                }
            }
            mp.setOnCompletionListener {
                stopInternal()
                notifyStateChanged()
            }
            mp.setOnErrorListener { _, _, _ ->
                stopInternal()
                notifyStateChanged()
                true
            }
            mp.setDataSource(url)
            mp.prepareAsync()
        } catch (_: IOException) {
            stopInternal()
            notifyStateChanged()
        }
    }

    fun stop() {
        stopInternal()
        notifyStateChanged()
    }

    private fun stopInternal() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playingUrl = null
    }
}
