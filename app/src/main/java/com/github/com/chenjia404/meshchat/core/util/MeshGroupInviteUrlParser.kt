package com.github.com.chenjia404.meshchat.core.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val GROUP_ID_UUID = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
)

/**
 * 解析 meshchat-server 风格的群邀请链接：`{base}/groups/{group_id}`，
 * 也接受路径中含 `/api/v1/groups/{group_id}` 的形式。
 *
 * [serverBaseHttpUrl] 为链接中的 API 根（scheme+host+port），**不必**与设置里的 meshproxy 地址一致。
 */
data class ParsedSuperGroupInvite(
    val groupId: String,
    val serverBaseHttpUrl: HttpUrl,
)

fun parseSuperGroupInviteUrl(raw: String): ParsedSuperGroupInvite {
    val url = raw.trim().toHttpUrlOrNull() ?: throw IllegalArgumentException("invalid_url")
    val segments = url.pathSegments.filter { it.isNotEmpty() }
    val gi = segments.indexOf("groups")
    if (gi < 0 || gi >= segments.size - 1) {
        throw IllegalArgumentException("missing_groups_path")
    }
    val groupId = segments[gi + 1]
    if (!GROUP_ID_UUID.matches(groupId)) {
        throw IllegalArgumentException("invalid_group_id")
    }
    return ParsedSuperGroupInvite(
        groupId = groupId,
        serverBaseHttpUrl = url.superGroupApiRoot(),
    )
}
