package com.github.com.chenjia404.meshchat.service.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.com.chenjia404.meshchat.MainActivity
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.data.local.db.AppDatabase
import com.github.com.chenjia404.meshchat.data.local.db.PublicChannelDatabase
import com.github.com.chenjia404.meshchat.data.remote.dto.ChatEventDto
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 本地聊天通知：MessagingStyle、按会话合并、与 [ChatActiveSessionStore] / 前后台协同。
 */
@Singleton
class LocalChatNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val publicChannelDatabase: PublicChannelDatabase,
    private val profileRepository: ProfileRepository,
    private val settingsStore: SettingsStore,
    private val activeSessionStore: ChatActiveSessionStore,
) {
    private val historyLock = Any()
    private val messageLines: MutableMap<String, ArrayDeque<Line>> = ConcurrentHashMap()

    private data class Line(val time: Long, val sender: String, val body: CharSequence)

    /** 通知摘要行长度上限，避免系统栏过长 */
    private fun trimNotificationLine(s: String, max: Int = 120): String =
        if (s.length <= max) s else s.take(max - 1) + "…"

    /**
     * 将 peer_id 解析为联系人备注/远端昵称；无则尝试本人资料；仍无则返回原 id。
     */
    private suspend fun peerDisplayName(peerId: String): String {
        val pid = peerId.trim()
        if (pid.isEmpty()) return context.getString(R.string.notification_new_message_fallback)
        appDatabase.contactDao().getContactOnce(pid)?.let { c ->
            c.nickname.takeIf { it.isNotBlank() }?.let { return it }
            c.remoteNickname?.takeIf { it.isNotBlank() }?.let { return it }
        }
        profileRepository.myProfile.first()?.let { p ->
            if (p.peerId.trim() == pid) {
                p.nickname.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return pid
    }

    fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(android.app.NotificationManager::class.java) ?: return
            val channel = android.app.NotificationChannel(
                CHANNEL_ID_MESSAGES,
                context.getString(R.string.notification_channel_chat_messages_name),
                android.app.NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_chat_messages_desc)
                enableVibration(true)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun cancelForDirect(conversationId: String) {
        cancelByKey(keyDirect(conversationId))
    }

    fun cancelForGroup(groupId: String) {
        cancelByKey(keyGroup(groupId))
    }

    fun cancelForPublicChannel(channelId: String) {
        cancelByKey(keyChannel(channelId))
    }

    private fun cancelByKey(key: String) {
        messageLines.remove(key)
        NotificationManagerCompat.from(context).cancel(key, stableNotificationId(key))
    }

    /**
     * meshproxy WebSocket [ChatEventDto]，仅处理 [type] == `message`（非纯状态同步）。
     */
    suspend fun onMeshProxySocketMessage(event: ChatEventDto) {
        if (event.type != "message") return
        if (!canPostNotifications()) return
        val myPeerId = profileRepository.myProfile.first()?.peerId?.trim().orEmpty()
        when (event.kind) {
            "group" -> {
                val groupId = event.groupId ?: event.conversationId ?: return
                val sender = event.senderPeerId?.trim().orEmpty()
                if (sender.isNotBlank() && myPeerId.isNotBlank() && sender == myPeerId) return
                if (shouldSuppressFor(ActiveChatSession.Group(groupId))) return
                val ge = appDatabase.groupDao().getGroupOnce(groupId)
                val title = ge?.title?.takeIf { it.isNotBlank() } ?: groupId
                val body = previewBodyFromSocket(event)
                val senderLabel = if (sender.isNotBlank()) peerDisplayName(sender) else title
                val sub = ge?.groupAbout?.takeIf { it.isNotBlank() }?.let { trimNotificationLine(it) }
                showGroupNotification(groupId, title, senderLabel, body, sub)
            }
            else -> {
                val cid = event.conversationId ?: return
                val dir = event.direction?.lowercase().orEmpty()
                if (dir == "outbound" || dir == "out") return
                if (shouldSuppressFor(ActiveChatSession.Direct(cid))) return
                val conv = appDatabase.directConversationDao().getConversationOnce(cid)
                val peerId = conv?.peerId.orEmpty()
                val contact = if (peerId.isNotBlank()) {
                    appDatabase.contactDao().getContactOnce(peerId)
                } else {
                    null
                }
                val chatTitle = contact?.nickname?.takeIf { it.isNotBlank() }
                    ?: contact?.remoteNickname?.takeIf { it.isNotBlank() }
                    ?: peerId.ifBlank { cid }
                val senderPeer = event.senderPeerId?.trim().orEmpty()
                val senderName = if (senderPeer.isNotEmpty()) peerDisplayName(senderPeer) else chatTitle
                val body = previewBodyFromSocket(event)
                val sub = contact?.bio?.takeIf { it.isNotBlank() }?.let { trimNotificationLine(it) }
                showDirectNotification(cid, chatTitle, senderName, body, sub)
            }
        }
    }

    suspend fun onSuperGroupSocketMessage(
        groupId: String,
        senderDisplay: String,
        body: String,
        senderKey: String,
    ) {
        if (!canPostNotifications()) return
        val myMeshId = settingsStore.meshChatServerUserIdFlow.value
        if (myMeshId != null && senderKey == "meshchat_user_$myMeshId") return
        if (shouldSuppressFor(ActiveChatSession.Group(groupId))) return
        val title = appDatabase.groupDao().getGroupOnce(groupId)?.title?.takeIf { it.isNotBlank() } ?: groupId
        showGroupNotification(groupId, title, senderDisplay.ifBlank { title }, body)
    }

    suspend fun onPublicChannelNewMessage(
        channelId: String,
        authorPeerId: String,
        preview: String,
    ) {
        if (!canPostNotifications()) return
        val myPeerId = profileRepository.myProfile.first()?.peerId?.trim().orEmpty()
        if (myPeerId.isNotBlank() && authorPeerId == myPeerId) return
        if (shouldSuppressFor(ActiveChatSession.PublicChannel(channelId))) return
        val ch = publicChannelDatabase.publicChannelDao().getChannel(channelId)
        val title = ch?.name?.takeIf { it.isNotBlank() } ?: channelId
        val c = appDatabase.contactDao().getContactOnce(authorPeerId)
        val authorLabel = c?.nickname?.takeIf { it.isNotBlank() }
            ?: c?.remoteNickname?.takeIf { it.isNotBlank() }
            ?: peerDisplayName(authorPeerId)
        val sub = ch?.bio?.takeIf { it.isNotBlank() }?.let { trimNotificationLine(it) }
        showPublicChannelNotification(channelId, title, authorLabel, preview, sub)
    }

    private suspend fun shouldSuppressFor(target: ActiveChatSession): Boolean {
        val appForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (!appForeground) return false
        return activeSessionStore.session.value == target
    }

    private fun previewBodyFromSocket(event: ChatEventDto): String {
        val t = event.plaintext?.trim().orEmpty()
        if (t.isNotEmpty()) return t
        val fn = event.fileName?.trim().orEmpty()
        if (fn.isNotEmpty()) {
            val mimeLabel = event.mimeType?.trim()?.takeIf { it.isNotEmpty() }
                ?: context.getString(R.string.notification_mime_fallback)
            return context.getString(R.string.notification_file_message_preview, mimeLabel, fn)
        }
        return context.getString(R.string.notification_new_message_fallback)
    }

    private fun showDirectNotification(
        conversationId: String,
        conversationTitle: String,
        senderName: String,
        body: String,
        subText: String? = null,
    ) {
        val key = keyDirect(conversationId)
        val lines = appendLine(key, senderName, body)
        postMessagingNotification(
            key = key,
            conversationTitle = conversationTitle,
            ticker = "$senderName: $body",
            lines = lines,
            contentIntent = pendingOpenIntent(OpenKind.DIRECT, conversationId),
            subText = subText,
        )
    }

    private fun showGroupNotification(
        groupId: String,
        groupTitle: String,
        senderName: String,
        body: String,
        subText: String? = null,
    ) {
        val key = keyGroup(groupId)
        val lines = appendLine(key, senderName, body)
        postMessagingNotification(
            key = key,
            conversationTitle = groupTitle,
            ticker = "$senderName: $body",
            lines = lines,
            contentIntent = pendingOpenIntent(OpenKind.GROUP, groupId),
            subText = subText,
        )
    }

    private fun showPublicChannelNotification(
        channelId: String,
        channelTitle: String,
        authorName: String,
        body: String,
        subText: String? = null,
    ) {
        val key = keyChannel(channelId)
        val lines = appendLine(key, authorName, body)
        postMessagingNotification(
            key = key,
            conversationTitle = channelTitle,
            ticker = "$authorName: $body",
            lines = lines,
            contentIntent = pendingOpenIntent(OpenKind.CHANNEL, channelId),
            subText = subText,
        )
    }

    private fun appendLine(key: String, sender: String, body: CharSequence): List<Line> {
        synchronized(historyLock) {
            val dq = messageLines.getOrPut(key) { ArrayDeque() }
            dq.addLast(Line(System.currentTimeMillis(), sender, body))
            while (dq.size > MAX_LINES) dq.removeFirst()
            return dq.toList()
        }
    }

    private fun postMessagingNotification(
        key: String,
        conversationTitle: String,
        ticker: String,
        lines: List<Line>,
        contentIntent: PendingIntent,
        subText: String? = null,
    ) {
        val self = Person.Builder().setName(context.getString(R.string.app_name)).build()
        val style = NotificationCompat.MessagingStyle(self)
            .setConversationTitle(conversationTitle)
        lines.forEach { line ->
            val senderPerson = Person.Builder().setName(line.sender).build()
            style.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    line.body,
                    line.time,
                    senderPerson,
                ),
            )
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(conversationTitle)
            .setContentText(ticker)
            .setStyle(style)
            .setTicker(ticker)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setGroup(GROUP_KEY_CHAT)
        if (!subText.isNullOrBlank()) {
            builder.setSubText(subText.trim())
        }
        NotificationManagerCompat.from(context).notify(key, stableNotificationId(key), builder.build())
    }

    private enum class OpenKind { DIRECT, GROUP, CHANNEL }

    private fun pendingOpenIntent(kind: OpenKind, id: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true)
            putExtra(
                MainActivity.EXTRA_OPEN_TYPE,
                when (kind) {
                    OpenKind.DIRECT -> OPEN_TYPE_DIRECT
                    OpenKind.GROUP -> OPEN_TYPE_GROUP
                    OpenKind.CHANNEL -> OPEN_TYPE_CHANNEL
                },
            )
            putExtra(MainActivity.EXTRA_OPEN_ID, id)
        }
        val req = stableNotificationId("pi_${kind.name}_$id")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, req, intent, flags)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        const val CHANNEL_ID_MESSAGES = "meshchat_chat_messages"
        private const val GROUP_KEY_CHAT = "meshchat_active_chats"
        private const val MAX_LINES = 8

        const val OPEN_TYPE_DIRECT = "direct"
        const val OPEN_TYPE_GROUP = "group"
        const val OPEN_TYPE_CHANNEL = "channel"

        fun keyDirect(conversationId: String) = "d:$conversationId"
        fun keyGroup(groupId: String) = "g:$groupId"
        fun keyChannel(channelId: String) = "c:$channelId"

        fun stableNotificationId(key: String): Int = (key.hashCode() and 0x7FFF_FFFF)
    }
}
