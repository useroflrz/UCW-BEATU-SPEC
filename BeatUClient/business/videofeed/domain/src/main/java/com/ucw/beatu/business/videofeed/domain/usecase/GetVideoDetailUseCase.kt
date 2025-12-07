package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject

/**
 * 获取视频详情UseCase
 * 负责获取单个视频的详细信息
 */
class GetVideoDetailUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行获取视频详情
     * @param videoId 视频ID
     * @return AppResult<Video>
     */
    suspend operator fun invoke(videoId: Long): AppResult<Video> {  // ✅ 修改：从 String 改为 Long
        return repository.getVideoDetail(videoId)
    }
}

