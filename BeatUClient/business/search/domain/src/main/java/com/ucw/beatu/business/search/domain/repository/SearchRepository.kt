package com.ucw.beatu.business.search.domain.repository

import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * 视频搜索 Repository 接口
 */
interface SearchRepository {
    /**
     * 搜索视频
     * 
     * @param query 搜索关键词
     * @param page 页码
     * @param limit 每页数量
     * @return Flow<AppResult<List<Video>>> 搜索结果
     */
    fun searchVideos(query: String, page: Int, limit: Int): Flow<AppResult<List<Video>>>
    
    /**
     * 从本地数据库获取搜索结果
     * 
     * @param query 搜索关键词
     * @return Flow<List<Video>> 本地搜索结果
     */
    fun observeSearchResults(query: String): Flow<List<Video>>
}

