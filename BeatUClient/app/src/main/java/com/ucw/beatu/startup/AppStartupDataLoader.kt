package com.ucw.beatu.startup

import android.util.Log
import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSource
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.videofeed.data.local.VideoInteractionLocalDataSource
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import com.ucw.beatu.shared.database.dao.UserFollowDao
import com.ucw.beatu.shared.database.dao.VideoInteractionDao
import com.ucw.beatu.shared.database.dao.WatchHistoryDao
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import com.ucw.beatu.shared.database.entity.VideoInteractionEntity
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity
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
    private val videoInteractionLocalDataSource: VideoInteractionLocalDataSource,
    private val watchHistoryDao: WatchHistoryDao,
    private val userFollowDao: UserFollowDao,
    private val videoInteractionDao: VideoInteractionDao
) {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "AppStartupDataLoader"
    
    private val currentUserId: String = "BEATU" // 根据需求文档，用户名（唯一）：BEATU
    
    /**
     * 安全地解析布尔值
     * 支持多种类型：Boolean、String("true"/"false")、Number(1/0)
     */
    private fun parseBoolean(value: Any?): Boolean {
        val result = when (value) {
            is Boolean -> {
                Log.d(TAG, "parseBoolean: 直接是 Boolean 类型，值=$value")
                value
            }
            is String -> {
                val bool = value.equals("true", ignoreCase = true)
                Log.d(TAG, "parseBoolean: 是 String 类型，值='$value'，解析为=$bool")
                bool
            }
            is Number -> {
                val bool = value.toInt() == 1
                Log.d(TAG, "parseBoolean: 是 Number 类型，值=$value，解析为=$bool")
                bool
            }
            null -> {
                Log.d(TAG, "parseBoolean: 值为 null，返回 false")
                false
            }
            else -> {
                // 尝试转换为字符串再解析
                val str = value.toString()
                val bool = str.equals("true", ignoreCase = true) || str == "1"
                Log.d(TAG, "parseBoolean: 未知类型 ${value.javaClass.simpleName}，值=$value，转换为字符串='$str'，解析为=$bool")
                bool
            }
        }
        return result
    }

    /**
     * 启动数据加载（在应用启动时调用）
     * 每次启动都执行相同的逻辑：先读本地，再读远程
     */
    fun startLoading() {
        Log.i(TAG, "startLoading invoked")
        startupScope.launch {
            try {
                Log.i(TAG, "startLoading launch begin")
                Log.d(TAG, "开始加载数据：先读本地，再读远程")
                loadData()
                Log.i(TAG, "startLoading launch end")
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
                            Log.i(TAG, "异步全量更新：用户关注关系，开始请求 /api/users/$currentUserId/follows")
                            val result = userRemoteDataSource.getUserFollows(currentUserId)
                            when (result) {
                                is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                                    Log.d(TAG, "异步全量更新用户关注关系：收到 ${result.data.size} 条原始数据")
                                    val follows = result.data.mapNotNull { map ->
                                        try {
                                            Log.d(TAG, "解析关注关系：map=$map")
                                            Log.d(TAG, "关注关系字段名：${map.keys.joinToString()}")
                                            // 尝试不同的字段名变体
                                            val userId = map["userId"] as? String 
                                                ?: map["user_id"] as? String 
                                                ?: map["userId"]?.toString()
                                                ?: currentUserId
                                            val authorId = map["authorId"] as? String 
                                                ?: map["author_id"] as? String 
                                                ?: map["authorId"]?.toString()
                                                ?: ""
                                            val isFollowed = parseBoolean(
                                                map["isFollowed"] ?: map["is_followed"]
                                            )
                                            val isPending = parseBoolean(
                                                map["isPending"] ?: map["is_pending"]
                                            )
                                            
                                            Log.d(TAG, "解析后的值：userId=$userId, authorId=$authorId, isFollowed=$isFollowed, isPending=$isPending")
                                            
                                            UserFollowEntity(
                                                userId = userId,
                                                authorId = authorId,
                                                isFollowed = isFollowed,
                                                isPending = isPending
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "解析关注关系失败：map=$map", e)
                                            Log.e(TAG, "可用字段名：${map.keys.joinToString()}")
                                            e.printStackTrace()
                                            null
                                        }
                                    }
                                    try {
                                        Log.d(TAG, "准备保存 ${follows.size} 条关注关系到数据库")
                                        follows.forEachIndexed { index, follow ->
                                            Log.d(TAG, "关注关系[$index]：userId=${follow.userId}, authorId=${follow.authorId}, isFollowed=${follow.isFollowed}, isPending=${follow.isPending}")
                                        }
                                        userLocalDataSource.saveUserFollows(follows)
                                        Log.i(TAG, "异步全量更新用户关注关系成功：${follows.size} 条")
                                        // 从数据库验证保存是否成功
                                        val savedFollows = userFollowDao.observeFollowees(currentUserId).first()
                                        Log.i(TAG, "数据库验证：查询到 ${savedFollows.size} 条关注关系（isFollowed=true）")
                                        if (savedFollows.size != follows.filter { it.isFollowed }.size) {
                                            Log.w(TAG, "警告：保存的关注关系数量不匹配！期望 ${follows.filter { it.isFollowed }.size} 条（isFollowed=true），实际 ${savedFollows.size} 条")
                                        }
                                        savedFollows.forEach { follow ->
                                            Log.d(TAG, "数据库中的关注关系：userId=${follow.userId}, authorId=${follow.authorId}, isFollowed=${follow.isFollowed}, isPending=${follow.isPending}")
                                        }
                                        // 查询所有关注关系（包括 isFollowed=false 的）
                                        val allFollowsInDb = userFollowDao.getAllByUserId(currentUserId)
                                        Log.i(TAG, "数据库完整验证：查询到 ${allFollowsInDb.size} 条关注关系（包括 isFollowed=false）")
                                        allFollowsInDb.forEach { follow ->
                                            Log.d(TAG, "数据库完整数据：userId=${follow.userId}, authorId=${follow.authorId}, isFollowed=${follow.isFollowed}, isPending=${follow.isPending}")
                                        }
                                        // 查询所有关注关系（不限制 userId）
                                        val allFollows = userFollowDao.getAll()
                                        Log.i(TAG, "数据库全局验证：查询到 ${allFollows.size} 条关注关系（所有用户）")
                                        allFollows.forEachIndexed { index, follow ->
                                            Log.d(TAG, "数据库全局数据[$index]：userId=${follow.userId}, authorId=${follow.authorId}, isFollowed=${follow.isFollowed}, isPending=${follow.isPending}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "保存用户关注关系失败：${e.message}", e)
                                        Log.e(TAG, "异常类型：${e.javaClass.name}")
                                        e.printStackTrace()
                                        // 尝试逐个保存以定位问题
                                        Log.d(TAG, "尝试逐个保存关注关系以定位问题")
                                        follows.forEachIndexed { index, follow ->
                                            try {
                                                userFollowDao.insert(follow)
                                                Log.d(TAG, "成功保存关注关系[$index]：userId=${follow.userId}, authorId=${follow.authorId}")
                                            } catch (e2: Exception) {
                                                Log.e(TAG, "保存关注关系[$index]失败：userId=${follow.userId}, authorId=${follow.authorId}, 错误=${e2.message}", e2)
                                            }
                                        }
                                    }
                                }
                                is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                                    Log.e(TAG, "异步全量更新用户关注关系失败：${result.message}, throwable=${result.throwable?.message}")
                                    result.throwable?.printStackTrace()
                                }
                                else -> {
                                    Log.w(TAG, "异步全量更新用户关注关系：未知结果类型")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "异步全量更新用户关注关系异常", e)
                            e.printStackTrace()
                        }
                    }
                    
                    // 异步全量更新：视频交互数据
                    async {
                        try {
                            Log.i(TAG, "异步全量更新：视频交互数据，开始请求 /api/videos/interactions")
                            val result = videoRemoteDataSource.getAllVideoInteractions()
                            when (result) {
                                is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                                    Log.d(TAG, "异步全量更新视频交互数据：收到 ${result.data.size} 条原始数据")
                                    val interactions = result.data.mapNotNull { map ->
                                        try {
                                            Log.d(TAG, "解析视频交互：map=$map")
                                            Log.d(TAG, "视频交互字段名：${map.keys.joinToString()}")
                                            // 尝试不同的字段名变体
                                            val videoId = (map["videoId"] as? Number)?.toLong() 
                                                ?: (map["video_id"] as? Number)?.toLong()
                                                ?: (map["videoId"]?.toString()?.toLongOrNull())
                                                ?: 0L
                                            val userId = map["userId"] as? String 
                                                ?: map["user_id"] as? String 
                                                ?: map["userId"]?.toString()
                                                ?: currentUserId
                                            val isLiked = parseBoolean(
                                                map["isLiked"] ?: map["is_liked"]
                                            )
                                            val isFavorited = parseBoolean(
                                                map["isFavorited"] ?: map["is_favorited"]
                                            )
                                            val isPending = parseBoolean(
                                                map["isPending"] ?: map["is_pending"]
                                            )
                                            
                                            Log.d(TAG, "解析后的值：videoId=$videoId, userId=$userId, isLiked=$isLiked, isFavorited=$isFavorited, isPending=$isPending")
                                            
                                            VideoInteractionEntity(
                                                videoId = videoId,
                                                userId = userId,
                                                isLiked = isLiked,
                                                isFavorited = isFavorited,
                                                isPending = isPending
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "解析视频交互失败：map=$map", e)
                                            Log.e(TAG, "可用字段名：${map.keys.joinToString()}")
                                            e.printStackTrace()
                                            null
                                        }
                                    }
                                    try {
                                        Log.d(TAG, "准备保存 ${interactions.size} 条视频交互到数据库")
                                        interactions.forEachIndexed { index, interaction ->
                                            Log.d(TAG, "视频交互[$index]：videoId=${interaction.videoId}, userId=${interaction.userId}, isLiked=${interaction.isLiked}, isFavorited=${interaction.isFavorited}, isPending=${interaction.isPending}")
                                        }
                                        videoInteractionLocalDataSource.saveInteractions(interactions)
                                        Log.i(TAG, "异步全量更新视频交互数据成功：${interactions.size} 条")
                                        // 从数据库验证保存是否成功
                                        val savedInteractions = videoInteractionDao.getInteractionsByUser(currentUserId)
                                        Log.i(TAG, "数据库验证：查询到 ${savedInteractions.size} 条视频交互")
                                        if (savedInteractions.size != interactions.size) {
                                            Log.w(TAG, "警告：保存的视频交互数量不匹配！期望 ${interactions.size} 条，实际 ${savedInteractions.size} 条")
                                        }
                                        savedInteractions.forEach { interaction ->
                                            Log.d(TAG, "数据库中的视频交互：videoId=${interaction.videoId}, userId=${interaction.userId}, isLiked=${interaction.isLiked}, isFavorited=${interaction.isFavorited}, isPending=${interaction.isPending}")
                                        }
                                        // 查询所有视频交互（用于完整验证）
                                        val allInteractions = videoInteractionDao.getAll()
                                        Log.i(TAG, "数据库全局验证：查询到 ${allInteractions.size} 条视频交互（所有用户）")
                                        allInteractions.forEach { interaction ->
                                            Log.d(TAG, "数据库全局数据：videoId=${interaction.videoId}, userId=${interaction.userId}, isLiked=${interaction.isLiked}, isFavorited=${interaction.isFavorited}, isPending=${interaction.isPending}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "保存视频交互数据失败：${e.message}", e)
                                        Log.e(TAG, "异常类型：${e.javaClass.name}")
                                        e.printStackTrace()
                                        // 尝试逐个保存以定位问题
                                        Log.d(TAG, "尝试逐个保存视频交互以定位问题")
                                        interactions.forEachIndexed { index, interaction ->
                                            try {
                                                videoInteractionDao.insertOrUpdate(interaction)
                                                Log.d(TAG, "成功保存视频交互[$index]：videoId=${interaction.videoId}, userId=${interaction.userId}")
                                            } catch (e2: Exception) {
                                                Log.e(TAG, "保存视频交互[$index]失败：videoId=${interaction.videoId}, userId=${interaction.userId}, 错误=${e2.message}", e2)
                                            }
                                        }
                                    }
                                }
                                is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                                    Log.e(TAG, "异步全量更新视频交互数据失败：${result.message}, throwable=${result.throwable?.message}")
                                    result.throwable?.printStackTrace()
                                }
                                else -> {
                                    Log.w(TAG, "异步全量更新视频交互数据：未知结果类型")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "异步全量更新视频交互数据异常", e)
                            e.printStackTrace()
                        }
                    }
                    
                    // 异步全量更新：观看历史数据
                    async {
                        try {
                            Log.i(TAG, "异步全量更新：观看历史数据，开始请求 /api/videos/watch-history")
                            val result = videoRemoteDataSource.getAllWatchHistories()
                            when (result) {
                                is com.ucw.beatu.shared.common.result.AppResult.Success -> {
                                    val histories = result.data.mapNotNull { map ->
                                        try {
                                            Log.d(TAG, "解析观看历史：map=$map")
                                            Log.d(TAG, "观看历史字段名：${map.keys.joinToString()}")
                                            // 尝试不同的字段名变体
                                            val videoId = (map["videoId"] as? Number)?.toLong() 
                                                ?: (map["video_id"] as? Number)?.toLong()
                                                ?: (map["videoId"]?.toString()?.toLongOrNull())
                                                ?: 0L
                                            val userId = map["userId"] as? String 
                                                ?: map["user_id"] as? String 
                                                ?: map["userId"]?.toString()
                                                ?: currentUserId
                                            val lastPlayPositionMs = (map["lastPlayPositionMs"] as? Number)?.toLong() 
                                                ?: (map["last_play_position_ms"] as? Number)?.toLong()
                                                ?: (map["lastPlayPositionMs"]?.toString()?.toLongOrNull())
                                                ?: 0L
                                            val watchedAt = (map["watchedAt"] as? Number)?.toLong() 
                                                ?: (map["watched_at"] as? Number)?.toLong()
                                                ?: (map["watchedAt"]?.toString()?.toLongOrNull())
                                                ?: System.currentTimeMillis()
                                            
                                            Log.d(TAG, "解析后的值：videoId=$videoId, userId=$userId, lastPlayPositionMs=$lastPlayPositionMs, watchedAt=$watchedAt")
                                            
                                            WatchHistoryEntity(
                                                videoId = videoId,
                                                userId = userId,
                                                lastPlayPositionMs = lastPlayPositionMs,
                                                watchedAt = watchedAt,
                                                isPending = false  // 从远程拉取的数据不需要标记为待同步
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "解析观看历史失败：map=$map", e)
                                            Log.e(TAG, "可用字段名：${map.keys.joinToString()}")
                                            e.printStackTrace()
                                            null
                                        }
                                    }
                                    try {
                                        Log.d(TAG, "准备保存 ${histories.size} 条观看历史到数据库")
                                        // 打印每条传输的数据
                                        histories.forEachIndexed { index, history ->
                                            Log.d(TAG, "观看历史[$index]: videoId=${history.videoId}, userId=${history.userId}, lastPlayPositionMs=${history.lastPlayPositionMs}, watchedAt=${history.watchedAt}, isPending=${history.isPending}")
                                        }
                                        watchHistoryDao.upsertAll(histories)
                                        Log.i(TAG, "异步全量更新观看历史数据成功：${histories.size} 条")
                                        // 查询所有观看历史（用于完整验证）
                                        val allHistories = watchHistoryDao.getAll()
                                        Log.i(TAG, "数据库全局验证：查询到 ${allHistories.size} 条观看历史（所有用户）")
                                        allHistories.forEach { history ->
                                            Log.d(TAG, "数据库全局数据：videoId=${history.videoId}, userId=${history.userId}, lastPlayPositionMs=${history.lastPlayPositionMs}, watchedAt=${history.watchedAt}, isPending=${history.isPending}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "保存观看历史数据失败：${e.message}", e)
                                        Log.e(TAG, "异常类型：${e.javaClass.name}")
                                        e.printStackTrace()
                                    }
                                }
                                is com.ucw.beatu.shared.common.result.AppResult.Error -> {
                                    Log.e(TAG, "异步全量更新观看历史数据失败：${result.message}")
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "异步全量更新观看历史数据异常", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "异步全量更新失败", e)
            }
        }
    }
}

