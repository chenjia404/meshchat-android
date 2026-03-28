package com.github.com.chenjia404.meshchat.service.meshchat

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * meshchat-server JWT 内存缓存（与 [com.github.com.chenjia404.meshchat.core.datastore.SettingsStore] 持久化同步）。
 */
@Singleton
class MeshChatServerAuthStore @Inject constructor() {
    @Volatile
    var jwt: String? = null

    @Volatile
    var jwtApiBase: String? = null

    fun setSession(token: String, apiBaseWithSlash: String) {
        jwt = token
        jwtApiBase = apiBaseWithSlash.trimEnd('/') + "/"
    }

    fun clear() {
        jwt = null
        jwtApiBase = null
    }

    fun matches(requestUrl: HttpUrl): Boolean {
        val t = jwt ?: return false
        val base = jwtApiBase?.toHttpUrlOrNull() ?: return false
        if (t.isBlank()) return false
        return requestUrl.host == base.host &&
            requestUrl.port == base.port &&
            requestUrl.scheme == base.scheme
    }

    /** 为直连 meshchat-server 请求附加 Bearer；路径以 `/auth/` 开头时不加。 */
    fun bearerTokenFor(requestUrl: HttpUrl): String? {
        if (requestUrl.encodedPath.startsWith("/auth/")) return null
        if (!matches(requestUrl)) return null
        return jwt
    }
}
