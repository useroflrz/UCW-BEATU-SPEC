package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject

class ShareVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(videoId: Long): AppResult<Unit> {  // ✅ 修改：从 String 改为 Long
        return repository.shareVideo(videoId)
    }
}


