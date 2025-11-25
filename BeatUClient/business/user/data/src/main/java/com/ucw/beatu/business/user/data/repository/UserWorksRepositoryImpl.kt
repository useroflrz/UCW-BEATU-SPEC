package com.ucw.beatu.business.user.data.repository

import com.ucw.beatu.business.user.data.local.UserWorksLocalDataSource
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.domain.repository.UserWorksRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserWorksRepositoryImpl @Inject constructor(
    private val localDataSource: UserWorksLocalDataSource
) : UserWorksRepository {

    override fun observeUserWorks(userId: String, limit: Int): Flow<List<UserWork>> {
        return localDataSource.observeUserWorks(userId, limit)
    }
}

