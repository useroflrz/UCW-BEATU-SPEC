package com.ucw.beatu.business.videofeed.presentation.di

import android.content.Context
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

        // 从资源或默认值读取播放器配置（业务模块不直接依赖 app 的 R）
        val connectTimeoutMs = getIntegerResource(
            context,
            "video_connect_timeout_ms",
            DEFAULT_CONNECT_TIMEOUT_MS.toInt()
        )
        val readTimeoutMs = getIntegerResource(
            context,
            "video_read_timeout_ms",
            DEFAULT_READ_TIMEOUT_MS.toInt()
        )
        val offscreenPreloadCount = getIntegerResource(
            context,
            "video_offscreen_preload_count",
            DEFAULT_OFFSCREEN_PRELOAD_COUNT
        )
        val maxReusablePlayers = getIntegerResource(
            context,
            "video_player_pool_max_reusable",
            DEFAULT_MAX_REUSABLE_PLAYERS
        )
        
        return VideoPlayerConfig(
            useCache = true,
            defaultSpeed = settings.defaultSpeed,
            offscreenPreloadCount = offscreenPreloadCount,
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs,
            maxReusablePlayers = maxReusablePlayers
        )
    }

    private fun getIntegerResource(
        context: Context,
        name: String,
        defaultValue: Int
    ): Int {
        val resId = context.resources.getIdentifier(name, "integer", context.packageName)
        return if (resId != 0) context.resources.getInteger(resId) else defaultValue
    }

    private const val DEFAULT_CONNECT_TIMEOUT_MS = 1_000L
    private const val DEFAULT_READ_TIMEOUT_MS = 1_000L
    private const val DEFAULT_OFFSCREEN_PRELOAD_COUNT = 1
    private const val DEFAULT_MAX_REUSABLE_PLAYERS = 3
}

