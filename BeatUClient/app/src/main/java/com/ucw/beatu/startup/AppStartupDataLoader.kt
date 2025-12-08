package com.ucw.beatu.startup

import android.util.Log
import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSource
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.videofeed.data.local.VideoInteractionLocalDataSource
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import com.ucw.beatu.shared.database.entity.VideoInteractionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用启动数据加载器
 * 负责在应用启动时加载数据：
 * - 每次启动：先读本地，再读远程
 * - 视频：分页同步更新
 * - 其他数据（用户、用户交互、用户关注）：全量异步更新
 */
@Singleton
class AppStartupDataLoader @Inject constructor(
    private val videoRepository: VideoRepository,
    private val userRepository: UserRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val userLocalDataSource: UserLocalDataSource,
    private val videoRemoteDataSource: VideoRemoteDataSource,
    private val videoInteractionLocalDataSource: VideoInteractionLocalDataSource
) {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "AppStartupDataLoader"
    
    private val currentUserId: String = "BEATU" // 根据需求文档，用户名（唯一）：BEATU

    /**
     * 启动数据加载（在应用启动时调用）
     * 每次启动都执行相同的逻辑：先读本地，再读远程
     */
    fun startLoading() {
        startupScope.launch {
            try {
                Log.d(TAG, "开始加载数据：先读本地，再读远程")
                loadData()
            } catch (e: Exception) {
                Log.e(TAG, "启动数据加载失败", e)
            }
        }
    }

    /**
     * 数据加载逻辑
     * 每次启动都执行：先读本地，再读远程
     * - 视频：分页同步更新（通过VideoRepository自动处理）
     * - 其他数据：全量异步更新
     */
    private suspend fun loadData() {
        // 1. 先读本地数据（VideoRepository会自动先读本地）
        // 视频数据通过VideoRepository.getVideoFeed自动处理：先读本地，再读远程
        coroutineScope {
            async {
                try {
                    Log.d(TAG, "加载视频数据（先读本地，再读远程，分页同步更新）")
                    // VideoRepository会自动处理：先读本地，再读远程
                    val result = videoRepository.getVideoFeed(page = 1, limit = 20, orientation = null).first()
                    when (result) {
                        is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                            val source = result.metadata["source"] as? String ?: "unknown"
                            Log.d(TAG, "视频数据加载成功：${result.data.size} 条，来源：$source")
                        }
                        is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                            Log.e(TAG, "视频数据加载失败：${result.message}")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "视频数据加载异常", e)
                }
            }
        }
        
        // 2. 异步全量更新其他数据（用户、用户交互、用户关注）
        startupScope.launch {
            try {
                Log.d(TAG, "开始异步全量更新：用户、用户交互、用户关注")
                
                coroutineScope {
                    // 异步全量更新：用户数据
                    async {
                        try {
                            Log.d(TAG, "异步全量更新：用户数据")
                            val result = userRemoteDataSource.getAllUsers()
                            when (result) {
                                is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                                    result.data.forEach { user ->
                                        userLocalDataSource.saveUser(user)
                                    }
                                    Log.d(TAG, "异步全量更新用户数据成功：${result.data.size} 条")
                                }
                                is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                                    Log.e(TAG, "异步全量更新用户数据失败：${result.message}")
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "异步全量更新用户数据异常", e)
                        }
                    }
                    
                    // 异步全量更新：用户关注关系
                    async {
                        try {
                            Log.d(TAG, "异步全量更新：用户关注关系")
                            val result = userRemoteDataSource.getUserFollows(currentUserId)
                            when (result) {
                                is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                                    val follows = result.data.mapNotNull { map ->
                                        try {
                                            UserFollowEntity(
                                                userId = map["userId"] as? String ?: currentUserId,
                                                authorId = map["authorId"] as? String ?: "",
                                                isFollowed = map["isFollowed"] as? Boolean ?: false,
                                                isPending = map["isPending"] as? Boolean ?: false
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "解析关注关系失败", e)
                                            null
                                        }
                                    }
                                    userLocalDataSource.saveUserFollows(follows)
                                    Log.d(TAG, "异步全量更新用户关注关系成功：${follows.size} 条")
                                }
                                is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                                    Log.e(TAG, "异步全量更新用户关注关系失败：${result.message}")
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "异步全量更新用户关注关系异常", e)
                        }
                    }
                    
                    // 异步全量更新：视频交互数据
                    async {
                        try {
                            Log.d(TAG, "异步全量更新：视频交互数据")
                            val result = videoRemoteDataSource.getAllVideoInteractions()
                            when (result) {
                                is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                                    val interactions = result.data.mapNotNull { map ->
                                        try {
                                            VideoInteractionEntity(
                                                videoId = (map["videoId"] as? Number)?.toLong() ?: 0L,
                                                userId = map["userId"] as? String ?: currentUserId,
                                                isLiked = map["isLiked"] as? Boolean ?: false,
                                                isFavorited = map["isFavorited"] as? Boolean ?: false,
                                                isPending = map["isPending"] as? Boolean ?: false
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "解析视频交互失败", e)
                                            null
                                        }
                                    }
                                    videoInteractionLocalDataSource.saveInteractions(interactions)
                                    Log.d(TAG, "异步全量更新视频交互数据成功：${interactions.size} 条")
                                }
                                is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                                    Log.e(TAG, "异步全量更新视频交互数据失败：${result.message}")
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "异步全量更新视频交互数据异常", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "异步全量更新失败", e)
            }
        }
    }
}

