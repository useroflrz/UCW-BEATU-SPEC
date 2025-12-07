package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.model.Comment
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import javax.inject.Inject

/**
 * 发布评论UseCase
 * 负责处理评论发布逻辑
 */
class PostCommentUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    /**
     * 执行发布评论操作
     * @param videoId 视频ID
     * @param content 评论内容
     * @return AppResult<Comment> 返回新创建的评论
     */
    suspend operator fun invoke(videoId: Long, content: String): AppResult<Comment> {  // ✅ 修改：从 String 改为 Long
        if (content.isBlank()) {
            return AppResult.Error(IllegalArgumentException("评论内容不能为空"))
        }
        return repository.postComment(videoId, content)
    }
}

