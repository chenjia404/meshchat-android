package com.github.com.chenjia404.meshchat.data.remote.ws

import com.github.com.chenjia404.meshchat.core.datastore.SettingsStore
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import com.github.com.chenjia404.meshchat.core.dispatchers.IoDispatcher
import com.github.com.chenjia404.meshchat.core.util.MeshchatHttpErrors
import com.github.com.chenjia404.meshchat.core.util.buildMeshChatServerWebSocketUrl
import com.github.com.chenjia404.meshchat.core.util.normalizeMeshChatServerBaseUrl
import com.github.com.chenjia404.meshchat.data.local.db.AppDatabase
import com.github.com.chenjia404.meshchat.data.local.entity.GroupMessageEntity
import androidx.room.withTransaction
import com.github.com.chenjia404.meshchat.data.mapper.toGroupEntity
import com.github.com.chenjia404.meshchat.data.mapper.toGroupMessageEntity
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerGroupDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerMessageDto
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.service.notification.LocalChatNotifier

/**
 * meshchat-server 实时通道：[`/ws?token=`](https://github.com/chenjia404/meshchat-server/blob/master/docs/API.md)
 * 连接后发送 `subscribe` 订阅本地已缓存的超级群，处理 `group.message.*` / `group.settings.updated` 等事件并写 Room。
 *
 * 当前仅支持 **与 DataStore 中 JWT 成对的单个 meshchat-server 基址**（与现有登录模型一致）。
 */
