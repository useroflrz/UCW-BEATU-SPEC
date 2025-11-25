package com.ucw.beatu.business.user.data.local

import com.ucw.beatu.business.user.data.mapper.toUserWork
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.shared.database.BeatUDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UserWorksLocalDataSource {
    fun observeUserWorks(userId: String, limit: Int): Flow<List<UserWork>>
}

class UserWorksLocalDataSourceImpl @Inject constructor(
    database: BeatUDatabase
) : UserWorksLocalDataSource {

    private val videoDao = database.videoDao()

    override fun observeUserWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        return videoDao.observeTopVideos(limit)
            .map { entities -> entities.map { it.toUserWork() } }
            .distinctUntilChanged()
    }
}

