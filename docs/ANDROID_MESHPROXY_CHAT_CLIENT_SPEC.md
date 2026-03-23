# Android 原生客户端实现文档（meshproxy 专用版，给 Codex）

## 1. 项目背景

我已经有一个去中心化聊天协议服务 `meshproxy`，Android 客户端**不实现底层协议本身**，只通过本地 HTTP API 和 WebSocket 与 meshproxy 通信。

meshproxy 提供的聊天相关能力包括：

* 个人资料 / 头像
* 好友请求
* 联系人
* 单聊会话与消息
* 群组与群消息
* 文件发送 / 下载
* WebSocket 实时事件推送

API 基底 URL 默认由 `api.listen` 决定，文档默认示例为 `http://127.0.0.1:19080`；WebSocket 地址为 `/api/v1/chat/ws`。当前 Local API 没有独立 API Key。([GitHub][1])

应用包名:com.github.com.chenjia404.meshchat
api文档：https://github.com/chenjia404/meshproxy/blob/master/chat-api.md

---

## 2. 目标

开发一个 **Android 原生聊天客户端**，界面和交互风格类似微信，但不要求视觉完全复刻。

必须支持：

* 私聊
* 群聊
* 文本消息
* 图片消息
* 视频消息
* 语音消息
* 普通文件消息
* HTTP 拉取数据
* WebSocket 实时更新
* 本地数据库缓存
* 断线重连
* 文件下载
* 图片预览 / 视频播放 / 语音播放

---

## 3. 协议事实约束（必须遵守）

这一节是 **Codex 不允许擅自改动** 的部分。

### 3.1 资料与头像

* `GET /api/v1/chat/me`
* `GET /api/v1/chat/profile`
* `POST /api/v1/chat/profile`
* `POST /api/v1/chat/profile/avatar`
* `GET /api/v1/chat/avatars/{name}`

其中头像上传使用 `multipart/form-data`，字段名固定为 `avatar`；头像访问路径为 `/api/v1/chat/avatars/{name}`。([GitHub][1])

### 3.2 好友请求

* `GET /api/v1/chat/requests`
* `POST /api/v1/chat/requests`
* `POST /api/v1/chat/requests/{request_id}/accept`
* `POST /api/v1/chat/requests/{request_id}/reject`

接受好友请求后返回的是一个 `Conversation` 对象，而不是通用 `Contact`。该对象包含 `conversation_id`、`peer_id`、`state`、`last_message_at`、`last_transport_mode`、`unread_count`、`retention_minutes`、`retention_sync_state`、`retention_synced_at`、`created_at`、`updated_at`。([GitHub][1])

### 3.3 联系人

* `GET /api/v1/chat/contacts`
* `DELETE /api/v1/chat/contacts/{peer_id}`
* `POST /api/v1/chat/contacts/{peer_id}/nickname`
* `POST /api/v1/chat/contacts/{peer_id}/block`

联系人字段包括 `peer_id`、`nickname`、`bio`、`avatar`、`cid`、`remote_nickname`、`retention_minutes`、`blocked`、`last_seen_at`、`updated_at`。([GitHub][1])

### 3.4 单聊会话与消息

* `GET /api/v1/chat/conversations`
* `DELETE /api/v1/chat/conversations/{conversation_id}`
* `POST /api/v1/chat/conversations/{conversation_id}/sync`
* `POST /api/v1/chat/conversations/{conversation_id}/read`
* `GET /api/v1/chat/conversations/{conversation_id}/messages`
* `POST /api/v1/chat/conversations/{conversation_id}/messages`
* `POST /api/v1/chat/conversations/{conversation_id}/files`
* `GET /api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/file`
* `POST /api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/revoke`
* `POST /api/v1/chat/conversations/{conversation_id}/retention`

单聊消息列表有一个特别点：
**无查询参数时返回数组；带 `limit`/`offset` 分页参数时返回对象**，对象结构为 `messages / total / limit / offset / has_more`。([GitHub][1])

### 3.5 群组与群消息

