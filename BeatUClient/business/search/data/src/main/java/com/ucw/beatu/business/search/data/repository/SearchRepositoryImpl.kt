package com.ucw.beatu.business.search.data.repository

import com.ucw.beatu.business.search.domain.repository.SearchRepository
import com.ucw.beatu.business.videofeed.data.api.VideoFeedApiService
import com.ucw.beatu.business.videofeed.data.mapper.toDomain
import com.ucw.beatu.business.videofeed.data.mapper.toEntity
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.shared.common.result.AppResult
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.VideoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 搜索 Repository 实现
 * 
 * 搜索策略：
 * 1. 本地搜索：先从本地数据库搜索（快速响应）
 * 2. 远程搜索：同时调用后端搜索接口
 * 3. AI搜索：调用AI搜索接口（如果有）
 * 4. 合并结果：将远程搜索结果保存到本地数据库
 * 5. 渲染UI：从本地数据库读取并渲染（数据先取出，再渲染）
 */
class SearchRepositoryImpl @Inject constructor(
    private val apiService: VideoFeedApiService,
    private val database: BeatUDatabase
) : SearchRepository {

    private val videoDao: VideoDao = database.videoDao()

    override fun searchVideos(query: String, page: Int, limit: Int): Flow<AppResult<List<Video>>> = flow {
        emit(AppResult.Loading)

        try {
            // ✅ 步骤1：本地搜索（快速响应）
            val localVideos = videoDao.observeSearchResults(query).first()
            if (localVideos.isNotEmpty()) {
                // 先返回本地结果，提升响应速度
                emit(AppResult.Success(localVideos.map { it.toDomain() }))
            }

            // ✅ 步骤2：远程搜索（异步获取最新数据）
            val response = apiService.searchVideos(query, page, limit)
            
            if (response.code == 0 || response.code == 200) {
                val remoteVideos = response.data?.items?.map { it.toDomain() } ?: emptyList()
                
                // ✅ 步骤3：将远程搜索结果保存到本地数据库
                if (remoteVideos.isNotEmpty()) {
                    // 保存视频实体
                    videoDao.insertAll(remoteVideos.map { it.toEntity() })
                    
                    // 如果本地没有结果，或者远程结果更新，则更新UI
                    if (localVideos.isEmpty() || remoteVideos.size > localVideos.size) {
                        emit(AppResult.Success(remoteVideos))
                    }
                } else if (localVideos.isEmpty()) {
                    // 本地和远程都没有结果
                    emit(AppResult.Success(emptyList()))
                }
            } else {
                // 远程搜索失败，如果有本地结果则继续使用，否则返回错误
                if (localVideos.isEmpty()) {
                    emit(AppResult.Error(Exception(response.message ?: "搜索失败")))
                }
            }
        } catch (e: Exception) {
            // 如果远程搜索失败，尝试使用本地结果
            val localVideos = try {
                videoDao.observeSearchResults(query).first()
            } catch (localError: Exception) {
                emptyList()
            }
            
            if (localVideos.isNotEmpty()) {
                emit(AppResult.Success(localVideos.map { it.toDomain() }))
            } else {
                emit(AppResult.Error(e))
            }
        }
    }.catch { e ->
        // 最后尝试使用本地结果
        val localVideos = try {
            videoDao.observeSearchResults(query).first()
        } catch (localError: Exception) {
            emptyList()
        }
        
        if (localVideos.isNotEmpty()) {
            emit(AppResult.Success(localVideos.map { it.toDomain() }))
        } else {
            emit(AppResult.Error(e))
        }
    }

    override fun observeSearchResults(query: String): Flow<List<Video>> {
        // ✅ 从本地数据库读取搜索结果（数据先取出，再渲染）
        return videoDao.observeSearchResults(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

