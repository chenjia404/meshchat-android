package com.github.com.chenjia404.meshchat.service.meshchat

import android.content.Context
import android.util.Base64
import com.github.com.chenjia404.meshchat.core.crypto.MeshProxyIdentityPaths
import com.github.com.chenjia404.meshchat.core.crypto.MeshchatIdentitySigner
import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import com.github.com.chenjia404.meshchat.core.util.MeshchatHttpErrors
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * meshchat-server 登录与 JWT 维护：
 * - `peer_id`：与 Quark 一致，来自 mesh-proxy `GET /api/v1/chat/me`（使用 [MeshChatApi]，与全应用其它请求同一 OkHttp 栈，避免裸客户端导致 403 等差异）。
 * - 签名：`filesDir/data/identity.key` → [MeshchatIdentitySigner]
 * - `POST …/auth/challenge`、`POST …/auth/login`：直连 meshchat-server 主机，使用无拦截器的 [OkHttpClient]（避免 baseUrl 拦截器把请求改写到 mesh-proxy）。
 */
@Singleton
class MeshChatServerSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshChatApi: MeshChatApi,
    private val settingsStore: SettingsStore,
    private val authStore: MeshChatServerAuthStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    /** 与 Quark [MeshchatSuperGroupInteract.httpClient]：仅用于 meshchat-server 本机 auth，不得经过 mesh-proxy 的 baseUrl 拦截器。 */
    private val meshchatServerAuthClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    suspend fun ensureSession(serverBase: HttpUrl) = withContext(ioDispatcher) {
        mutex.withLock {
            restoreFromPrefsIfNeeded()
            if (authStore.matches(serverBase)) return@withLock
            performLogin(serverBase)
        }
    }

    fun currentBearerFor(url: HttpUrl): String? = authStore.bearerTokenFor(url)

    /** OkHttp Authenticator：401 时强制重新 challenge/login（同 host）。 */
    fun forceRefreshBlocking(requestUrl: HttpUrl) {
        runBlocking(ioDispatcher) {
            mutex.withLock {
                settingsStore.clearMeshChatServerJwt()
                authStore.clear()
                val base = requestUrl.newBuilder()
                    .encodedPath("/")
                    .encodedQuery(null)
                    .encodedFragment(null)
                    .build()
                performLogin(base)
            }
        }
    }

    private suspend fun restoreFromPrefsIfNeeded() {
        if (authStore.jwt != null) return
        val (t, b) = settingsStore.getMeshChatServerJwtPair()
        if (!t.isNullOrBlank() && !b.isNullOrBlank()) {
            authStore.setSession(t, b)
        }
    }

    private suspend fun performLogin(serverBase: HttpUrl) {
        try {
            val me = meshChatApi.getMe()
                ?: throw IOException("无法取得本机 profile（mesh-proxy 返回空，请确认已连接且已登录）")
            val peerId = me.peerId.trim().ifBlank {
                throw IOException("无法取得 peer_id（请确认 mesh-proxy 已启动）")
            }
            val seed = MeshchatIdentitySigner.loadEd25519Seed32(MeshProxyIdentityPaths.identityKeyFile(context))
            val baseNormalized = serverBase.newBuilder()
                .encodedPath("/")
                .encodedQuery(null)
                .encodedFragment(null)
                .build()
            val challengeUrl = baseNormalized.resolve("auth/challenge")!!.toString()
            val loginUrl = baseNormalized.resolve("auth/login")!!.toString()

            val chBody = MeshchatServerAuthHttp.postJson(
                meshchatServerAuthClient,
                challengeUrl,
                MeshchatServerAuthHttp.buildChallengeRequestJson(peerId),
                null,
            )
            val (challengeId, challenge) = MeshchatServerAuthHttp.parseChallengeResponse(chBody)
            val sigB64 = MeshchatIdentitySigner.signChallengeBase64(seed, challenge)
            val pub32 = MeshchatIdentitySigner.publicKey32FromSeed(seed)
            val pubB64 = Base64.encodeToString(
                MeshchatIdentitySigner.marshalLibp2pEd25519PublicKeyProtobuf(pub32),
                Base64.NO_WRAP,
            )
            val loginBody = MeshchatServerAuthHttp.postJson(
                meshchatServerAuthClient,
                loginUrl,
                MeshchatServerAuthHttp.buildLoginRequestJson(peerId, challengeId, sigB64, pubB64),
                null,
            )
            val (token, _, userId) = MeshchatServerAuthHttp.parseLoginResponse(loginBody)
            val baseStr = baseNormalized.toString()
            settingsStore.setMeshChatServerJwt(token, baseStr)
            authStore.setSession(token, baseStr)
            if (userId > 0L) {
                settingsStore.setMeshChatServerUserId(userId)
            }
        } catch (e: Exception) {
            MeshchatHttpErrors.log("meshchat_perform_login", e)
            throw e
        }
    }
}