* `GET /api/v1/groups`
* `POST /api/v1/groups`
* `GET /api/v1/groups/{group_id}`
* `POST /api/v1/groups/{group_id}/{action}`
* `GET /api/v1/groups/{group_id}/messages`
* `POST /api/v1/groups/{group_id}/messages`
* `POST /api/v1/groups/{group_id}/files`
* `GET /api/v1/groups/{group_id}/messages/{msg_id}/file`
* `POST /api/v1/groups/{group_id}/messages/{msg_id}/revoke`
* `POST /api/v1/groups/{group_id}/sync`

群组 action 路径统一是 `/api/v1/groups/{group_id}/{action}`，支持 `invite / join / leave / remove / title / retention / dissolve / controller`。群消息 GET 返回数组，POST 文本发送 body 为 `{"text":"..."}`。([GitHub][1])

### 3.6 WebSocket

WebSocket 地址是：

`ws://{host}:{port}/api/v1/chat/ws`

事件对象 `ChatEvent` 的 `type` 只包括：

* `message`
* `message_state`
* `friend_request`
* `conversation_deleted`
* `contact_deleted`

并且有 `kind` 字段区分 `direct` 或 `group`。`message` 表示“新消息或需刷新列表”，客户端不能简单假设它永远只代表 append 一条完整新消息。([GitHub][1])

---

## 4. 客户端设计原则

下面这些是 **客户端架构设计决策**，不是协议硬性规定。

1. UI 层不能直接依赖 DTO
2. 必须使用：

   * `Remote DTO -> Domain Model -> Local Entity -> UI Model`
3. direct chat 和 group chat 后端模型不同，客户端也应该分开存储
4. 聊天 UI 可以统一抽象成一个 `UiMessage`
5. 图片 / 视频 / 音频 / 普通文件 **不是后端独立 msg_type**，客户端应根据 `msg_type + mime_type` 推断渲染类型。这个判断依据来自 direct message 与 group message 的字段定义：后端主要是 `chat_text/chat_file` 与 `group_chat_text/group_chat_file`。([GitHub][1])

---

## 5. 技术栈要求

请使用：

* Kotlin
* Jetpack Compose
* MVVM
* Repository
* Retrofit + OkHttp
* OkHttp WebSocket
* Room
* Hilt
* DataStore
* Kotlin Coroutines + Flow
* Navigation Compose
* Coil
* AndroidX Media3

不要使用 XML 作为主 UI。
不要在 UI 层直接调用 Retrofit。
不要把 WebSocket 解析逻辑散落到各页面。

---

## 6. 项目结构要求

建议单 `app` module，内部按 feature 分包。

```text
app/
  src/main/java/com/example/meshchat/
    App.kt

    core/
      common/
      ui/
      util/
      network/
      dispatchers/

    data/
      remote/
        dto/
        api/
        ws/
      local/
        db/
        entity/
        dao/
      mapper/
      repository/

    domain/
      model/
      repository/
      usecase/

    feature/
      splash/
      home/
      chatlist/
      contacts/
      groups/
      chatroom/
      media/
      settings/

    service/
      download/
      playback/
      storage/

    navigation/
```

---

## 7. Domain Model 设计

下面的字段命名是客户端 domain model，**不是直接照抄 DTO**。
但字段语义必须和 meshproxy 对齐。对应来源见各 API 章节。([GitHub][1])

### 7.1 Profile

```kotlin
data class Profile(
    val peerId: String,
    val nickname: String,
    val bio: String,
    val avatar: String?,
    val avatarCid: String?,
    val chatKexPub: String?,
    val createdAt: String
)
```

### 7.2 Contact

```kotlin
data class Contact(
    val peerId: String,
    val nickname: String,
    val bio: String,
    val avatar: String?,
    val cid: String?,
    val remoteNickname: String?,
    val retentionMinutes: Int,
    val blocked: Boolean,
    val lastSeenAt: String?,
    val updatedAt: String
)
```

### 7.3 FriendRequest

