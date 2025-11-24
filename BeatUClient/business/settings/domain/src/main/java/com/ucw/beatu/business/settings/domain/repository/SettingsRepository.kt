package com.ucw.beatu.business.settings.domain.repository

import com.ucw.beatu.business.settings.domain.model.PlaybackQualityPreference
import com.ucw.beatu.business.settings.domain.model.SettingsPreference
import kotlinx.coroutines.flow.Flow

/**
 * 设置模块仓储接口，抽象 DataStore/网络等实现。
 */
interface SettingsRepository {
    fun observeSettings(): Flow<SettingsPreference>
    suspend fun updateAiSearch(enabled: Boolean)
    suspend fun updateAiComment(enabled: Boolean)
    suspend fun updateAutoPlay(enabled: Boolean)
    suspend fun updateDefaultSpeed(speed: Float)
    suspend fun updateDefaultQuality(quality: PlaybackQualityPreference)
}


