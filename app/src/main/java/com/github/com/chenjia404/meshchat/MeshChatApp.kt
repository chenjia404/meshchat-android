package com.github.com.chenjia404.meshchat

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.github.com.chenjia404.meshchat.core.binary.BinaryRuntime
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import com.github.com.chenjia404.meshchat.data.repository.AppCoordinator
import com.github.com.chenjia404.meshchat.service.notification.LocalChatNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

@HiltAndroidApp
class MeshChatApp : Application() {
    @Inject
    lateinit var appCoordinator: AppCoordinator

    @Inject
    lateinit var localChatNotifier: LocalChatNotifier

    @Inject
    lateinit var binaryRuntime: BinaryRuntime

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        Coil.setImageLoader(imageLoader)
        localChatNotifier.ensureChannelsCreated()
        if (binaryRuntime.startIfNeeded()) {
            appCoordinator.start()
            applicationScope.launch {
                runCatching { appCoordinator.refreshAll() }
            }
        }
    }
}
