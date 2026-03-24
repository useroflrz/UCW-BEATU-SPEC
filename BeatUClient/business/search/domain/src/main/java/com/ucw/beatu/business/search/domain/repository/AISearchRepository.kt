package com.ucw.beatu.business.search.domain.repository

import com.ucw.beatu.business.search.domain.model.AISearchResult
import kotlinx.coroutines.flow.Flow

/**
 * AI 搜索 Repository 接口
 */
interface AISearchRepository {
    /**
     * 执行 AI 搜索（流式）
     * 
     * @param userQuery 用户查询文本
     * @return Flow<AISearchResult> 流式搜索结果
     */
    fun searchStream(userQuery: String): Flow<AISearchResult>

    /** 停止当前 SSE 流，避免新旧流并发渲染。 */
    fun stopCurrentStream()
}

