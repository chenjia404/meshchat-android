package com.github.com.chenjia404.meshchat.data.remote.ws

import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import com.github.com.chenjia404.meshchat.data.remote.dto.ChatEventDto
import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Singleton
class MeshChatSocket @Inject constructor(
    private val settingsStore: SettingsStore,
    private val gson: Gson,
    @Named("ws") private val webSocketClient: OkHttpClient,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val started = AtomicBoolean(false)

    fun start(onEvent: suspend (ChatEventDto) -> Unit) {
        if (!started.compareAndSet(false, true)) return
        applicationScope.launch(ioDispatcher) {
            settingsStore.baseUrlFlow.collectLatest {
                while (true) {
                    val closeSignal = CompletableDeferred<Unit>()
                    val request = Request.Builder()
                        .url(settingsStore.currentWebSocketUrl())
                        .build()
                    val socket = webSocketClient.newWebSocket(request, object : WebSocketListener() {
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            val event = runCatching { gson.fromJson(text, ChatEventDto::class.java) }.getOrNull()
                            if (event != null) {
                                applicationScope.launch(ioDispatcher) { onEvent(event) }
                            }
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, reason)
                            closeSignal.complete(Unit)
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            closeSignal.complete(Unit)
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            closeSignal.complete(Unit)
                        }
                    })
                    closeSignal.await()
                    socket.cancel()
                    delay(2_000)
                }
            }
        }
    }
}

