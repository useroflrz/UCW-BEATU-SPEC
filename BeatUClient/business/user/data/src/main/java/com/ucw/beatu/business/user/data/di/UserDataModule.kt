package com.ucw.beatu.business.user.data.di

import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.local.UserLocalDataSourceImpl
import com.ucw.beatu.business.user.data.local.UserWorksLocalDataSource
import com.ucw.beatu.business.user.data.local.UserWorksLocalDataSourceImpl
import com.ucw.beatu.business.user.data.repository.UserRepositoryImpl
import com.ucw.beatu.business.user.data.repository.UserWorksRepositoryImpl
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.user.domain.repository.UserWorksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 用户数据层依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserDataModule {

    @Binds
    @Singleton
    abstract fun bindUserLocalDataSource(
        impl: UserLocalDataSourceImpl
    ): UserLocalDataSource

    @Binds
    @Singleton
    abstract fun bindUserWorksLocalDataSource(
        impl: UserWorksLocalDataSourceImpl
    ): UserWorksLocalDataSource

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindUserWorksRepository(
        impl: UserWorksRepositoryImpl
    ): UserWorksRepository
}

