package com.github.com.chenjia404.meshchat.core.binary

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Singleton
class BinaryRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var process: Process? = null
    private var logJob: Job? = null

    fun startIfNeeded(): Boolean {
        if (process != null) return true
        return runCatching {
            val started = BinaryManager(context).executeBinary()
            process = started
            logJob?.cancel()
            logJob = scope.launch {
                BufferedReader(InputStreamReader(started.inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        Log.d("BinaryRuntime", line)
                    }
                }
            }
            true
        }.onFailure {
            Log.w("BinaryRuntime", "Failed to start libmeshproxy.so: ${it.message}", it)
        }.getOrDefault(false)
    }

    fun stop() {
        logJob?.cancel()
        logJob = null
        process?.destroy()
        process = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
