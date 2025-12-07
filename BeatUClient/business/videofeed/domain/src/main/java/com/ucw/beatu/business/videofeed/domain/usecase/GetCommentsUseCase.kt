package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取评论列表UseCase
 * 负责获取视频的评论列表（分页）
 */
class GetCommentsUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行获取评论列表
     * @param videoId 视频ID
     * @param page 页码，从1开始
     * @param limit 每页数量
     * @return Flow<AppResult<List<Comment>>> 响应式数据流
     */
    operator fun invoke(
        videoId: Long,  // ✅ 修改：从 String 改为 Long
        page: Int = 1,
        limit: Int = 20
    ): Flow<AppResult<List<Comment>>> {
        return repository.getComments(videoId, page, limit)
    }
}

