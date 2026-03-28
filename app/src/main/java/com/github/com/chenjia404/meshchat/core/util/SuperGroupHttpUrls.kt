package com.github.com.chenjia404.meshchat.core.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * 由「邀请链接」解析出的超级群 API 根地址（如 `https://chat-api.example.com/`），
 * 与设置里的 meshproxy 地址可以不同。
 */
fun HttpUrl.superGroupApiRoot(): HttpUrl =
    newBuilder()
        .encodedPath("/")
        .encodedQuery(null)
        .encodedFragment(null)
        .build()

/** 将持久化的字符串还原为 [HttpUrl]（须带末尾 `/` 以便 [HttpUrl.resolve]） */
fun String.toSuperGroupBaseHttpUrl(): HttpUrl {
    val t = trim().trimEnd('/')
    return "$t/".toHttpUrl()
}

/** 与 [toSuperGroupBaseHttpUrl] 字符串形式一致，便于 DataStore / Room 比较 */
fun normalizeMeshChatServerBaseUrl(s: String): String {
    val t = s.trim().trimEnd('/')
    return "$t/"
}

fun HttpUrl.joinSuperGroup(groupId: String): HttpUrl =
    resolve("groups/$groupId/join") ?: error("bad_super_group_base")

/** `PATCH /users/{peer_id}/profile`，[peer_id] 经 [HttpUrl.Builder.addPathSegment] 编码 */
fun HttpUrl.userProfileByPeerId(peerId: String): HttpUrl =
    superGroupApiRoot().newBuilder()
        .addPathSegment("users")
        .addPathSegment(peerId)
        .addPathSegment("profile")
        .build()

/** [GET /me/groups](https://github.com/chenjia404/meshchat-server/blob/master/docs/API.md) */
fun HttpUrl.meGroups(limit: Int?, offset: Int?): HttpUrl {
    val b = superGroupApiRoot().newBuilder()
        .addPathSegment("me")
        .addPathSegment("groups")
    if (limit != null) b.addQueryParameter("limit", limit.toString())
    if (offset != null) b.addQueryParameter("offset", offset.toString())
    return b.build()
}

fun HttpUrl.superGroupDetail(groupId: String): HttpUrl =
    resolve("groups/$groupId") ?: error("bad_super_group_base")

/** `POST /groups/{group_id}/leave`，无请求体 */
fun HttpUrl.superGroupLeave(groupId: String): HttpUrl =
    resolve("groups/$groupId/leave") ?: error("bad_super_group_base")

fun HttpUrl.superGroupMessages(groupId: String, beforeSeq: Long?, limit: Int?): HttpUrl {
    val base = resolve("groups/$groupId/messages") ?: error("bad_super_group_base")
    val b = base.newBuilder()
    if (beforeSeq != null) b.addQueryParameter("before_seq", beforeSeq.toString())
    if (limit != null) b.addQueryParameter("limit", limit.toString())
    return b.build()
}

fun HttpUrl.superGroupPostMessage(groupId: String): HttpUrl =
    resolve("groups/$groupId/messages") ?: error("bad_super_group_base")

fun HttpUrl.superGroupRetract(groupId: String, messageId: String): HttpUrl =
    resolve("groups/$groupId/messages/$messageId/retract") ?: error("bad_super_group_base")

fun HttpUrl.superGroupRegisterFile(): HttpUrl =
    resolve("files") ?: error("bad_super_group_base")

fun HttpUrl.superGroupIpfsAdd(): HttpUrl =
    resolve("api/ipfs/add") ?: error("bad_super_group_base")

fun HttpUrl.superGroupMembersList(groupId: String): HttpUrl =
    resolve("groups/$groupId/members") ?: error("bad_super_group_base")

/** `POST /groups/{group_id}/members/invite`，body：`{ "peer_ids": [...] }` */
fun HttpUrl.superGroupMembersInvite(groupId: String): HttpUrl =
    resolve("groups/$groupId/members/invite") ?: error("bad_super_group_base")
