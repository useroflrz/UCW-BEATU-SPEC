package com.ucw.beatu.business.search.domain.repository

import com.ucw.beatu.business.search.domain.model.SearchHistory
import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史与建议仓储接口
 * 统一提供：搜索历史、热门关键词、输入建议
 */
interface SearchHistoryRepository {
    fun observeSearchHistory(userId: String): Flow<List<SearchHistory>>

    fun saveSearchQuery(userId: String, query: String)

    fun clearSearchHistory(userId: String)

    /** 热门搜索关键词（供搜索页、Widget 等复用） */
    fun getHotKeywords(): List<String>

    /** 输入时的搜索建议：历史+热门中匹配 query 的项 */
    fun getSearchSuggestions(userId: String, query: String): List<String>
}
