package com.github.com.chenjia404.meshchat.service.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.IOException

/**
 * 语音录制；与私聊 [com.github.com.chenjia404.meshchat.feature.directchat.DirectChatScreen] 语音输入配合。
 * 最长 60s（[MediaRecorder.setMaxDuration]），到时长通过 [setOnMaxDurationReachedListener] 回调。
 *
 * **编码策略**
 * - Android 10（API 29）及以上：优先使用 **Ogg 容器 + Opus**（[MediaRecorder.OutputFormat.OGG] +
 *   [MediaRecorder.AudioEncoder.OPUS]），与常见 Web MIME **`audio/ogg; codecs=opus`** 一致。
 * - 更低系统版本或 Opus 初始化失败时：回退为 **M4A + AAC**（与旧版行为一致）。
 */
class VoiceRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var onMaxDurationReached: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null

    val isRecording: Boolean
        get() = recorder != null

    fun setOnMaxDurationReachedListener(listener: (() -> Unit)?) {
        onMaxDurationReached = listener
    }

    fun setOnErrorListener(listener: (() -> Unit)?) {
        onError = listener
    }

    /** 当前片段最大振幅，用于波形展示；未录音时为 0 */
    fun pollMaxAmplitude(): Int {
        val r = recorder ?: return 0
        return try {
            r.maxAmplitude
        } catch (_: Throwable) {
            0
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun start(): File {
        if (recorder != null) return checkNotNull(outputFile)
        val directory = context.cacheDir

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val opusFile = File(directory, "voice_${System.currentTimeMillis()}.ogg")
            val mrOpus = buildRecorder(opusFile, useOpus = true)
            try {
                mrOpus.prepare()
                mrOpus.start()
                recorder = mrOpus
                outputFile = opusFile
                return opusFile
            } catch (_: Throwable) {
                // 部分机型/ROM 对 OGG+Opus 支持不完整，回退 AAC
                runCatching { mrOpus.release() }
                opusFile.delete()
            }
        }

        val aacFile = File(directory, "voice_${System.currentTimeMillis()}.m4a")
        val mr = buildRecorder(aacFile, useOpus = false)
        mr.prepare()
        mr.start()
        recorder = mr
        outputFile = aacFile
        return aacFile
    }

    private fun buildRecorder(file: File, useOpus: Boolean): MediaRecorder {
        val mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        if (useOpus) {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            mediaRecorder.setAudioEncodingBitRate(32_000)
            mediaRecorder.setAudioSamplingRate(48_000)
        } else {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(64_000)
            mediaRecorder.setAudioSamplingRate(44_100)
        }
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.setMaxDuration(60_000)
        mediaRecorder.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                mainHandler.post {
                    onMaxDurationReached?.invoke()
                }
            }
        }
        mediaRecorder.setOnErrorListener { _, _, _ ->
            mainHandler.post {
                onError?.invoke()
            }
        }
        return mediaRecorder
    }

    fun stop(): File? {
        val file = outputFile
        runCatching { recorder?.stop() }
        release()
        return file
    }

    fun cancel() {
        val file = outputFile
        runCatching {
            recorder?.stop()
        }
        file?.delete()
        release()
    }

    fun release() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile = null
    }
}
