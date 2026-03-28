package com.github.com.chenjia404.meshchat.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MeshChatServerAuthChallengeRequestDto(
    @SerializedName("peer_id") val peerId: String,
)

data class MeshChatServerAuthChallengeResponseDto(
    @SerializedName("challenge_id") val challengeId: String,
    @SerializedName("challenge") val challenge: String,
    @SerializedName("expires_at") val expiresAt: String?,
)

data class MeshChatServerAuthLoginRequestDto(
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("challenge_id") val challengeId: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("public_key") val publicKey: String,
)

data class MeshChatServerAuthLoginResponseDto(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: MeshChatServerUserDto?,
)
