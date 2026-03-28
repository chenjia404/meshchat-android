package com.github.com.chenjia404.meshchat.core.network

import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatApi
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatServerDirectApi
import com.github.com.chenjia404.meshchat.service.meshchat.MeshChatServerAuthInterceptor
import com.github.com.chenjia404.meshchat.service.meshchat.MeshChatServerSessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideBaseUrlInterceptor(settingsStore: SettingsStore): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val configuredBase = settingsStore.currentBaseHttpUrl()
        val rewrittenUrl = original.url.newBuilder()
            .scheme(configuredBase.scheme)
            .host(configuredBase.host)
            .port(configuredBase.port)
            .build()
        chain.proceed(
            original.newBuilder()
                .url(rewrittenUrl)
                .build(),
        )
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    @Provides
    @Singleton
    @Named("http")
    fun provideHttpClient(
        baseUrlInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @Named("ws")
    fun provideWebSocketClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @Named("meshproxy")
    fun provideRetrofit(
        @Named("http") okHttpClient: OkHttpClient,
        gson: Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:19080/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideMeshChatApi(@Named("meshproxy") retrofit: Retrofit): MeshChatApi =
        retrofit.create(MeshChatApi::class.java)

    @Provides
    @Singleton
    @Named("meshchatServerDirectClient")
    fun provideMeshChatServerDirectClient(
        authInterceptor: MeshChatServerAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        lazySession: Lazy<MeshChatServerSessionManager>,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator { _, response ->
            val req = response.request
            if (req.header("Mesh-Auth-Retry") != null) return@authenticator null
            if (req.url.encodedPath.startsWith("/auth/")) return@authenticator null
            if (response.code != 401) return@authenticator null
            lazySession.get().forceRefreshBlocking(req.url)
            val token = lazySession.get().currentBearerFor(req.url) ?: return@authenticator null
            req.newBuilder()
                .removeHeader("Authorization")
                .header("Authorization", "Bearer $token")
                .header("Mesh-Auth-Retry", "1")
                .build()
        }
        .build()

    @Provides
    @Singleton
    @Named("meshchatServerDirectRetrofit")
    fun provideMeshChatServerDirectRetrofit(
        @Named("meshchatServerDirectClient") client: OkHttpClient,
        gson: Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideMeshChatServerDirectApi(
        @Named("meshchatServerDirectRetrofit") retrofit: Retrofit,
    ): MeshChatServerDirectApi = retrofit.create(MeshChatServerDirectApi::class.java)
}

