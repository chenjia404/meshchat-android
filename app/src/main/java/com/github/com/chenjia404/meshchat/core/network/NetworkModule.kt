package com.github.com.chenjia404.meshchat.core.network

import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.data.remote.api.MeshChatApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    fun provideMeshChatApi(retrofit: Retrofit): MeshChatApi = retrofit.create(MeshChatApi::class.java)
}

