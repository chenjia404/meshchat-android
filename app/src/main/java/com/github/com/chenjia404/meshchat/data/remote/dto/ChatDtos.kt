package com.github.com.chenjia404.meshchat.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ProfileDto(
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("avatar_cid") val avatarCid: String?,
    @SerializedName("chat_kex_pub") val chatKexPub: String?,
    @SerializedName("created_at") val createdAt: String,
)

data class UpdateProfileBodyDto(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("bio") val bio: String,
)

data class FriendRequestDto(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("from_peer_id") val fromPeerId: String,
    @SerializedName("to_peer_id") val toPeerId: String,
    @SerializedName("state") val state: String,
    @SerializedName("intro_text") val introText: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("retention_minutes") val retentionMinutes: Int?,
    @SerializedName("remote_chat_kex_pub") val remoteChatKexPub: String?,
    @SerializedName("conversation_id") val conversationId: String?,
    @SerializedName("last_transport_mode") val lastTransportMode: String?,
    @SerializedName("retry_count") val retryCount: Int?,
    @SerializedName("next_retry_at") val nextRetryAt: String?,
    @SerializedName("retry_job_status") val retryJobStatus: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
)

data class SendFriendRequestBodyDto(
    @SerializedName("to_peer_id") val toPeerId: String,
    @SerializedName("intro_text") val introText: String,
)

data class ContactDto(
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("cid") val cid: String?,
    @SerializedName("remote_nickname") val remoteNickname: String?,
    @SerializedName("retention_minutes") val retentionMinutes: Int?,
    @SerializedName("blocked") val blocked: Boolean?,
    @SerializedName("last_seen_at") val lastSeenAt: String?,
    @SerializedName("updated_at") val updatedAt: String,
)

data class ContactNicknameBodyDto(
    @SerializedName("nickname") val nickname: String,
)

data class ContactBlockBodyDto(
    @SerializedName("blocked") val blocked: Boolean,
)

data class DirectConversationDto(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("state") val state: String,
    @SerializedName("last_message_at") val lastMessageAt: String?,
    @SerializedName("last_transport_mode") val lastTransportMode: String?,
    @SerializedName("unread_count") val unreadCount: Int?,
    @SerializedName("retention_minutes") val retentionMinutes: Int?,
    @SerializedName("retention_sync_state") val retentionSyncState: String?,
    @SerializedName("retention_synced_at") val retentionSyncedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
)

data class DirectMessageDto(
    @SerializedName("msg_id") val msgId: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("sender_peer_id") val senderPeerId: String,
    @SerializedName("receiver_peer_id") val receiverPeerId: String,
    @SerializedName("direction") val direction: String,
    @SerializedName("msg_type") val msgType: String,
    @SerializedName("plaintext") val plaintext: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("file_cid") val fileCid: String?,
    @SerializedName("transport_mode") val transportMode: String?,
    @SerializedName("state") val state: String,
    @SerializedName("counter") val counter: Long?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("delivered_at") val deliveredAt: String?,
)

data class DirectMessagesPageDto(
    @SerializedName("messages") val messages: List<DirectMessageDto>?,
    @SerializedName("total") val total: Int?,
    @SerializedName("limit") val limit: Int?,
    @SerializedName("offset") val offset: Int?,
    @SerializedName("has_more") val hasMore: Boolean?,
)

data class SendTextBodyDto(
    @SerializedName("text") val text: String,
)

data class RetentionBodyDto(
    @SerializedName("retention_minutes") val retentionMinutes: Int,
)

data class GroupDto(
    @SerializedName("group_id") val groupId: String,
    @SerializedName("title") val title: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("controller_peer_id") val controllerPeerId: String,
    @SerializedName("current_epoch") val currentEpoch: Long?,
    @SerializedName("retention_minutes") val retentionMinutes: Int?,
    @SerializedName("state") val state: String,
    @SerializedName("last_event_seq") val lastEventSeq: Long?,
    @SerializedName("last_message_at") val lastMessageAt: String?,
    @SerializedName("member_count") val memberCount: Int?,
    @SerializedName("local_member_role") val localMemberRole: String?,
    @SerializedName("local_member_state") val localMemberState: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
)

data class CreateGroupBodyDto(
    @SerializedName("title") val title: String,
    @SerializedName("members") val members: List<String>,
)

data class GroupMemberDto(
    @SerializedName("group_id") val groupId: String,
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("role") val role: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("invited_by") val invitedBy: String?,
    @SerializedName("joined_epoch") val joinedEpoch: Long?,
    @SerializedName("left_epoch") val leftEpoch: Long?,
    @SerializedName("updated_at") val updatedAt: String?,
)

