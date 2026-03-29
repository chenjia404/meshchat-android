package com.github.com.chenjia404.meshchat

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.github.com.chenjia404.meshchat.service.notification.ChatOpenNavigationBus
import com.github.com.chenjia404.meshchat.service.notification.ChatOpenRequest
import com.github.com.chenjia404.meshchat.service.notification.LocalChatNotifier
import com.github.com.chenjia404.meshchat.share.IncomingShareManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var incomingShareManager: IncomingShareManager

    @Inject
    lateinit var chatOpenNavigationBus: ChatOpenNavigationBus

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShareManager.setFromIntent(intent)
        requestPostNotificationsIfNeeded()
        setContent { AppContent(incomingShareManager) }
        dispatchNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShareManager.setFromIntent(intent)
        dispatchNotificationIntent(intent)
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun dispatchNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false) != true) return
        val type = intent.getStringExtra(EXTRA_OPEN_TYPE) ?: return
        val id = intent.getStringExtra(EXTRA_OPEN_ID) ?: return
        val req = when (type) {
            LocalChatNotifier.OPEN_TYPE_DIRECT -> ChatOpenRequest.DirectChat(id)
            LocalChatNotifier.OPEN_TYPE_GROUP -> ChatOpenRequest.GroupChat(id)
            LocalChatNotifier.OPEN_TYPE_CHANNEL -> ChatOpenRequest.PublicChannel(id)
            else -> return
        }
        chatOpenNavigationBus.requestOpen(req)
    }

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "meshchat_extra_from_notification"
        const val EXTRA_OPEN_TYPE = "meshchat_extra_open_type"
        const val EXTRA_OPEN_ID = "meshchat_extra_open_id"
    }
}
