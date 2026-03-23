package com.github.com.chenjia404.meshchat.service.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class VoiceRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun start(): File {
        if (recorder != null) return checkNotNull(outputFile)
        val directory = context.cacheDir
        val file = File(directory, "voice_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioEncodingBitRate(64_000)
        mediaRecorder.setAudioSamplingRate(44_100)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.setMaxDuration(60_000)
        mediaRecorder.prepare()
        mediaRecorder.start()
        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        val currentRecorder = recorder ?: return outputFile
        val file = outputFile
        runCatching { currentRecorder.stop() }
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
