package com.github.com.chenjia404.meshchat.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** meshchat-server [User](https://github.com/chenjia404/meshchat-server/blob/master/docs/API.md) */
data class MeshChatServerUserDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("avatar_cid") val avatarCid: String?,
)

/** meshchat-server Group */
data class MeshChatServerGroupDto(
    @SerializedName("group_id") val groupId: String,
    @SerializedName("title") val title: String,
    @SerializedName("about") val about: String?,
    @SerializedName("avatar_cid") val avatarCid: String?,
    @SerializedName("member_list_visibility") val memberListVisibility: String?,
    @SerializedName("join_mode") val joinMode: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("last_message_seq") val lastMessageSeq: Long?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

/** `POST /groups/{group_id}/join` 返回成员 */
data class MeshChatServerGroupMemberDto(
    @SerializedName("user") val user: MeshChatServerUserDto?,
    @SerializedName("role") val role: String?,
    @SerializedName("status") val status: String?,
)

data class MeshChatServerMessageDto(
    @SerializedName("group_id") val groupId: String,
    @SerializedName("message_id") val messageId: String,
    @SerializedName("seq") val seq: Long?,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("payload") val payload: JsonElement?,
    @SerializedName("sender") val sender: MeshChatServerUserDto?,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?,
)

data class MeshChatServerPostMessageBodyDto(
    @SerializedName("content_type") val contentType: String,
    @SerializedName("payload") val payload: JsonElement,
    @SerializedName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerializedName("forward_from_message_id") val forwardFromMessageId: String? = null,
    @SerializedName("signature") val signature: String = "",
)

data class MeshChatServerRegisterFileBodyDto(
    @SerializedName("cid") val cid: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("size") val size: Long,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("duration_seconds") val durationSeconds: Int? = null,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("thumbnail_cid") val thumbnailCid: String = "",
)

data class MeshChatServerRegisterFileResponseDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("cid") val cid: String?,
)

data class IpfsAddResponseDto(
    @SerializedName("cid") val cid: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("pinned") val pinned: Boolean?,
)

/** `POST /groups/{group_id}/members/invite` */
data class MeshChatServerInviteMembersBodyDto(
    @SerializedName("peer_ids") val peerIds: List<String>,
)

/** `PATCH /users/{peer_id}/profile` */
data class MeshChatServerPatchUserProfileBodyDto(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_cid") val avatarCid: String?,
    @SerializedName("bio") val bio: String,
    @SerializedName("status") val status: String,
)
