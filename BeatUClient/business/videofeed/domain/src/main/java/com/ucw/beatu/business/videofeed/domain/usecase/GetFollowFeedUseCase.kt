package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取关注流 UseCase
 * 获取当前用户关注的创作者发布的视频（需登录）
 */
class GetFollowFeedUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(page: Int = 1, limit: Int = 20): Flow<AppResult<List<Video>>> {
        return repository.getFollowFeed(page, limit)
    }
}
