package com.github.com.chenjia404.meshchat.data.remote.api

import com.github.com.chenjia404.meshchat.data.remote.dto.IpfsAddResponseDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerGroupDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerGroupMemberDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerInviteMembersBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerPatchUserProfileBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerPostMessageBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerRegisterFileBodyDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerRegisterFileResponseDto
import com.github.com.chenjia404.meshchat.data.remote.dto.MeshChatServerUserDto
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

/**
 * 直连公网 meshchat-server（完整 [@Url]，不经 meshproxy host 重写）。
 * 登录：`MeshChatServerSessionManager` 内 OkHttp（与 Quark 一致）；其余请求带 Bearer。
 */
interface MeshChatServerDirectApi {
    @POST
    suspend fun postJoinGroup(@Url url: String): MeshChatServerGroupMemberDto

    @POST
    suspend fun postLeaveGroup(@Url url: String): MeshChatServerGroupMemberDto

    @GET
    suspend fun getGroup(@Url url: String): MeshChatServerGroupDto

    /** [GET /me/groups](https://github.com/chenjia404/meshchat-server/blob/master/docs/API.md) 返回当前用户 active 群列表 */
    @GET
    suspend fun getMeGroups(@Url url: String): List<MeshChatServerGroupDto>?

    /** [PATCH /groups/{group_id}](https://github.com/chenjia404/meshchat-server/blob/master/docs/API.md) */
    @PATCH
    suspend fun patchGroup(
        @Url url: String,
        @Body body: JsonObject,
    ): MeshChatServerGroupDto

    /** [PATCH /users/{peer_id}/profile](https://github.com/chenjia404/meshchat-server/blob/master/docs/API.md) */
    @PATCH
    suspend fun patchUserProfileByPeerId(
        @Url url: String,
        @Body body: MeshChatServerPatchUserProfileBodyDto,
    ): MeshChatServerUserDto

    @GET
    suspend fun getGroupMembers(@Url url: String): List<MeshChatServerGroupMemberDto>?

    @POST
    suspend fun postInviteMembers(
        @Url url: String,
        @Body body: MeshChatServerInviteMembersBodyDto,
    ): List<MeshChatServerGroupMemberDto>?

    @GET
    suspend fun getMessages(@Url url: String): List<MeshChatServerMessageDto>?

    @POST
    suspend fun postMessage(
        @Url url: String,
        @Body body: MeshChatServerPostMessageBodyDto,
    ): MeshChatServerMessageDto

    @POST
    suspend fun postRetract(@Url url: String): MeshChatServerMessageDto

    @POST
    suspend fun postRegisterFile(
        @Url url: String,
        @Body body: MeshChatServerRegisterFileBodyDto,
    ): MeshChatServerRegisterFileResponseDto

    @Multipart
    @POST
    suspend fun postIpfsAdd(@Url url: String, @Part file: MultipartBody.Part): IpfsAddResponseDto
}
