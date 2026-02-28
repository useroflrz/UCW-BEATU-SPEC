package com.ucw.beatu.business.search.data.repository

import com.ucw.beatu.business.search.domain.model.SearchHistory
import com.ucw.beatu.business.search.domain.repository.SearchHistoryRepository
import com.ucw.beatu.shared.common.threading.SuspendBridge
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchHistoryRepositoryImpl @Inject constructor(
    database: BeatUDatabase
) : SearchHistoryRepository {
    private val searchHistoryDao = database.searchHistoryDao()

    override fun observeSearchHistory(userId: String): Flow<List<SearchHistory>> {
        return searchHistoryDao.observeSearchHistory(userId).map { historyList ->
            historyList.map { entity ->
                SearchHistory(
                    query = entity.query,
                    userId = entity.userId,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override fun saveSearchQuery(userId: String, query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return

        val history = SearchHistoryEntity(
            query = trimmedQuery,
            userId = userId,
            createdAt = System.currentTimeMillis()
        )
        SuspendBridge.runBlocking {
            searchHistoryDao.insert(history)
            while (searchHistoryDao.getHistoryCount(userId) > 5) {
                searchHistoryDao.deleteOldestRecord(userId)
            }
        }
    }

    override fun clearSearchHistory(userId: String) {
        SuspendBridge.runBlocking {
            searchHistoryDao.deleteByUserId(userId)
        }
    }

    override fun getHotKeywords(): List<String> {
        return HOT_KEYWORDS
    }

    override fun getSearchSuggestions(userId: String, query: String): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val history = SuspendBridge.runBlocking {
            searchHistoryDao.getSearchHistory(userId).map { it.query }
        }
        val fromHistory = history.filter { it.lowercase().contains(q) }
        val fromHot = HOT_KEYWORDS.filter { it.lowercase().contains(q) }
        return (fromHistory + fromHot).distinct().take(10)
    }

    companion object {
        private val HOT_KEYWORDS = listOf(
            "搞笑视频", "美食", "旅游", "音乐", "舞蹈",
            "萌宠", "游戏", "科技", "运动", "穿搭"
        )
    }
}
