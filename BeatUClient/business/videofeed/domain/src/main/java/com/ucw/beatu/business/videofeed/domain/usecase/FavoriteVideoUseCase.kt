package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject

/**
 * 收藏视频UseCase
 * 负责处理视频收藏逻辑
 */
class FavoriteVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行收藏操作
     * @param videoId 视频ID
     * @return AppResult<Unit>
     */
    suspend operator fun invoke(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return repository.favoriteVideo(videoId)
    }
}

