package com.github.com.chenjia404.meshchat.service.meshchat

import android.util.Log
import com.github.com.chenjia404.meshchat.core.util.MeshchatHttpErrors
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * 与 QuarkPayAndroid [MeshchatSuperGroupInteract] / [CommonInteract.httpGetChatMeProfileSync] 的 HTTP 形态一致：
 * JSON POST/GET、可选 Bearer；非 2xx 抛 [IOException]。
 */
internal object MeshchatServerAuthHttp {

    private const val TAG = "MeshchatHTTP"

    fun trimTrailingSlash(s: String): String {
        if (s.length <= 1) return s
        var t = s
        while (t.endsWith("/") && t.length > 1) {
            t = t.substring(0, t.length - 1)
        }
        return t
    }

    /**
     * 同步 GET `{meshProxyBase}/api/v1/chat/me`，解析 `peer_id`（与 Quark [CommonInteract.httpGetChatMeProfileSync] 路径一致）。
     */
    @Throws(IOException::class)
    fun getChatMePeerIdSync(client: OkHttpClient, meshProxyBase: HttpUrl): String {
        val url = meshProxyBase.newBuilder()
            .addPathSegment("api")
            .addPathSegment("v1")
            .addPathSegment("chat")
            .addPathSegment("me")
            .build()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                val msg = buildHttpErrorMessage(response.code, errBody)
                Log.w(TAG, "getChatMePeerIdSync HTTP ${response.code} url=$url body=${errBody.take(2000)}")
                throw IOException(msg)
            }
            val body = response.body?.string().orEmpty().ifBlank { "{}" }
            val obj = JsonParser.parseString(body).asJsonObject
            val pid = if (obj.has("peer_id") && obj.get("peer_id").isJsonPrimitive) {
                obj.get("peer_id").asString
            } else {
                ""
            }
            if (pid.isBlank()) {
                throw IOException("无法取得本机 peer_id（请确认 mesh-proxy 已启动）")
            }
            return pid.trim()
        }
    }

    @Throws(IOException::class)
    fun postJson(client: OkHttpClient, url: String, json: String, bearer: String?): String {
        val mt = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mt)
        val builder = Request.Builder().url(url).post(body)
        if (!bearer.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $bearer")
        }
        client.newCall(builder.build()).execute().use { response ->
            val resp = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = buildHttpErrorMessage(response.code, resp)
                Log.w(TAG, "postJson HTTP ${response.code} url=$url body=${resp.take(2000)}")
                throw IOException(msg)
            }
            return resp
        }
    }

    /** 与 Logcat / Toast 一致：优先解析 `error.message`（meshchat-server JSON）。 */
    fun buildHttpErrorMessage(code: Int, body: String): String {
        val b = body.trim()
        val friendly = MeshchatHttpErrors.formatServerErrorBody(b)
        return when {
            friendly != null -> "HTTP $code $friendly"
            b.isNotEmpty() -> "HTTP $code $b"
            else -> "HTTP $code"
        }
    }

    /**
     * 解析 login 响应：优先 [token]，否则 [access_token]；[expires_in] 秒；[user.id]。
     */
    fun parseLoginResponse(loginBody: String): Triple<String, Long, Long> {
        val lo = JsonParser.parseString(loginBody).asJsonObject
        var token: String? = null
        if (lo.has("token") && lo.get("token").isJsonPrimitive) {
            token = lo.get("token").asString
        }
        if (token.isNullOrBlank() && lo.has("access_token") && lo.get("access_token").isJsonPrimitive) {
            token = lo.get("access_token").asString
        }
        if (token.isNullOrBlank()) {
            throw IOException("login 响应缺少 token")
        }
        var expMs = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        if (lo.has("expires_in") && lo.get("expires_in").isJsonPrimitive) {
            runCatching {
                val sec = lo.get("expires_in").asLong
                expMs = System.currentTimeMillis() + sec * 1000L
            }
        }
        var userId = 0L
        if (lo.has("user") && lo.get("user").isJsonObject) {
            val u = lo.getAsJsonObject("user")
            if (u.has("id") && u.get("id").isJsonPrimitive) {
                runCatching { userId = u.get("id").asLong }
            }
        }
        return Triple(token.trim(), expMs, userId)
    }

    fun parseChallengeResponse(chBody: String): Pair<String, String> {
        val ch = JsonParser.parseString(chBody).asJsonObject
        val challengeId = if (ch.has("challenge_id") && ch.get("challenge_id").isJsonPrimitive) {
            ch.get("challenge_id").asString
        } else {
            null
        }
        val challenge = if (ch.has("challenge") && ch.get("challenge").isJsonPrimitive) {
            ch.get("challenge").asString
        } else {
            null
        }
        if (challengeId.isNullOrBlank() || challenge.isNullOrBlank()) {
            throw IOException("auth challenge 响应无效")
        }
        return Pair(challengeId, challenge)
    }

    fun buildLoginRequestJson(peerId: String, challengeId: String, sigB64: String, pubB64: String): String {
        val loginReq = JsonObject()
        loginReq.addProperty("peer_id", peerId)
        loginReq.addProperty("challenge_id", challengeId)
        loginReq.addProperty("signature", sigB64)
        loginReq.addProperty("public_key", pubB64)
        return loginReq.toString()
    }

    fun buildChallengeRequestJson(peerId: String): String {
        val chReq = JsonObject()
        chReq.addProperty("peer_id", peerId)
        return chReq.toString()
    }
}
