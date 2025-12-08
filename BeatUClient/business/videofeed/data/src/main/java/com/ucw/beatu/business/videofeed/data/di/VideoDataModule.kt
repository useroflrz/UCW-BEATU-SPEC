package com.ucw.beatu.business.videofeed.data.di

import com.ucw.beatu.business.videofeed.data.api.VideoFeedApiService
import com.ucw.beatu.business.videofeed.data.local.VideoInteractionLocalDataSource
import com.ucw.beatu.business.videofeed.data.local.VideoInteractionLocalDataSourceImpl
import com.ucw.beatu.business.videofeed.data.local.VideoLocalDataSource
import com.ucw.beatu.business.videofeed.data.local.VideoLocalDataSourceImpl
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSource
import com.ucw.beatu.business.videofeed.data.remote.VideoRemoteDataSourceImpl
import com.ucw.beatu.business.videofeed.data.repository.VideoRepositoryImpl
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import com.ucw.beatu.shared.network.retrofit.RetrofitProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 视频数据层依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object VideoDataModule {

    @Provides
    @Singleton
    fun provideVideoFeedApiService(
        retrofit: Retrofit
    ): VideoFeedApiService {
        return RetrofitProvider.createService(retrofit)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VideoDataBindsModule {

    @Binds
    @Singleton
    abstract fun bindVideoRemoteDataSource(
        impl: VideoRemoteDataSourceImpl
    ): VideoRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindVideoLocalDataSource(
        impl: VideoLocalDataSourceImpl
    ): VideoLocalDataSource

    @Binds
    @Singleton
    abstract fun bindVideoInteractionLocalDataSource(
        impl: VideoInteractionLocalDataSourceImpl
    ): VideoInteractionLocalDataSource

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        impl: VideoRepositoryImpl
    ): VideoRepository
}

