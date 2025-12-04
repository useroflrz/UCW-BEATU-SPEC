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
     * 根据作者名称查询该用户的作品（使用authorName）
     */
    fun observeUserWorks(authorName: String, limit: Int): Flow<List<UserWork>>
    
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

    override fun observeUserWorks(authorName: String, limit: Int): Flow<List<UserWork>> {
        // 按 authorName 查询该用户的作品
        return videoDao.observeVideosByAuthorName(authorName, limit)
            .map { entities -> entities.map { it.toUserWork() } }
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
        // 查询播放历史：JOIN watch_history表，按 lastWatchAt 降序排序，符合数据库表逻辑
        return videoDao.observeHistoryVideos(userId, limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }
}

