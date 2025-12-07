package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject

/**
 * 点赞视频UseCase
 * 负责处理视频点赞逻辑
 */
class LikeVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行点赞操作
     * @param videoId 视频ID
     * @return AppResult<Unit>
     */
    suspend operator fun invoke(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return repository.likeVideo(videoId)
    }
}

