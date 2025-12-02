package com.ucw.beatu.business.user.data.local

import com.ucw.beatu.business.user.data.mapper.toUserWork
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.shared.database.BeatUDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UserWorksLocalDataSource {
    fun observeUserWorks(authorName: String, limit: Int): Flow<List<UserWork>>
    fun observeFavoritedWorks(limit: Int): Flow<List<UserWork>>
    fun observeLikedWorks(limit: Int): Flow<List<UserWork>>
    fun observeHistoryWorks(limit: Int): Flow<List<UserWork>>
}

class UserWorksLocalDataSourceImpl @Inject constructor(
    database: BeatUDatabase
) : UserWorksLocalDataSource {

    private val videoDao = database.videoDao()

    override fun observeUserWorks(authorName: String, limit: Int): Flow<List<UserWork>> {
        // 按 authorId 查询该用户的作品
        return videoDao.observeVideosByAuthorName( authorName , limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }

    override fun observeFavoritedWorks(limit: Int): Flow<List<UserWork>> {
        // 查询收藏的视频
        return videoDao.observeFavoritedVideos(limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }

    override fun observeLikedWorks(limit: Int): Flow<List<UserWork>> {
        // 查询点赞的视频
        return videoDao.observeLikedVideos(limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }

    override fun observeHistoryWorks(limit: Int): Flow<List<UserWork>> {
        // 查询播放历史：使用 JOIN 查询，按 lastSeekMs 降序排序
        return videoDao.observeHistoryVideos(limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }
}

