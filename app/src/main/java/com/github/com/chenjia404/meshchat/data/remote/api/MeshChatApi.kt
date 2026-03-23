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
import com.github.com.chenjia404.meshchat.data.remote.dto.ProfileDto
import com.github.com.chenjia404.meshchat.data.remote.dto.RawJsonDto
import com.github.com.chenjia404.meshchat.data.remote.dto.RetentionBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SendFriendRequestBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SendTextBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.SimpleStatusDto
import com.github.com.chenjia404.meshchat.data.remote.dto.UpdateProfileBodyDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MeshChatApi {
    @GET("/api/v1/chat/me")
    suspend fun getMe(): ProfileDto

    @GET("/api/v1/chat/profile")
    suspend fun getProfile(): ProfileDto

    @POST("/api/v1/chat/profile")
    suspend fun updateProfile(@Body body: UpdateProfileBodyDto): ProfileDto

    @Multipart
    @POST("/api/v1/chat/profile/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): ProfileDto

    @GET("/api/v1/chat/avatars/{name}")
    suspend fun getAvatar(@Path("name") name: String): ResponseBody

    @GET("/api/v1/chat/requests")
    suspend fun getRequests(): List<FriendRequestDto>

    @POST("/api/v1/chat/requests")
    suspend fun sendRequest(@Body body: SendFriendRequestBodyDto): FriendRequestDto

    @POST("/api/v1/chat/requests/{request_id}/accept")
    suspend fun acceptRequest(@Path("request_id") requestId: String): DirectConversationDto

    @POST("/api/v1/chat/requests/{request_id}/reject")
    suspend fun rejectRequest(@Path("request_id") requestId: String): SimpleStatusDto

    @GET("/api/v1/chat/contacts")
    suspend fun getContacts(): List<ContactDto>

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
    suspend fun getConversations(): List<DirectConversationDto>

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

    /** 服务端可能返回空 body，Retrofit 会得到 null，故声明为可空 */
    @GET("/api/v1/groups")
    suspend fun getGroups(): List<GroupDto>?

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
    suspend fun getGroupMessages(@Path("group_id") groupId: String): List<GroupMessageDto>

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
}

