package com.ucw.beatu.business.search.data.repository

import com.ucw.beatu.business.search.data.api.AISearchApiService
import com.ucw.beatu.business.search.data.api.dto.AISearchStreamChunk
import com.ucw.beatu.business.search.domain.model.AISearchResult
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import com.ucw.beatu.business.videofeed.data.api.VideoFeedApiService
import com.ucw.beatu.business.videofeed.data.api.dto.VideoDto
import com.ucw.beatu.business.videofeed.data.mapper.toEntity
import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.VideoDao
import com.ucw.beatu.shared.database.entity.VideoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

/**
 * AI 搜索 Repository 实现
 * 
 * 功能：
 * 1. 调用AI搜索接口获取视频ID列表（本地+远程）
 * 2. 根据视频ID从远程获取视频详情
 * 3. 将视频详情保存到本地数据库
 * 4. 从本地数据库读取并渲染UI
 */
class AISearchRepositoryImpl @Inject constructor(
    private val apiService: AISearchApiService,
    private val videoApiService: VideoFeedApiService,
    private val database: BeatUDatabase
) : AISearchRepository {
    
    private val videoDao: VideoDao = database.videoDao()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "AISearchRepository"
    }
    
    override fun searchStream(userQuery: String): Flow<AISearchResult> {
        return apiService.searchStream(userQuery)
            .map { chunk ->
                processChunk(chunk)
            }
            .onEach { result ->
                // ✅ 当收到视频ID列表时，异步从远程获取视频详情并保存到本地
                val videoIds = result.videoIds + result.localVideoIds
                if (videoIds.isNotEmpty()) {
                    syncScope.launch {
                        fetchAndSaveVideos(videoIds)
                    }
                }
            }
    }
    
    /**
     * 根据视频ID列表从远程获取视频详情并保存到本地数据库
     */
    private suspend fun fetchAndSaveVideos(videoIds: List<Long>) {  // ✅ 修改：从 List<String> 改为 List<Long>
        try {
            val videoEntities = mutableListOf<VideoEntity>()
            
            // 批量获取视频详情
            for (videoId in videoIds) {
                try {
                    val response = videoApiService.getVideoDetail(videoId.toString())  // Retrofit Path 参数需要 String
                    
                    if (response.code == 0 || response.code == 200) {
                        response.data?.let { videoDto ->
                            videoEntities.add(videoDto.toEntity())
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "获取视频详情失败: videoId=$videoId", e)
                }
            }
            
            // 批量保存到本地数据库
            if (videoEntities.isNotEmpty()) {
                videoDao.insertAll(videoEntities)
                AppLogger.d(TAG, "已保存 ${videoEntities.size} 个视频到本地数据库")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "批量获取并保存视频失败", e)
        }
    }
    
    /**
     * 处理数据块，转换为搜索结果
     */
    private fun processChunk(chunk: AISearchStreamChunk): AISearchResult {
        return when (chunk.chunkType) {
            "answer" -> {
                AISearchResult(aiAnswer = chunk.content)
            }
            "keywords" -> {
                try {
                    val keywords = parseJsonArray(chunk.content)
                    AISearchResult(keywords = keywords)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "解析关键词失败", e)
                    AISearchResult()
                }
            }
            "videoIds" -> {
                try {
                    val videoIds = parseJsonArrayToLong(chunk.content)  // ✅ 修改：解析为 Long 列表
                    AISearchResult(videoIds = videoIds)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "解析视频 ID 失败", e)
                    AISearchResult()
                }
            }
            "localVideoIds" -> {
                try {
                    val localVideoIds = parseJsonArrayToLong(chunk.content)  // ✅ 修改：解析为 Long 列表
                    AISearchResult(localVideoIds = localVideoIds)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "解析本地视频 ID 失败", e)
                    AISearchResult()
                }
            }
            "error" -> {
                AISearchResult(error = chunk.content)
            }
            else -> {
                AISearchResult()
            }
        }
    }
    
    /**
     * 解析 JSON 数组字符串为 String 列表（用于关键词）
     */
    private fun parseJsonArray(jsonString: String): List<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "解析 JSON 数组失败: $jsonString", e)
            emptyList()
        }
    }
    
    /**
     * 解析 JSON 数组字符串为 Long 列表（用于视频 ID）
     */
    private fun parseJsonArrayToLong(jsonString: String): List<Long> {  // ✅ 新增：解析为 Long 列表
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { 
                jsonArray.getLong(it)  // 直接获取 Long 类型
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "解析 JSON 数组为 Long 失败: $jsonString", e)
            emptyList()
        }
    }
}

