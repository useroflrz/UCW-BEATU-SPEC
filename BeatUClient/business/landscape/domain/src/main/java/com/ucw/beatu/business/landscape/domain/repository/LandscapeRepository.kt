package com.ucw.beatu.business.landscape.domain.repository

import com.ucw.beatu.business.landscape.domain.model.VideoItem
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * 横屏视频仓储接口
 * 定义横屏业务层需要的数据访问接口
 */
interface LandscapeRepository {
    /**
     * 获取横屏视频列表（分页）
     * @param page 页码，从1开始
     * @param limit 每页数量
     * @return Flow<AppResult<List<Video>>> 响应式数据流
     */
    fun getLandscapeVideos(page: Int = 1, limit: Int = 20): Flow<AppResult<List<VideoItem>>>

    /**
     * 加载更多横屏视频
     * @param page 页码
     * @param limit 每页数量
     * @return Flow<AppResult<List<Video>>> 响应式数据流
     */
    fun loadMoreLandscapeVideos(page: Int, limit: Int = 20): Flow<AppResult<List<VideoItem>>>
}

