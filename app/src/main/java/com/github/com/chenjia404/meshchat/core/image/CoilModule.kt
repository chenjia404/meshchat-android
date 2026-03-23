package com.github.com.chenjia404.meshchat.core.image

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

/**
 * 注册 [VideoFrameDecoder]，否则聊天里视频消息无法用 [coil.request.videoFrameMillis] 显示预览图。
 * 使用与应用相同的 [@Named("http")] 客户端，保证附件 URL 经 baseUrl 拦截器与接口一致。
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @Named("http") okHttpClient: OkHttpClient,
    ): ImageLoader =
        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
}
