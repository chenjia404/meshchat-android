package com.github.com.chenjia404.meshchat.data.mapper

import com.github.com.chenjia404.meshchat.data.local.entity.ChatEventEntity
import com.github.com.chenjia404.meshchat.data.local.entity.ContactEntity
import com.github.com.chenjia404.meshchat.data.local.entity.DirectConversationEntity
import com.github.com.chenjia404.meshchat.data.local.entity.DirectMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.FriendRequestEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupEntity
import com.github.com.chenjia404.meshchat.data.local.entity.GroupMessageEntity
import com.github.com.chenjia404.meshchat.data.local.entity.ProfileEntity
import com.github.com.chenjia404.meshchat.data.remote.dto.ChatEventDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ContactDto
import com.github.com.chenjia404.meshchat.data.remote.dto.DirectConversationDto
import com.github.com.chenjia404.meshchat.data.remote.dto.DirectMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.FriendRequestDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupDeliverySummaryDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupDto
import com.github.com.chenjia404.meshchat.data.remote.dto.GroupMessageDto
import com.github.com.chenjia404.meshchat.data.remote.dto.ProfileDto
import com.github.com.chenjia404.meshchat.domain.model.ChatEvent
import com.github.com.chenjia404.meshchat.domain.model.Contact
import com.github.com.chenjia404.meshchat.domain.model.DirectConversation
import com.github.com.chenjia404.meshchat.domain.model.DirectMessage
import com.github.com.chenjia404.meshchat.domain.model.FriendRequest
import com.github.com.chenjia404.meshchat.domain.model.Group
import com.github.com.chenjia404.meshchat.domain.model.GroupDeliverySummary
import com.github.com.chenjia404.meshchat.domain.model.GroupMessage
import com.github.com.chenjia404.meshchat.domain.model.Profile

fun ProfileDto.toDomain() = Profile(
    peerId = peerId,
    nickname = nickname,
    bio = bio,
    avatar = avatar,
    avatarCid = avatarCid,
    chatKexPub = chatKexPub,
    createdAt = createdAt,
)

fun Profile.toEntity() = ProfileEntity(peerId, nickname, bio, avatar, avatarCid, chatKexPub, createdAt)
fun ProfileEntity.toDomain() = Profile(peerId, nickname, bio, avatar, avatarCid, chatKexPub, createdAt)