data class GroupEventDeliverySummaryDto(
    @SerializedName("total") val total: Int?,
    @SerializedName("sent_to_transport") val sentToTransport: Int?,
    @SerializedName("queued_for_retry") val queuedForRetry: Int?,
    @SerializedName("failed") val failed: Int?,
)

data class GroupEventViewDto(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("group_id") val groupId: String,
    @SerializedName("event_seq") val eventSeq: Long?,
    @SerializedName("event_type") val eventType: String?,
    @SerializedName("actor_peer_id") val actorPeerId: String?,
    @SerializedName("signer_peer_id") val signerPeerId: String?,
    @SerializedName("payload_json") val payloadJson: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("delivery_summary") val deliverySummary: GroupEventDeliverySummaryDto?,
)

data class GroupDetailsDto(
    @SerializedName("group") val group: GroupDto,
    @SerializedName("members") val members: List<GroupMemberDto>?,
    @SerializedName("recent_events") val recentEvents: List<GroupEventViewDto>?,
)

data class GroupDeliverySummaryDto(
    @SerializedName("total") val total: Int?,
    @SerializedName("pending") val pending: Int?,
    @SerializedName("sent_to_transport") val sentToTransport: Int?,
    @SerializedName("queued_for_retry") val queuedForRetry: Int?,
    @SerializedName("delivered_remote") val deliveredRemote: Int?,
    @SerializedName("failed") val failed: Int?,
)

data class GroupMessageDto(
    @SerializedName("msg_id") val msgId: String,
    @SerializedName("group_id") val groupId: String,
    @SerializedName("epoch") val epoch: Long?,
    @SerializedName("sender_peer_id") val senderPeerId: String,
    @SerializedName("sender_seq") val senderSeq: Long?,
    @SerializedName("msg_type") val msgType: String,
    @SerializedName("plaintext") val plaintext: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("file_cid") val fileCid: String?,
    @SerializedName("signature") val signature: String?,
    @SerializedName("state") val state: String,
    @SerializedName("delivery_summary") val deliverySummary: GroupDeliverySummaryDto?,
    @SerializedName("created_at") val createdAt: String,
)

data class GroupInviteBodyDto(
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("role") val role: String,
    @SerializedName("invite_text") val inviteText: String,
)

data class GroupReasonBodyDto(
    @SerializedName("reason") val reason: String,
)

data class GroupRemoveBodyDto(
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("reason") val reason: String,
)

data class GroupTitleBodyDto(
    @SerializedName("title") val title: String,
)

data class GroupControllerBodyDto(
    @SerializedName("peer_id") val peerId: String,
)

data class GroupSyncBodyDto(
    @SerializedName("from_peer_id") val fromPeerId: String,
)

data class SimpleStatusDto(
    @SerializedName("status") val status: String?,
    @SerializedName("ok") val ok: Boolean?,
    @SerializedName("peer_id") val peerId: String?,
    @SerializedName("conversation_id") val conversationId: String?,
    @SerializedName("group_id") val groupId: String?,
    @SerializedName("from_peer_id") val fromPeerId: String?,
)

data class ChatEventDto(
    @SerializedName("type") val type: String,
    @SerializedName("kind") val kind: String?,
    @SerializedName("conversation_id") val conversationId: String?,
    @SerializedName("msg_id") val msgId: String?,
    @SerializedName("msg_type") val msgType: String?,
    @SerializedName("request_id") val requestId: String?,
    @SerializedName("from_peer_id") val fromPeerId: String?,
    @SerializedName("to_peer_id") val toPeerId: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("at_unix_millis") val atUnixMillis: Long?,
    @SerializedName("plaintext") val plaintext: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("sender_peer_id") val senderPeerId: String?,
    @SerializedName("receiver_peer_id") val receiverPeerId: String?,
    @SerializedName("direction") val direction: String?,
    @SerializedName("counter") val counter: Long?,
    @SerializedName("transport_mode") val transportMode: String?,
    @SerializedName("message_state") val messageState: String?,
    @SerializedName("created_at_unix_millis") val createdAtUnixMillis: Long?,
    @SerializedName("delivered_at_unix_millis") val deliveredAtUnixMillis: Long?,
    @SerializedName("epoch") val epoch: Long?,
    @SerializedName("sender_seq") val senderSeq: Long?,
    @SerializedName("delivery_summary") val deliverySummary: GroupDeliverySummaryDto?,
)

typealias RawJsonDto = JsonElement
