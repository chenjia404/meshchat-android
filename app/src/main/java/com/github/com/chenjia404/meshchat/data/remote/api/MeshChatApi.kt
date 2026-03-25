package com.github.com.chenjia404.meshchat.data.remote.api

import com.github.com.chenjia404.meshchat.data.remote.dto.ChatEventDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ContactBlockBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ContactDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ContactNicknameBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.CreateGroupBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.DirectConversationDto
import com.github.com.chenjia404.meshchat.data.remote.dto.DirectMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.FriendRequestDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupControllerBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupDetailsDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupInviteBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupReasonBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupRemoveBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupSyncBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupTitleBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.CreatePublicChannelBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ProfileDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelHeadDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelMessagesPageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelSubscribeBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelSubscribeResultDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelSummaryDto
import com.github.com.chenjia404.meshchat.data.remote.dto.PublicChannelUpsertMessageBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.RawJsonDto
import com.github.com.chenjia404.meshchat.data.remote.dto.RetentionBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SendFriendRequestBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SendTextBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SimpleStatusDto
import com.github.com.chenjia404.meshchat.data.remote.dto.UpdateProfileBodyDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface MeshChatApi {
    /** 响应体为空时 Gson 为 null，须可空，避免 Kotlin NPE */
    @GET("/api/v1/chat/me")
    suspend fun getMe(): ProfileDto?

    @GET("/api/v1/chat/profile")
    suspend fun getProfile(): ProfileDto?

    @POST("/api/v1/chat/profile")
    suspend fun updateProfile(@Body body: UpdateProfileBodyDto): ProfileDto

    @Multipart
    @POST("/api/v1/chat/profile/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): ProfileDto

    @GET("/api/v1/chat/avatars/{name}")
    suspend fun getAvatar(@Path("name") name: String): ResponseBody

    /** 无数据时服务端可能返回空 body，Retrofit 为 null，须可空 */
    @GET("/api/v1/chat/requests")
    suspend fun getRequests(): List<FriendRequestDto>?

    @POST("/api/v1/chat/requests")
    suspend fun sendRequest(@Body body: SendFriendRequestBodyDto): FriendRequestDto

    @POST("/api/v1/chat/requests/{request_id}/accept")
    suspend fun acceptRequest(@Path("request_id") requestId: String): DirectConversationDto

    @POST("/api/v1/chat/requests/{request_id}/reject")
    suspend fun rejectRequest(@Path("request_id") requestId: String): SimpleStatusDto

    @GET("/api/v1/chat/contacts")
    suspend fun getContacts(): List<ContactDto>?

    @DELETE("/api/v1/chat/contacts/{peer_id}")
    suspend fun deleteContact(@Path("peer_id") peerId: String): SimpleStatusDto

    @POST("/api/v1/chat/contacts/{peer_id}/nickname")
    suspend fun updateContactNickname(
        @Path("peer_id") peerId: String,
        @Body body: ContactNicknameBodyDto,
    ): ContactDto

    @POST("/api/v1/chat/contacts/{peer_id}/block")
    suspend fun setBlocked(
        @Path("peer_id") peerId: String,
        @Body body: ContactBlockBodyDto,
    ): ContactDto

    @GET("/api/v1/chat/conversations")
    suspend fun getConversations(): List<DirectConversationDto>?

    @DELETE("/api/v1/chat/conversations/{conversation_id}")
    suspend fun deleteConversation(@Path("conversation_id") conversationId: String): SimpleStatusDto

    @POST("/api/v1/chat/conversations/{conversation_id}/sync")
    suspend fun syncConversation(@Path("conversation_id") conversationId: String): SimpleStatusDto

    @POST("/api/v1/chat/conversations/{conversation_id}/read")
    suspend fun markConversationRead(@Path("conversation_id") conversationId: String): DirectConversationDto

    @GET("/api/v1/chat/conversations/{conversation_id}/messages")
    suspend fun getConversationMessages(
        @Path("conversation_id") conversationId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): RawJsonDto

    @POST("/api/v1/chat/conversations/{conversation_id}/messages")
    suspend fun sendConversationMessage(
        @Path("conversation_id") conversationId: String,
        @Body body: SendTextBodyDto,
    ): DirectMessageDto

    @Multipart
    @POST("/api/v1/chat/conversations/{conversation_id}/files")
    suspend fun sendConversationFile(
        @Path("conversation_id") conversationId: String,
        @Part file: MultipartBody.Part,
    ): DirectMessageDto

    @GET("/api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/file")
    suspend fun getConversationFile(
        @Path("conversation_id") conversationId: String,
        @Path("msg_id") msgId: String,
    ): ResponseBody

    @POST("/api/v1/chat/conversations/{conversation_id}/messages/{msg_id}/revoke")
    suspend fun revokeConversationMessage(
        @Path("conversation_id") conversationId: String,
        @Path("msg_id") msgId: String,
    ): SimpleStatusDto

    @POST("/api/v1/chat/conversations/{conversation_id}/retention")
    suspend fun updateConversationRetention(
        @Path("conversation_id") conversationId: String,
        @Body body: RetentionBodyDto,
    ): DirectConversationDto

    /**
     * 服务端可能返回空 body；使用 [Response] 避免 Retrofit Kotlin 挂起扩展在「可空 List」上仍抛 NPE。
     */
    @GET("/api/v1/groups")
    suspend fun getGroups(): Response<List<GroupDto>>

    @POST("/api/v1/groups")
    suspend fun createGroup(@Body body: CreateGroupBodyDto): GroupDto

    @GET("/api/v1/groups/{group_id}")
    suspend fun getGroup(@Path("group_id") groupId: String): GroupDetailsDto

    @POST("/api/v1/groups/{group_id}/invite")
    suspend fun invite(
        @Path("group_id") groupId: String,
        @Body body: GroupInviteBodyDto,
    ): GroupDto

    @POST("/api/v1/groups/{group_id}/join")
    suspend fun join(@Path("group_id") groupId: String): GroupDto

    @POST("/api/v1/groups/{group_id}/leave")
    suspend fun leave(
        @Path("group_id") groupId: String,
        @Body body: GroupReasonBodyDto,
    ): GroupDto

    @POST("/api/v1/groups/{group_id}/remove")
    suspend fun remove(
        @Path("group_id") groupId: String,
        @Body body: GroupRemoveBodyDto,
    ): GroupDto

    @POST("/api/v1/groups/{group_id}/title")
    suspend fun updateTitle(
        @Path("group_id") groupId: String,
        @Body body: GroupTitleBodyDto,
    ): GroupDto

    @POST("/api/v1/groups/{group_id}/retention")
    suspend fun updateGroupRetention(
        @Path("group_id") groupId: String,
        @Body body: RetentionBodyDto,
    ): GroupDto

    @POST("/api/v1/groups/{group_id}/dissolve")
    suspend fun dissolve(
        @Path("group_id") groupId: String,
        @Body body: GroupReasonBodyDto,
    ): GroupDto

    @POST("/api/v1/groups/{group_id}/controller")
    suspend fun changeController(
        @Path("group_id") groupId: String,
        @Body body: GroupControllerBodyDto,
    ): GroupDto

    @GET("/api/v1/groups/{group_id}/messages")
    suspend fun getGroupMessages(@Path("group_id") groupId: String): List<GroupMessageDto>?

    @POST("/api/v1/groups/{group_id}/messages")
    suspend fun sendGroupMessage(
        @Path("group_id") groupId: String,
        @Body body: SendTextBodyDto,
    ): GroupMessageDto

    @Multipart
    @POST("/api/v1/groups/{group_id}/files")
    suspend fun sendGroupFile(
        @Path("group_id") groupId: String,
        @Part file: MultipartBody.Part,
    ): GroupMessageDto

    @GET("/api/v1/groups/{group_id}/messages/{msg_id}/file")
    suspend fun getGroupFile(
        @Path("group_id") groupId: String,
        @Path("msg_id") msgId: String,
    ): ResponseBody

    @POST("/api/v1/groups/{group_id}/messages/{msg_id}/revoke")
    suspend fun revokeGroupMessage(
        @Path("group_id") groupId: String,
        @Path("msg_id") msgId: String,
    ): SimpleStatusDto

    @POST("/api/v1/groups/{group_id}/sync")
    suspend fun syncGroup(
        @Path("group_id") groupId: String,
        @Body body: GroupSyncBodyDto,
    ): SimpleStatusDto

    @GET("/api/v1/public-channels/subscriptions")
    suspend fun listPublicChannelSubscriptions(): List<PublicChannelSummaryDto>?

    @POST("/api/v1/public-channels")
    suspend fun createPublicChannel(@Body body: CreatePublicChannelBodyDto): PublicChannelSummaryDto

    @GET("/api/v1/public-channels/{channel_id}")
    suspend fun getPublicChannel(@Path("channel_id") channelId: String): PublicChannelSummaryDto

    /** 更新频道资料（名称、简介等）；与创建时 body 结构一致。 */
    @PUT("/api/v1/public-channels/{channel_id}")
    suspend fun updatePublicChannel(
        @Path("channel_id") channelId: String,
        @Body body: CreatePublicChannelBodyDto,
    ): PublicChannelSummaryDto

    @Multipart
    @POST("/api/v1/public-channels/{channel_id}/avatar")
    suspend fun uploadPublicChannelAvatar(
        @Path("channel_id") channelId: String,
        @Part avatar: MultipartBody.Part,
    ): PublicChannelSummaryDto

    @GET("/api/v1/public-channels/{channel_id}/head")
    suspend fun getPublicChannelHead(@Path("channel_id") channelId: String): PublicChannelHeadDto

    @POST("/api/v1/public-channels/{channel_id}/subscribe")
    suspend fun subscribePublicChannel(
        @Path("channel_id") channelId: String,
        @Body body: PublicChannelSubscribeBodyDto,
    ): PublicChannelSubscribeResultDto

    @POST("/api/v1/public-channels/{channel_id}/unsubscribe")
    suspend fun unsubscribePublicChannel(@Path("channel_id") channelId: String): SimpleStatusDto

    @GET("/api/v1/public-channels/{channel_id}/messages")
    suspend fun getPublicChannelMessages(
        @Path("channel_id") channelId: String,
        @Query("before_message_id") beforeMessageId: Long? = null,
        @Query("limit") limit: Int? = null,
    ): PublicChannelMessagesPageDto

    @POST("/api/v1/public-channels/{channel_id}/messages")
    suspend fun createPublicChannelMessage(
        @Path("channel_id") channelId: String,
        @Body body: PublicChannelUpsertMessageBodyDto,
    ): PublicChannelMessageDto

    @PUT("/api/v1/public-channels/{channel_id}/messages/{message_id}")
    suspend fun updatePublicChannelMessage(
        @Path("channel_id") channelId: String,
        @Path("message_id") messageId: Long,
        @Body body: PublicChannelUpsertMessageBodyDto,
    ): PublicChannelMessageDto

    @POST("/api/v1/public-channels/{channel_id}/messages/{message_id}/revoke")
    suspend fun revokePublicChannelMessage(
        @Path("channel_id") channelId: String,
        @Path("message_id") messageId: Long,
    ): PublicChannelMessageDto

    @Multipart
    @POST("/api/v1/public-channels/{channel_id}/messages/file")
    suspend fun createPublicChannelFileMessage(
        @Path("channel_id") channelId: String,
        @Part file: MultipartBody.Part,
        @Part("text") text: RequestBody,
        @Part("mime_type") mimeType: RequestBody,
    ): PublicChannelMessageDto
}

