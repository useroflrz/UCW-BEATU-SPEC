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
import javax.inject.Inject

/**
 * 搜索 Repository 实现
 * 调用后端搜索接口，将结果保存到本地数据库，然后从数据库读取
 */
class SearchRepositoryImpl @Inject constructor(
    private val apiService: VideoFeedApiService,
    private val database: BeatUDatabase
) : SearchRepository {

    private val videoDao: VideoDao = database.videoDao()

    override fun searchVideos(query: String, page: Int, limit: Int): Flow<AppResult<List<Video>>> = flow {
        emit(AppResult.Loading)

        try {
            // 调用后端搜索接口
            val response = apiService.searchVideos(query, page, limit)
            
            if (response.code == 0 || response.code == 200) {
                val videos = response.data?.items?.map { it.toDomain() } ?: emptyList()
                
                // 将搜索结果保存到本地数据库
                if (videos.isNotEmpty()) {
                    videoDao.insertAll(videos.map { it.toEntity() })
                }
                
                emit(AppResult.Success(videos))
            } else {
                emit(AppResult.Error(Exception(response.message ?: "搜索失败")))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e))
        }
    }.catch { e ->
        emit(AppResult.Error(e))
    }

    override fun observeSearchResults(query: String): Flow<List<Video>> {
        // 从本地数据库读取搜索结果
        return videoDao.observeSearchResults(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

