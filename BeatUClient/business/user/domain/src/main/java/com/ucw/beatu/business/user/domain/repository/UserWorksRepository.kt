package com.ucw.beatu.business.user.domain.repository

import com.ucw.beatu.business.user.domain.model.UserWork
import kotlinx.coroutines.flow.Flow

/**
 * 用户作品仓储接口，复用视频流缓存数据
 */
interface UserWorksRepository {
    /**
     * 订阅指定用户的作品列表；若作者暂无缓存，则回落到首页 Top N 视频
     */
    fun observeUserWorks(userId: String, limit: Int = DEFAULT_LIMIT): Flow<List<UserWork>>

    companion object {
        const val DEFAULT_LIMIT = 30
    }
}

