package com.ucw.beatu.business.videofeed.domain.model

/**
 * 互动异步同步事件：
 * 用于把“本地乐观更新 -> 远程异步同步”过程中的失败信息回传给 UI，
 * 以便做提示与可见状态回滚。
 */
sealed class InteractionSyncEvent {
    data class LikeSyncFailed(
        val videoId: Long,
        val rollbackToLiked: Boolean,
        val message: String
    ) : InteractionSyncEvent()

    data class FavoriteSyncFailed(
        val videoId: Long,
        val rollbackToFavorited: Boolean,
        val message: String
    ) : InteractionSyncEvent()

    data class CommentSyncFailed(
        val videoId: Long,
        val tempCommentId: String,
        val message: String
    ) : InteractionSyncEvent()
}

