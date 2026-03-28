package com.github.com.chenjia404.meshchat.service.meshchat

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshChatServerAuthInterceptor @Inject constructor(
    private val authStore: MeshChatServerAuthStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        if (path.startsWith("/auth/")) {
            return chain.proceed(req)
        }
        val token = authStore.bearerTokenFor(req.url) ?: return chain.proceed(req)
        return chain.proceed(
            req.newBuilder().header("Authorization", "Bearer $token").build(),
        )
    }
}
