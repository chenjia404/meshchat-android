package com.github.com.chenjia404.meshchat.core.util

import android.util.Log
import com.google.gson.JsonParser
import retrofit2.HttpException

/**
 * meshchat-server / mesh-proxy 直连请求的错误日志与用户可读文案。
 * 服务端错误体形如：`{"error":{"code":"join_not_allowed","message":"..."}}`
 */
object MeshchatHttpErrors {
    private const val TAG = "MeshchatHTTP"

    /** 打印到 Logcat（含 HTTP 状态码与响应体摘要）。 */
    fun log(where: String, t: Throwable) {
        when (t) {
            is HttpException -> {
                val code = t.code()
                val url = t.response()?.raw()?.request?.url?.toString().orEmpty()
                val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()?.trim().orEmpty()
                Log.w(TAG, "[$where] HTTP $code url=$url body=${body.take(2000)}", t)
            }
            else -> Log.w(TAG, "[$where] ${t.message}", t)
        }
    }

    /**
     * 解析 meshchat-server 标准错误 JSON，返回适合 Toast 的短句（优先 [error.message]，可带 [error.code]）。
     */
    fun formatServerErrorBody(body: String): String? {
        val t = body.trim()
        if (t.isEmpty() || !t.startsWith("{")) return null
        return runCatching {
            val root = JsonParser.parseString(t).asJsonObject
            val err = root.getAsJsonObject("error") ?: return@runCatching null
            val message = err.get("message")?.asString?.trim().orEmpty()
            val code = err.get("code")?.asString?.trim().orEmpty()
            when {
                message.isNotEmpty() && code.isNotEmpty() -> "$message ($code)"
                message.isNotEmpty() -> message
                code.isNotEmpty() -> code
                else -> null
            }
        }.getOrNull()
    }

    /** 从异常中提取 Toast 文案（含 [HttpException] 与带 JSON 的 [Throwable.message]）。 */
    fun messageForToast(t: Throwable): String? {
        when (t) {
            is HttpException -> {
                val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()?.trim().orEmpty()
                formatServerErrorBody(body)?.let { return it }
                val code = t.code()
                return if (body.isNotEmpty()) "HTTP $code $body" else "HTTP $code"
            }
            else -> {
                val msg = t.message ?: return null
                val idx = msg.indexOf('{')
                if (idx >= 0) {
                    formatServerErrorBody(msg.substring(idx))?.let { return it }
                }
                return msg.takeIf { it.isNotBlank() }
            }
        }
    }

}
