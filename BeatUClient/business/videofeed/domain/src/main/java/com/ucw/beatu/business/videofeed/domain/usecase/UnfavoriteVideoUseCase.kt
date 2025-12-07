package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject

/**
 * 取消收藏UseCase
 * 负责处理取消收藏逻辑
 */
class UnfavoriteVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行取消收藏操作
     * @param videoId 视频ID
     * @return AppResult<Unit>
     */
    suspend operator fun invoke(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return repository.unfavoriteVideo(videoId)
    }
}

