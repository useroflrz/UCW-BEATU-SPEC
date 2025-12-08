package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import javax.inject.Inject

/**
 * 保存观看历史UseCase
 * 负责处理观看历史的异步写入逻辑
 */
class SaveWatchHistoryUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行保存观看历史操作
     * @param videoId 视频ID
     * @param positionMs 播放位置（毫秒），默认为0
     */
    suspend operator fun invoke(videoId: Long, positionMs: Long = 0L) {
        repository.saveWatchHistory(videoId, positionMs)
    }
}

