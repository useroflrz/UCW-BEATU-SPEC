package com.ucw.beatu.business.user.data.local

import com.ucw.beatu.business.user.data.mapper.toUserWork
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.shared.database.BeatUDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UserWorksLocalDataSource {
    /**
     * 根据作者ID查询该用户的作品（使用authorId）
     */
    fun observeUserWorks(authorId: String, limit: Int): Flow<List<UserWork>>
    
    /**
     * 查询用户收藏的视频（需要userId，JOIN user_interactions表）
     */
    fun observeFavoritedWorks(userId: String, limit: Int): Flow<List<UserWork>>
    
    /**
     * 查询用户点赞的视频（需要userId，JOIN user_interactions表）
     */
    fun observeLikedWorks(userId: String, limit: Int): Flow<List<UserWork>>
    
    /**
     * 查询用户观看历史（需要userId，JOIN watch_history表）
     */
    fun observeHistoryWorks(userId: String, limit: Int): Flow<List<UserWork>>
}

class UserWorksLocalDataSourceImpl @Inject constructor(
    database: BeatUDatabase
) : UserWorksLocalDataSource {

    private val videoDao = database.videoDao()

    override fun observeUserWorks(authorId: String, limit: Int): Flow<List<UserWork>> {
        // 从数据库查询该用户的作品（通过 authorId）
        android.util.Log.d("UserWorksLocalDataSource", "查询用户作品：authorId=$authorId, limit=$limit")
        return videoDao.observeVideosByAuthorId(authorId, limit)
            .map { entities -> 
                android.util.Log.d("UserWorksLocalDataSource", "从数据库查询到 ${entities.size} 条视频数据")
                entities.map { it.toUserWork() } 
            }
            .distinctUntilChanged()
    }

    override fun observeFavoritedWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        // 查询收藏的视频：JOIN user_interactions表，符合数据库表逻辑
        return videoDao.observeFavoritedVideos(userId, limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }

    override fun observeLikedWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        // 查询点赞的视频：JOIN user_interactions表，符合数据库表逻辑
        return videoDao.observeLikedVideos(userId, limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }

    override fun observeHistoryWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        // 查询播放历史：JOIN watch_history表，按 watchedAt 升序排序，最先观看的显示在前面
        android.util.Log.d("UserWorksLocalDataSource", "查询观看历史：userId=$userId, limit=$limit")
        return videoDao.observeHistoryVideos(userId, limit)
            .map { entities -> 
                android.util.Log.d("UserWorksLocalDataSource", "从数据库查询到 ${entities.size} 条历史视频数据")
                if (entities.isNotEmpty()) {
                    android.util.Log.d("UserWorksLocalDataSource", "第一条历史视频：videoId=${entities[0].videoId}, title=${entities[0].title}")
                }
                entities.map { it.toUserWork() } 
            }
            .distinctUntilChanged()
    }
}

