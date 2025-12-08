package com.ucw.beatu.business.user.domain.repository

import com.ucw.beatu.business.user.domain.model.UserWork
import kotlinx.coroutines.flow.Flow

/**
 * 用户作品仓储接口，复用视频流缓存数据
 */
interface UserWorksRepository {
    /**
     * 订阅指定用户的作品列表（使用authorId查询）
     */
    fun observeUserWorks(authorId: String, limit: Int = DEFAULT_LIMIT): Flow<List<UserWork>>

    /**
     * 订阅收藏的视频列表（需要userId，JOIN user_interactions表）
     */
    fun observeFavoritedWorks(userId: String, limit: Int = DEFAULT_LIMIT): Flow<List<UserWork>>

    /**
     * 订阅点赞的视频列表（需要userId，JOIN user_interactions表）
     */
    fun observeLikedWorks(userId: String, limit: Int = DEFAULT_LIMIT): Flow<List<UserWork>>

    /**
     * 订阅播放历史的视频列表（需要userId，JOIN watch_history表）
     */
    fun observeHistoryWorks(userId: String, limit: Int = DEFAULT_LIMIT): Flow<List<UserWork>>

    companion object {
        const val DEFAULT_LIMIT = 30
    }
}