@Singleton
class MeshChatServerWebSocket @Inject constructor(
    private val database: AppDatabase,
    private val gson: Gson,
    private val groupRepository: GroupRepository,
    private val localChatNotifier: LocalChatNotifier,
    private val settingsStore: SettingsStore,
    @Named("ws") private val webSocketClient: OkHttpClient,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val started = AtomicBoolean(false)

    private data class WsDrive(
        val token: String,
        val apiBaseWithSlash: String,
        val groupIds: List<String>,
    )

    fun start() {
        if (!started.compareAndSet(false, true)) return
        applicationScope.launch(ioDispatcher) {
            combine(
                settingsStore.meshChatServerJwtPairFlow,
                database.groupDao().observeSuperGroups(),
            ) { pair, superEntities ->
                val token = pair.first?.trim()?.takeIf { it.isNotEmpty() } ?: return@combine null
                val base = pair.second?.trim()?.takeIf { it.isNotEmpty() } ?: return@combine null
                val baseNorm = normalizeMeshChatServerBaseUrl(base)
                val ids = superEntities
                    .asSequence()
                    .filter { normalizeMeshChatServerBaseUrl(it.superGroupApiBaseUrl.orEmpty()) == baseNorm }
                    .map { it.groupId }
                    .distinct()
                    .sorted()
                    .toList()
                if (ids.isEmpty()) return@combine null
                WsDrive(token = token, apiBaseWithSlash = normalizeMeshChatServerBaseUrl(base), groupIds = ids)
            }
                .distinctUntilChanged()
                .collectLatest { drive ->
                    if (drive == null) return@collectLatest
                    while (isActive) {
                        runConnection(drive)
                        delay(2_000L)
                    }
                }
        }
    }

    private suspend fun runConnection(drive: WsDrive) {
        val closeSignal = CompletableDeferred<Unit>()
        val url = buildMeshChatServerWebSocketUrl(drive.apiBaseWithSlash, drive.token)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${drive.token}")
            .build()
        val subscribeJson = buildSubscribePayload(drive.groupIds)
        val socket = webSocketClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(subscribeJson)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    applicationScope.launch(ioDispatcher) {
                        handleIncoming(text, drive.apiBaseWithSlash)
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                    closeSignal.complete(Unit)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    closeSignal.complete(Unit)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    MeshchatHttpErrors.log("meshchat_server_ws", t)
                    closeSignal.complete(Unit)
                }
            },
        )
        try {
            closeSignal.await()
        } finally {
            socket.cancel()
        }
    }

    private fun buildSubscribePayload(groupIds: List<String>): String {
        val o = JsonObject()
        o.addProperty("action", "subscribe")
        val arr = JsonArray()
        groupIds.forEach { arr.add(it) }
        o.add("group_ids", arr)
        return gson.toJson(o)
    }

    private suspend fun handleIncoming(text: String, apiBaseWithSlash: String) {
        val root = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull() ?: return
        val type = root.get("type")?.asString ?: return
        val dataEl = root.get("data")
        when (type) {
            "subscription.updated" -> Unit
            "group.message.created",
            "group.message.edited",
            "group.message.deleted",
            -> {
                if (dataEl == null || !dataEl.isJsonObject) return
                val dto = runCatching {
                    gson.fromJson(dataEl, MeshChatServerMessageDto::class.java)
                }.getOrNull() ?: return
                runCatching {
                    val entity = dto.toGroupMessageEntity()
                    database.withTransaction {
                        database.groupMessageDao().upsert(entity)
                        val activityAt = entity.createdAt.trim().takeIf { it.isNotEmpty() }
                            ?: Instant.now().toString()
                        database.groupDao().patchLastMessageAt(
                            groupId = dto.groupId,
                            lastMessageAt = activityAt,
                            updatedAt = activityAt,
                        )
                    }
                }.onFailure { e ->
                    MeshchatHttpErrors.log("meshchat_server_ws_message_upsert", e)
                }
                if (type == "group.message.created") {
                    runCatching { notifySuperGroupCreated(dto) }
                }
            }
            "group.settings.updated" -> {
                if (dataEl == null || !dataEl.isJsonObject) return
                val dto = runCatching {
                    gson.fromJson(dataEl, MeshChatServerGroupDto::class.java)
                }.getOrNull() ?: return
                runCatching {
                    database.groupDao().upsert(dto.toGroupEntity(normalizeMeshChatServerBaseUrl(apiBaseWithSlash)))
                }.onFailure { e ->
                    MeshchatHttpErrors.log("meshchat_server_ws_group_upsert", e)
                }
            }
            "group.member.updated" -> {
                val gid = extractGroupIdFromMemberEvent(dataEl) ?: return
                runCatching { groupRepository.refreshGroup(gid) }
                    .onFailure { e -> MeshchatHttpErrors.log("meshchat_server_ws_member_refresh($gid)", e) }
            }
            else -> Unit
        }
    }

    /**
     * 文档中 `GroupMember` 未强制带 `group_id`；若服务端在 `data` 顶层附带则用于刷新群成员。
     */
    private suspend fun notifySuperGroupCreated(dto: MeshChatServerMessageDto) {
        val entity = dto.toGroupMessageEntity()
        val preview = superGroupPreviewLine(dto, entity)
        if (preview.isBlank()) return
        val senderId = dto.sender?.id?.let { "meshchat_user_$it" } ?: "meshchat_unknown"
        val label = dto.sender?.displayName?.takeIf { it.isNotBlank() }
            ?: dto.sender?.username?.takeIf { it.isNotBlank() }
            ?: senderId
        localChatNotifier.onSuperGroupSocketMessage(
            groupId = dto.groupId,
            senderDisplay = label,
            body = preview,
            senderKey = senderId,
        )
    }

    private fun superGroupPreviewLine(dto: MeshChatServerMessageDto, entity: GroupMessageEntity): String {
        return when (dto.contentType) {
            "text", "forward" -> entity.plaintext?.trim().orEmpty()
            "image" -> "[图片]"
            "video" -> "[视频]"
            "voice" -> "[语音]"
            "file" -> entity.fileName?.let { "[文件] $it" } ?: "[文件]"
            else -> entity.plaintext?.trim().orEmpty()
        }
    }

    private fun extractGroupIdFromMemberEvent(dataEl: JsonElement?): String? {
        if (dataEl == null || !dataEl.isJsonObject) return null
        val o = dataEl.asJsonObject
        if (o.has("group_id")) {
            return o.get("group_id")?.asString?.trim()?.takeIf { it.isNotEmpty() }
        }
        return null
    }
}
