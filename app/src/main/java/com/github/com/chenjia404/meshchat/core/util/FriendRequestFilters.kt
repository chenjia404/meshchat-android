package com.github.com.chenjia404.meshchat.core.util

import com.github.com.chenjia404.meshchat.domain.model.FriendRequest

/**
 * 联系人页「好友请求」列表：
 * - 不展示**我发出的**请求（[FriendRequest.fromPeerId] 为本人）
 * - 不展示**已处理结束**的请求（已接受、已拒绝等）
 * - 仅展示**发给我且仍待处理**的请求（[FriendRequest.toPeerId] 为本人）
 */
fun FriendRequest.shouldShowInIncomingPendingList(myPeerId: String?): Boolean {
    if (myPeerId.isNullOrBlank()) return false
    if (fromPeerId == myPeerId) return false
    if (toPeerId != myPeerId) return false
    if (state.isTerminalFriendRequestState()) return false
    return true
}

private fun String.isTerminalFriendRequestState(): Boolean {
    return when (trim().lowercase()) {
        "accepted",
        "rejected",
        "cancelled",
        "declined",
        "expired",
        -> true
        else -> false
    }
}
