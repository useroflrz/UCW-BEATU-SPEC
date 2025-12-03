package com.ucw.beatu.business.videofeed.presentation.di

import android.content.Context
import com.ucw.beatu.R
import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
import com.ucw.beatu.shared.player.model.VideoPlayerConfig
import com.ucw.beatu.shared.player.pool.VideoPlayerPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VideoFeedPresentationModule {

    @Provides
    @Singleton
    fun provideVideoPlayerPool(
        @ApplicationContext context: Context,
        config: VideoPlayerConfig
    ): VideoPlayerPool {
        return VideoPlayerPool(context, config)
    }

    @Provides
    @Singleton
    fun provideVideoPlayerConfig(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): VideoPlayerConfig {
        // 从Settings模块读取默认配置
        val settings = runBlocking {
            settingsRepository.observeSettings().first()
        }
        
        // 从config.xml读取配置
        val connectTimeoutMs = context.resources.getInteger(R.integer.video_connect_timeout_ms)
        val readTimeoutMs = context.resources.getInteger(R.integer.video_read_timeout_ms)
        val offscreenPreloadCount = context.resources.getInteger(R.integer.video_offscreen_preload_count)
        val maxReusablePlayers = context.resources.getInteger(R.integer.video_player_pool_max_reusable)
        
        return VideoPlayerConfig(
            useCache = true,
            defaultSpeed = settings.defaultSpeed,
            offscreenPreloadCount = offscreenPreloadCount,
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs,
            maxReusablePlayers = maxReusablePlayers
        )
    }
}

