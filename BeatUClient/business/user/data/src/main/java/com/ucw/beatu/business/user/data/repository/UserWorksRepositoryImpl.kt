package com.ucw.beatu.business.user.data.repository

import com.ucw.beatu.business.user.data.local.UserWorksLocalDataSource
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.domain.repository.UserWorksRepository
import com.ucw.beatu.business.videofeed.domain.model.Video
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.common.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

class UserWorksRepositoryImpl @Inject constructor(
    private val localDataSource: UserWorksLocalDataSource,
    private val videoRepository: VideoRepository
) : UserWorksRepository {

    // 后台协程作用域，用于异步刷新数据
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeUserWorks(authorId: String, limit: Int): Flow<List<UserWork>> {
        // 先从本地数据库读取并返回（快速显示）
        val localFlow = localDataSource.observeUserWorks(authorId, limit)
        
        // 在后台从远程API获取最新数据并更新本地数据库
        // 由于后端API不支持按authorId查询，我们需要获取多页数据以确保覆盖该作者的所有作品
        refreshScope.launch {
            try {
                // 获取足够多的页数（例如前5页，每页30条，共150条视频）
                // 这样可以确保覆盖大部分作者的所有作品
                val pagesToFetch = 5
                val pageLimit = 30
                
                for (page in 1..pagesToFetch) {
                    // 只获取第一个成功的结果，忽略Loading状态
                    val result = videoRepository.getVideoFeed(page, pageLimit, null)
                        .take(10) // 最多取10个值，避免无限等待
                        .firstOrNull { 
                            it is AppResult.Success<*> || it is AppResult.Error
                        }
                    
                    when (result) {
                        is AppResult.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val videos = result.data as? List<Video>
                            if (videos != null) {
                                // VideoRepository会自动保存数据到本地数据库
                                // 如果获取到的视频数量少于pageLimit，说明已经获取完所有数据
                                if (videos.size < pageLimit) {
                                    break
                                }
                            }
                        }
                        is AppResult.Error -> {
                            // 如果获取失败，停止继续获取
                            break
                        }
                        else -> {
                            // Loading或其他状态，继续下一页
                        }
                    }
                }
            } catch (e: Exception) {
                // 静默失败，不影响本地数据的显示
                android.util.Log.e("UserWorksRepository", "Failed to refresh video data from remote", e)
            }
        }
        
        return localFlow
    }

    override fun observeFavoritedWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        return localDataSource.observeFavoritedWorks(userId, limit)
    }

    override fun observeLikedWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        return localDataSource.observeLikedWorks(userId, limit)
    }

    override fun observeHistoryWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        return localDataSource.observeHistoryWorks(userId, limit)
    }
}

