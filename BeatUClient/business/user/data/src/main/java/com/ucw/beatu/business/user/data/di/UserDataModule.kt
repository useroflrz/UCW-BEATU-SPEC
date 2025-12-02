package com.ucw.beatu.business.user.data.di

import com.ucw.beatu.business.user.data.api.UserApiService
import com.ucw.beatu.business.user.data.local.UserLocalDataSource
import com.ucw.beatu.business.user.data.local.UserLocalDataSourceImpl
import com.ucw.beatu.business.user.data.local.UserWorksLocalDataSource
import com.ucw.beatu.business.user.data.local.UserWorksLocalDataSourceImpl
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSource
import com.ucw.beatu.business.user.data.remote.UserRemoteDataSourceImpl
import com.ucw.beatu.business.user.data.repository.UserRepositoryImpl
import com.ucw.beatu.business.user.data.repository.UserWorksRepositoryImpl
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.user.domain.repository.UserWorksRepository
import com.ucw.beatu.shared.network.retrofit.RetrofitProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 用户数据层依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object UserApiModule {

    @Provides
    @Singleton
    fun provideUserApiService(
        retrofit: Retrofit
    ): UserApiService {
        return RetrofitProvider.createService(retrofit)
    }
}

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
    abstract fun bindUserRemoteDataSource(
        impl: UserRemoteDataSourceImpl
    ): UserRemoteDataSource

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