```kotlin
data class FriendRequest(
    val requestId: String,
    val fromPeerId: String,
    val toPeerId: String,
    val state: String,
    val introText: String?,
    val nickname: String?,
    val bio: String?,
    val avatar: String?,
    val retentionMinutes: Int?,
    val remoteChatKexPub: String?,
    val conversationId: String?,
    val lastTransportMode: String?,
    val retryCount: Int?,
    val nextRetryAt: String?,
    val retryJobStatus: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### 7.4 DirectConversation

```kotlin
data class DirectConversation(
    val conversationId: String,
    val peerId: String,
    val state: String,
    val lastMessageAt: String?,
    val lastTransportMode: String?,
    val unreadCount: Int,
    val retentionMinutes: Int,
    val retentionSyncState: String?,
    val retentionSyncedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### 7.5 DirectMessage

```kotlin
data class DirectMessage(
    val msgId: String,
    val conversationId: String,
    val senderPeerId: String,
    val receiverPeerId: String,
    val direction: String,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val transportMode: String?,
    val state: String,
    val counter: Long?,
    val createdAt: String,
    val deliveredAt: String?
)
```

### 7.6 Group

```kotlin
data class Group(
    val groupId: String,
    val title: String,
    val avatar: String?,
    val controllerPeerId: String,
    val currentEpoch: Long,
    val retentionMinutes: Int,
    val state: String,
    val lastEventSeq: Long,
    val lastMessageAt: String?,
    val memberCount: Int?,
    val localMemberRole: String?,
    val localMemberState: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### 7.7 GroupMember

```kotlin
data class GroupMember(
    val groupId: String,
    val peerId: String,
    val role: String,
    val state: String,
    val invitedBy: String?,
    val joinedEpoch: Long?,
    val leftEpoch: Long?,
    val updatedAt: String
)
```

### 7.8 GroupMessage

```kotlin
data class GroupMessage(
    val msgId: String,
    val groupId: String,
    val epoch: Long,
    val senderPeerId: String,
    val senderSeq: Long,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val signature: String?,
    val state: String,
    val deliverySummary: GroupDeliverySummary?,
    val createdAt: String
)

data class GroupDeliverySummary(
    val total: Int,
    val pending: Int,
    val sentToTransport: Int,
    val queuedForRetry: Int,
    val deliveredRemote: Int,
    val failed: Int
)
```

---

## 8. UI 统一模型

虽然 direct 和 group 后端不一样，但 Compose 页面可以统一使用一个 UI 模型。

### 8.1 枚举

```kotlin
enum class UiConversationKind {
    DIRECT, GROUP
}

enum class UiMessageContentKind {
    TEXT, IMAGE, VIDEO, AUDIO, FILE, SYSTEM
}
```

### 8.2 统一消息模型

```kotlin
data class UiMessage(
    val id: String,
    val conversationId: String,
    val conversationKind: UiConversationKind,
    val senderPeerId: String,
    val senderDisplayName: String?,
    val isSelf: Boolean,
    val contentKind: UiMessageContentKind,
    val backendMsgType: String,
    val text: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val createdAt: String,
    val state: String,
    val deliveredAt: String? = null,
    val deliverySummary: GroupDeliverySummary? = null
)
```

### 8.3 客户端消息类型判断规则

这部分是客户端策略，但依据是后端 `msg_type` 定义。([GitHub][1])

```kotlin
fun resolveUiMessageContentKind(msgType: String, mimeType: String?): UiMessageContentKind {
    return when (msgType) {
        "chat_text", "group_chat_text" -> UiMessageContentKind.TEXT
        "chat_file", "group_chat_file" -> when {
            mimeType?.startsWith("image/") == true -> UiMessageContentKind.IMAGE
            mimeType?.startsWith("video/") == true -> UiMessageContentKind.VIDEO
            mimeType?.startsWith("audio/") == true -> UiMessageContentKind.AUDIO
            else -> UiMessageContentKind.FILE
        }
        else -> UiMessageContentKind.SYSTEM
    }
}
```

`SYSTEM` 用来兼容 `retention_update`、`group_invite_notice` 等非普通气泡类消息。WebSocket 文档明确列出了 `retention_update` 等消息类型示例。([GitHub][1])

---

## 9. DTO 层命名要求

请不要再使用过于抽象的通用名字，直接按协议拆 DTO：

```text
ProfileDto
ContactDto
FriendRequestDto
DirectConversationDto
DirectMessageDto
PagedDirectMessagesDto
GroupDto
GroupDetailsDto
GroupMemberDto
GroupMessageDto
GroupDeliverySummaryDto
ChatEventDto
```

---

## 10. Room 本地数据库设计

由于协议天然分 direct / group 两套，数据库也分开设计。

### 10.1 表

* `profiles`
* `contacts`
* `friend_requests`
* `direct_conversations`
* `direct_messages`
* `groups`
* `group_members`
* `group_messages`

### 10.2 DirectConversationEntity

```kotlin
@Entity(tableName = "direct_conversations")
data class DirectConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerId: String,
    val state: String,
    val lastMessageAt: String?,
    val lastTransportMode: String?,
    val unreadCount: Int,
    val retentionMinutes: Int,
    val retentionSyncState: String?,
    val retentionSyncedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### 10.3 DirectMessageEntity

```kotlin
@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val msgId: String,
    val conversationId: String,
    val senderPeerId: String,
    val receiverPeerId: String,
    val direction: String,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val transportMode: String?,
    val state: String,
    val counter: Long?,
    val createdAt: String,
    val deliveredAt: String?
)
```

### 10.4 GroupEntity

```kotlin
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val title: String,
    val avatar: String?,
    val controllerPeerId: String,
    val currentEpoch: Long,
    val retentionMinutes: Int,
    val state: String,
    val lastEventSeq: Long,
    val lastMessageAt: String?,
    val memberCount: Int?,
    val localMemberRole: String?,
    val localMemberState: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### 10.5 GroupMessageEntity

```kotlin
@Entity(tableName = "group_messages")
data class GroupMessageEntity(
    @PrimaryKey val msgId: String,
    val groupId: String,
    val epoch: Long,
    val senderPeerId: String,
    val senderSeq: Long,
    val msgType: String,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileCid: String?,
    val signature: String?,
    val state: String,
    val deliverySummaryJson: String?,
    val createdAt: String
)
```

### 10.6 存储原则

* 所有 HTTP 拉取结果先写入 DB
* 所有 WebSocket 事件先写入 DB
* UI 统一观察 Room Flow
* 会话列表先显示本地，再后台刷新
* direct messages 支持分页
* group messages 第一版可以先按全量列表实现，因为文档当前只明确了 direct messages 的 offset/limit 分页；group messages 文档写的是列表数组返回。([GitHub][1])

---

## 11. 聊天列表组装规则

### 11.1 单聊列表项

meshproxy 的 direct conversation 不直接返回标题和头像，需要客户端自己关联联系人数据。`DirectConversation` 只提供 `peer_id` 和会话相关状态字段。联系人资料则在 `GET /api/v1/chat/contacts` 返回。([GitHub][1])

单聊列表项 UI 规则：

* 会话 ID：`conversation_id`
* 标题：从 `Contact.nickname` 取
* 副标题：最后一条消息摘要
* 头像：若 `contact.avatar` 有值，则拼 `/api/v1/chat/avatars/{name}`
* 时间：`last_message_at`
* 未读：`unread_count`

### 11.2 群聊列表项

群组列表项可直接来自 `Group`：

* 会话 ID：`group_id`
* 标题：`title`
* 头像：`avatar`
* 时间：`last_message_at`
* 摘要：最后一条群消息推导

---

## 12. HTTP API 接口定义要求

下面是客户端 Retrofit 层建议接口。路径和字段必须与文档一致。([GitHub][1])

### 12.1 ProfileApi

```kotlin
interface ProfileApi {
    @GET("/api/v1/chat/me")
    suspend fun getMe(): ProfileDto

    @GET("/api/v1/chat/profile")
    suspend fun getProfile(): ProfileDto

    @POST("/api/v1/chat/profile")
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest
    ): ProfileDto

    @Multipart
    @POST("/api/v1/chat/profile/avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): ProfileDto
}
```

### 12.2 FriendRequestApi

```kotlin
interface FriendRequestApi {
    @GET("/api/v1/chat/requests")
    suspend fun getRequests(): List<FriendRequestDto>

    @POST("/api/v1/chat/requests")
    suspend fun createRequest(
        @Body body: CreateFriendRequestRequest
    ): FriendRequestDto

    @POST("/api/v1/chat/requests/{requestId}/accept")
    suspend fun acceptRequest(
        @Path("requestId") requestId: String
    ): DirectConversationDto

    @POST("/api/v1/chat/requests/{requestId}/reject")
    suspend fun rejectRequest(
        @Path("requestId") requestId: String
    ): RejectRequestResponseDto
}
```

### 12.3 ContactApi

```kotlin
interface ContactApi {
    @GET("/api/v1/chat/contacts")
    suspend fun getContacts(): List<ContactDto>

    @DELETE("/api/v1/chat/contacts/{peerId}")
    suspend fun deleteContact(
        @Path("peerId") peerId: String
    ): DeleteContactResponseDto

    @POST("/api/v1/chat/contacts/{peerId}/nickname")
    suspend fun updateContactNickname(
        @Path("peerId") peerId: String,
        @Body body: UpdateNicknameRequest
    ): ContactDto

    @POST("/api/v1/chat/contacts/{peerId}/block")
    suspend fun blockContact(
        @Path("peerId") peerId: String,
        @Body body: BlockContactRequest
    ): ContactDto
}
```

### 12.4 DirectConversationApi

```kotlin
interface DirectConversationApi {
    @GET("/api/v1/chat/conversations")
    suspend fun getConversations(): List<DirectConversationDto>

    @DELETE("/api/v1/chat/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): DeleteConversationResponseDto

    @POST("/api/v1/chat/conversations/{conversationId}/sync")
    suspend fun syncConversation(
        @Path("conversationId") conversationId: String
    ): SyncConversationResponseDto

    @POST("/api/v1/chat/conversations/{conversationId}/read")
    suspend fun markConversationRead(
        @Path("conversationId") conversationId: String
    ): DirectConversationDto

    @POST("/api/v1/chat/conversations/{conversationId}/retention")
    suspend fun updateRetention(
        @Path("conversationId") conversationId: String,
        @Body body: UpdateRetentionRequest
    ): DirectConversationDto
}
```

### 12.5 DirectMessageApi

这里要特别注意消息列表的“两种返回形态”。文档明确写了：无参数返回数组，分页返回对象。([GitHub][1])

```kotlin
interface DirectMessageApi {
    @GET("/api/v1/chat/conversations/{conversationId}/messages")
    suspend fun getMessagesRaw(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): ResponseBody

    @POST("/api/v1/chat/conversations/{conversationId}/messages")
    suspend fun sendTextMessage(
        @Path("conversationId") conversationId: String,
        @Body body: SendTextMessageRequest
    ): DirectMessageDto

    @Multipart
    @POST("/api/v1/chat/conversations/{conversationId}/files")
    suspend fun sendFileMessage(
        @Path("conversationId") conversationId: String,
        @Part file: MultipartBody.Part
    ): DirectMessageDto

    @Streaming
    @GET("/api/v1/chat/conversations/{conversationId}/messages/{msgId}/file")
    suspend fun downloadFile(
        @Path("conversationId") conversationId: String,
        @Path("msgId") msgId: String
    ): ResponseBody

    @POST("/api/v1/chat/conversations/{conversationId}/messages/{msgId}/revoke")
    suspend fun revokeMessage(
        @Path("conversationId") conversationId: String,
        @Path("msgId") msgId: String
    ): RevokeMessageResponseDto
}
```

**实现要求：**
客户端自己写 `DirectMessageResponseParser`：

* 如果返回体是 `[` 开头，解析为 `List<DirectMessageDto>`
* 如果返回体是 `{` 开头，解析为 `PagedDirectMessagesDto`

### 12.6 GroupApi

```kotlin
interface GroupApi {
    @GET("/api/v1/groups")
    suspend fun getGroups(): List<GroupDto>

    @POST("/api/v1/groups")
    suspend fun createGroup(
        @Body body: CreateGroupRequest
    ): GroupDto

    @GET("/api/v1/groups/{groupId}")
    suspend fun getGroupDetails(
        @Path("groupId") groupId: String
    ): GroupDetailsDto

    @POST("/api/v1/groups/{groupId}/invite")
    suspend fun invite(
        @Path("groupId") groupId: String,
        @Body body: InviteMemberRequest
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/join")
    suspend fun join(
        @Path("groupId") groupId: String
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/leave")
    suspend fun leave(
        @Path("groupId") groupId: String,
        @Body body: LeaveGroupRequest? = null
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/remove")
    suspend fun remove(
        @Path("groupId") groupId: String,
        @Body body: RemoveMemberRequest
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/title")
    suspend fun updateTitle(
        @Path("groupId") groupId: String,
        @Body body: UpdateGroupTitleRequest
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/retention")
    suspend fun updateRetention(
        @Path("groupId") groupId: String,
        @Body body: UpdateRetentionRequest
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/dissolve")
    suspend fun dissolve(
        @Path("groupId") groupId: String,
        @Body body: DissolveGroupRequest? = null
    ): GroupDto

    @POST("/api/v1/groups/{groupId}/controller")
    suspend fun transferController(
        @Path("groupId") groupId: String,
        @Body body: TransferControllerRequest
    ): GroupDto
}
```

### 12.7 GroupMessageApi

```kotlin
interface GroupMessageApi {
    @GET("/api/v1/groups/{groupId}/messages")
    suspend fun getMessages(
        @Path("groupId") groupId: String
    ): List<GroupMessageDto>

    @POST("/api/v1/groups/{groupId}/messages")
    suspend fun sendTextMessage(
        @Path("groupId") groupId: String,
        @Body body: SendGroupTextMessageRequest
    ): GroupMessageDto

    @Multipart
    @POST("/api/v1/groups/{groupId}/files")
    suspend fun sendFileMessage(
        @Path("groupId") groupId: String,
        @Part file: MultipartBody.Part
    ): GroupMessageDto

    @Streaming
    @GET("/api/v1/groups/{groupId}/messages/{msgId}/file")
    suspend fun downloadFile(
        @Path("groupId") groupId: String,
        @Path("msgId") msgId: String
    ): ResponseBody

    @POST("/api/v1/groups/{groupId}/messages/{msgId}/revoke")
    suspend fun revokeMessage(
        @Path("groupId") groupId: String,
        @Path("msgId") msgId: String
    ): GroupRevokeResponseDto

    @POST("/api/v1/groups/{groupId}/sync")
    suspend fun syncGroup(
        @Path("groupId") groupId: String,
        @Body body: SyncGroupRequest
    ): SyncGroupResponseDto
}
```

---

## 13. 文件发送与下载规则

这里必须严格按 meshproxy 实际协议做，不要再设计成“先上传、再发消息”的通用聊天方案。文档已经明确了 direct/group 都是**直接发送文件消息接口**。([GitHub][1])

### 13.1 单聊发文件

调用：

`POST /api/v1/chat/conversations/{conversation_id}/files`

* `multipart/form-data`
* 字段名固定：`file`
* 返回：单条 `Message`

### 13.2 群聊发文件

调用：

`POST /api/v1/groups/{group_id}/files`

* `multipart/form-data`
* 字段名固定：`file`
* 返回：单条 `GroupMessage`

### 13.3 下载文件

单聊：

`GET /api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/file`

群聊：

`GET /api/v1/groups/{group_id}/messages/{msg_id}/file`

### 13.4 客户端表现

客户端仍然应该做 optimistic UI：

1. 先本地插入一条发送中消息
2. 调文件发送接口
3. 成功后用服务端返回消息覆盖本地发送中消息
4. 失败则更新状态为 failed，并支持重试

---

## 14. WebSocket 设计

### 14.1 ChatEventDto

字段来自 `ChatEvent` 定义。([GitHub][1])

```kotlin
data class ChatEventDto(
    val type: String,
    val kind: String?,
    val conversationId: String?,
    val msgId: String?,
    val msgType: String?,
    val requestId: String?,
    val fromPeerId: String?,
    val toPeerId: String?,
    val state: String?,
    val atUnixMillis: Long?,
    val plaintext: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val senderPeerId: String?,
    val receiverPeerId: String?,
    val direction: String?,
    val counter: Long?,
    val transportMode: String?,
    val messageState: String?,
    val createdAtUnixMillis: Long?,
    val deliveredAtUnixMillis: Long?,
    val epoch: Long?,
    val senderSeq: Long?,
    val deliverySummary: GroupDeliverySummaryDto?
)
```

### 14.2 WsManager 要求

实现一个统一 WebSocket 管理器：

* 建立连接
* 维护连接状态
* 自动重连
* 心跳
* 事件 JSON 解析
* 事件落库
* 对外暴露 `Flow<ConnectionState>`

### 14.3 事件处理策略

根据文档说明，建议如下：([GitHub][1])

* `type=message`

  * 若消息字段完整，则直接转 direct/group message 落库
  * 若消息字段不完整，则触发对应会话 HTTP 刷新
* `type=message_state`

  * 更新 direct/group message 状态
* `type=friend_request`

  * 刷新好友请求列表
* `type=conversation_deleted`

  * 删除本地 direct conversation
* `type=contact_deleted`

  * 删除 contact，并联动 direct conversation 展示信息

### 14.4 连接状态

```kotlin
enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    FAILED
}
```

---

## 15. 页面要求

### 15.1 启动页

功能：

* 读取 base URL 配置
* 初始化 Room
* 初始化 Retrofit / WS
* 尝试建立 WebSocket
* 进入首页

### 15.2 首页

底部 Tab 至少 3 个：

* 聊天
* 联系人
* 我

也可预留第 4 个：

* 发现（占位）

### 15.3 聊天列表页

显示：

* 头像
* 名称
* 最后一条消息摘要
* 时间
* 未读数
* 连接状态提示（可顶部 banner）

### 15.4 联系人页

显示：

* 好友请求入口
* 联系人列表
* 群组列表入口

### 15.5 群组页

显示：

* 群列表
* 群详情入口
* 成员数
* 最近消息时间

### 15.6 聊天页

必须支持：

* 单聊
* 群聊
* 文本发送
* 文件发送
* 图片预览
* 视频播放
* 音频播放
* 普通文件下载

群聊中，对方消息要显示昵称和头像。昵称优先来自本地缓存资料；如果无资料，显示 peerId 缩略形式。

---

## 16. 聊天气泡与媒体要求

### 16.1 文本

* 普通气泡
* 长按复制
* 发送失败可重试

### 16.2 图片

* 消息列表显示缩略图
* 点击进入全屏预览
* 优先使用本地缓存
* 若无本地缓存则临时下载

### 16.3 视频

* 显示预览卡片
* 点击进入 Media3 播放页
* 可以先走“下载后播放”的稳定方案

### 16.4 音频

* 气泡显示时长
* 点击播放 / 暂停
* 同时只能播放一条
* 离开页面停止播放

### 16.5 普通文件

* 显示文件名、大小、MIME
* 点击下载
* 下载完成后支持打开 / 分享

---

## 17. 本地文件存储策略

实现 `FileStorageManager`，负责：

* 临时缓存目录
* 文件命名
* 冲突处理
* 已下载文件查找
* MIME 与扩展名判断

建议：

* 图片 / 音频 / 视频：放 app 私有缓存目录
* 普通文件：先下载到 app 私有目录，再允许外部分享
* 数据库中记录 `localPath`

---

## 18. Repository 层要求

至少实现：

* `ProfileRepository`
* `FriendRequestRepository`
* `ContactRepository`
* `DirectConversationRepository`
* `DirectMessageRepository`
* `GroupRepository`
* `GroupMessageRepository`
* `RealtimeRepository`
* `FileRepository`

并且做到：

* HTTP 拉取后写 DB
* WS 事件写 DB
* ViewModel 只读 Repository 暴露的 Flow / suspend API

---

## 19. UseCase 建议

至少包括：

* `GetMeUseCase`
* `ObserveContactsUseCase`
* `ObserveChatListUseCase`
* `ObserveDirectMessagesUseCase`
* `ObserveGroupMessagesUseCase`
* `LoadMoreDirectMessagesUseCase`
* `SendDirectTextMessageUseCase`
* `SendDirectFileMessageUseCase`
* `SendGroupTextMessageUseCase`
* `SendGroupFileMessageUseCase`
* `DownloadMessageFileUseCase`
* `ObserveConnectionStateUseCase`
* `RetryFailedMessageUseCase`

---

## 20. ViewModel 建议

至少包括：

* `SplashViewModel`
* `ChatListViewModel`
* `ContactsViewModel`
* `FriendRequestsViewModel`
* `GroupsViewModel`
* `DirectChatRoomViewModel`
* `GroupChatRoomViewModel`
* `SettingsViewModel`

不建议把 direct / group 聊天页强行共用一个过于复杂的 ViewModel。UI 可共用组件，但状态管理建议分开。

---

## 21. 开发顺序

### Phase 1：工程骨架

* Compose 工程
* Hilt
* Navigation
* 主题
* 首页框架
* 假数据页面

### Phase 2：本地数据库

* Room entities
* DAO
* Repository skeleton
* Flow 观察列表

### Phase 3：Direct Chat HTTP 接入

* contacts
* conversations
* direct messages
* send text
* send file
* download file

### Phase 4：Group HTTP 接入

* groups
* group details
* group messages
* group file messages

### Phase 5：WebSocket

* WsManager
* 事件解析
* 自动重连
* 事件落库
* UI 自动刷新

### Phase 6：媒体

* 图片预览
* 视频播放
* 音频播放
* 文件下载管理

### Phase 7：打磨

* 错误处理
* 重试
* 空状态
* loading
* 缓存清理
* URL 配置页

---

## 22. 错误处理要求

必须处理：

* HTTP 404 / 500
* chat service not available
* JSON 解析失败
* direct messages 返回结构歧义
* WebSocket 断线
* 文件发送失败
* 文件下载失败
* Media3 播放失败
* 数据库异常
* base URL 非法

文档说明聊天服务未启用时可能返回 404 和纯文本 `chat service not available`，客户端应专门兼容。([GitHub][1])

---

## 23. 设置页要求

至少提供：

* HTTP Base URL
* WebSocket Base URL
* 当前连接状态
* 清理缓存
* 开发日志开关
* 关于页

Base URL 不要写死。
默认值可以是：

* HTTP：`http://127.0.0.1:19080`
* WS：`ws://127.0.0.1:19080/api/v1/chat/ws`

默认 HTTP 基底和 WS 路径来自文档。([GitHub][1])

---

## 24. Codex 实现约束

请严格遵守：

1. 先输出目录树
2. 再生成 `build.gradle.kts` 与依赖
3. 再生成 DTO / Entity / Dao / Mapper / Repository
4. 再生成页面和 ViewModel
5. 每一步都保持项目可编译
6. 不要把 DTO 直接传进 Compose
7. 不要把 direct / group 混成一个巨型 if-else 文件
8. direct messages 的分页一定使用 `limit + offset`
9. 文件发送接口必须直接调用 `/files`
10. 图片 / 视频 / 音频渲染必须根据 `mime_type` 判断
11. WebSocket 事件类型只能按文档支持的几种实现，不要自造 `message.new` 之类命名

---


---

## 26. 额外实现建议

### 26.1 要在客户端预先计算 conversation_id

文档给了 `conversation_id` 的稳定生成规则，请按规则生成。

### 26.2 群邀请通知先按 SYSTEM 处理

文档里群邀请通知是 `group_invite_notice`，结构比普通消息复杂很多，包含单聊密文和嵌套 envelope。第一版客户端先把它当系统消息展示，不做深度解析。([GitHub][1])

### 26.3 direct / group 聊天页可以共用 UI 组件，不共用底层状态机

UI 气泡、输入框、附件选择器可以共用；但数据加载、发送、状态刷新、下载逻辑建议分 direct 和 group 两套 ViewModel。