fun FriendRequestDto.toDomain() = FriendRequest(
    requestId = requestId,
    fromPeerId = fromPeerId,
    toPeerId = toPeerId,
    state = state,
    introText = introText,
    nickname = nickname,
    bio = bio,
    avatar = avatar,
    retentionMinutes = retentionMinutes ?: 0,
    remoteChatKexPub = remoteChatKexPub,
    conversationId = conversationId,
    lastTransportMode = lastTransportMode,
    retryCount = retryCount,
    nextRetryAt = nextRetryAt,
    retryJobStatus = retryJobStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FriendRequest.toEntity() = FriendRequestEntity(
    requestId,
    fromPeerId,
    toPeerId,
    state,
    introText,
    nickname,
    bio,
    avatar,
    retentionMinutes,
    remoteChatKexPub,
    conversationId,
    lastTransportMode,
    retryCount,
    nextRetryAt,
    retryJobStatus,
    createdAt,
    updatedAt,
)

fun FriendRequestEntity.toDomain() = FriendRequest(
    requestId,
    fromPeerId,
    toPeerId,
    state,
    introText,
    nickname,
    bio,
    avatar,
    retentionMinutes,
    remoteChatKexPub,
    conversationId,
    lastTransportMode,
    retryCount,
    nextRetryAt,
    retryJobStatus,
    createdAt,
    updatedAt,
)

fun ContactDto.toDomain() = Contact(
    peerId = peerId,
    nickname = nickname,
    bio = bio,
    avatar = avatar,
    cid = cid,
    remoteNickname = remoteNickname,
    retentionMinutes = retentionMinutes ?: 0,
    blocked = blocked ?: false,
    lastSeenAt = lastSeenAt,
    updatedAt = updatedAt,
)

fun Contact.toEntity() = ContactEntity(peerId, nickname, bio, avatar, cid, remoteNickname, retentionMinutes, blocked, lastSeenAt, updatedAt)
fun ContactEntity.toDomain() = Contact(peerId, nickname, bio, avatar, cid, remoteNickname, retentionMinutes, blocked, lastSeenAt, updatedAt)

fun DirectConversationDto.toDomain() = DirectConversation(
    conversationId = conversationId,
    peerId = peerId,
    state = state,
    lastMessageAt = lastMessageAt,
    lastTransportMode = lastTransportMode,
    unreadCount = unreadCount ?: 0,
    retentionMinutes = retentionMinutes ?: 0,
    retentionSyncState = retentionSyncState,
    retentionSyncedAt = retentionSyncedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun DirectConversation.toEntity() = DirectConversationEntity(
    conversationId,
    peerId,
    state,
    lastMessageAt,
    lastTransportMode,
    unreadCount,
    retentionMinutes,
    retentionSyncState,
    retentionSyncedAt,
    createdAt,
    updatedAt,
)

fun DirectConversationEntity.toDomain() = DirectConversation(
    conversationId,
    peerId,
    state,
    lastMessageAt,
    lastTransportMode,
    unreadCount,
    retentionMinutes,
    retentionSyncState,
    retentionSyncedAt,
    createdAt,
    updatedAt,
)

fun DirectMessageDto.toDomain() = DirectMessage(
    msgId = msgId,
    conversationId = conversationId,
    senderPeerId = senderPeerId,
    receiverPeerId = receiverPeerId,
    direction = direction,
    msgType = msgType,
    plaintext = plaintext,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    fileCid = fileCid,
    transportMode = transportMode,
    state = state,
    counter = counter,
    createdAt = createdAt,
    deliveredAt = deliveredAt,
)

fun DirectMessage.toEntity() = DirectMessageEntity(
    msgId,
    conversationId,
    senderPeerId,
    receiverPeerId,
    direction,
    msgType,
    plaintext,
    fileName,
    mimeType,
    fileSize,
    fileCid,
    transportMode,
    state,
    counter,
    createdAt,
    deliveredAt,
)

fun DirectMessageEntity.toDomain() = DirectMessage(
    msgId,
    conversationId,
    senderPeerId,
    receiverPeerId,
    direction,
    msgType,
    plaintext,
    fileName,
    mimeType,
    fileSize,
    fileCid,
    transportMode,
    state,
    counter,
    createdAt,
    deliveredAt,
)

fun GroupDto.toDomain() = Group(
    groupId = groupId,
    title = title,
    avatar = avatar,
    controllerPeerId = controllerPeerId,
    currentEpoch = currentEpoch ?: 0L,
    retentionMinutes = retentionMinutes ?: 0,
    state = state,
    lastEventSeq = lastEventSeq ?: 0L,
    lastMessageAt = lastMessageAt,
    memberCount = memberCount,
    localMemberRole = localMemberRole,
    localMemberState = localMemberState,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Group.toEntity() = GroupEntity(
    groupId,
    title,
    avatar,
    controllerPeerId,
    currentEpoch,
    retentionMinutes,
    state,
    lastEventSeq,
    lastMessageAt,
    memberCount,
    localMemberRole,
    localMemberState,
    createdAt,
    updatedAt,
)

fun GroupEntity.toDomain() = Group(
    groupId,
    title,
    avatar,
    controllerPeerId,
    currentEpoch,
    retentionMinutes,
    state,
    lastEventSeq,
    lastMessageAt,
    memberCount,
    localMemberRole,
    localMemberState,
    createdAt,
    updatedAt,
)

fun GroupDeliverySummaryDto.toDomain() = GroupDeliverySummary(
    total = total ?: 0,
    pending = pending ?: 0,
    sentToTransport = sentToTransport ?: 0,
    queuedForRetry = queuedForRetry ?: 0,
    deliveredRemote = deliveredRemote ?: 0,
    failed = failed ?: 0,
)

fun GroupMessageDto.toDomain() = GroupMessage(
    msgId = msgId,
    groupId = groupId,
    epoch = epoch,
    senderPeerId = senderPeerId,
    senderSeq = senderSeq,
    msgType = msgType,
    plaintext = plaintext,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    fileCid = fileCid,
    signature = signature,
    state = state,
    deliverySummary = deliverySummary?.toDomain(),
    createdAt = createdAt,
)

fun GroupMessage.toEntity() = GroupMessageEntity(
    msgId,
    groupId,
    epoch,
    senderPeerId,
    senderSeq,
    msgType,
    plaintext,
    fileName,
    mimeType,
    fileSize,
    fileCid,
    signature,
    state,
    deliverySummary?.total,
    deliverySummary?.pending,
    deliverySummary?.sentToTransport,
    deliverySummary?.queuedForRetry,
    deliverySummary?.deliveredRemote,
    deliverySummary?.failed,
    createdAt,
)

fun GroupMessageEntity.toDomain() = GroupMessage(
    msgId,
    groupId,
    epoch,
    senderPeerId,
    senderSeq,
    msgType,
    plaintext,
    fileName,
    mimeType,
    fileSize,
    fileCid,
    signature,
    state,
    if (deliveryTotal == null && deliveryPending == null && deliverySentToTransport == null &&
        deliveryQueuedForRetry == null && deliveryDeliveredRemote == null && deliveryFailed == null
    ) {
        null
    } else {
        GroupDeliverySummary(
            total = deliveryTotal ?: 0,
            pending = deliveryPending ?: 0,
            sentToTransport = deliverySentToTransport ?: 0,
            queuedForRetry = deliveryQueuedForRetry ?: 0,
            deliveredRemote = deliveryDeliveredRemote ?: 0,
            failed = deliveryFailed ?: 0,
        )
    },
    createdAt,
)

fun ChatEventDto.toDomain() = ChatEvent(
    type = type,
    kind = kind,
    conversationId = conversationId,
    msgId = msgId,
    msgType = msgType,
    requestId = requestId,
    fromPeerId = fromPeerId,
    toPeerId = toPeerId,
    state = state,
    atUnixMillis = atUnixMillis ?: System.currentTimeMillis(),
    plaintext = plaintext,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    senderPeerId = senderPeerId,
    receiverPeerId = receiverPeerId,
    direction = direction,
    counter = counter,
    transportMode = transportMode,
    messageState = messageState,
    createdAtUnixMillis = createdAtUnixMillis,
    deliveredAtUnixMillis = deliveredAtUnixMillis,
    epoch = epoch,
    senderSeq = senderSeq,
    deliverySummary = deliverySummary?.toDomain(),
)

fun ChatEvent.toEntity() = ChatEventEntity(
    id = id,
    type = type,
    kind = kind,
    conversationId = conversationId,
    msgId = msgId,
    msgType = msgType,
    requestId = requestId,
    fromPeerId = fromPeerId,
    toPeerId = toPeerId,
    state = state,
    atUnixMillis = atUnixMillis,
    plaintext = plaintext,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    senderPeerId = senderPeerId,
    receiverPeerId = receiverPeerId,
    direction = direction,
    counter = counter,
    transportMode = transportMode,
    messageState = messageState,
    createdAtUnixMillis = createdAtUnixMillis,
    deliveredAtUnixMillis = deliveredAtUnixMillis,
    epoch = epoch,
    senderSeq = senderSeq,
    deliveryTotal = deliverySummary?.total,
    deliveryPending = deliverySummary?.pending,
    deliverySentToTransport = deliverySummary?.sentToTransport,
    deliveryQueuedForRetry = deliverySummary?.queuedForRetry,
    deliveryDeliveredRemote = deliverySummary?.deliveredRemote,
    deliveryFailed = deliverySummary?.failed,
)

fun ChatEventEntity.toDomain() = ChatEvent(
    id,
    type,
    kind,
    conversationId,
    msgId,
    msgType,
    requestId,
    fromPeerId,
    toPeerId,
    state,
    atUnixMillis,
    plaintext,
    fileName,
    mimeType,
    fileSize,
    senderPeerId,
    receiverPeerId,
    direction,
    counter,
    transportMode,
    messageState,
    createdAtUnixMillis,
    deliveredAtUnixMillis,
    epoch,
    senderSeq,
    if (deliveryTotal == null && deliveryPending == null && deliverySentToTransport == null &&
        deliveryQueuedForRetry == null && deliveryDeliveredRemote == null && deliveryFailed == null
    ) null else GroupDeliverySummary(
        total = deliveryTotal ?: 0,
        pending = deliveryPending ?: 0,
        sentToTransport = deliverySentToTransport ?: 0,
        queuedForRetry = deliveryQueuedForRetry ?: 0,
        deliveredRemote = deliveryDeliveredRemote ?: 0,
        failed = deliveryFailed ?: 0,
    ),
)

