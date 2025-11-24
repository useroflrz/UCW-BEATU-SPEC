package com.ucw.beatu.business.settings.data.repository

import com.ucw.beatu.business.settings.domain.model.PlaybackQualityPreference
import com.ucw.beatu.business.settings.domain.model.SettingsPreference
import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<SettingsPreference> {
        val aiSearchFlow = preferencesDataStore.getBoolean(Keys.AI_SEARCH, false)
        val aiCommentFlow = preferencesDataStore.getBoolean(Keys.AI_COMMENT, false)
        val autoPlayFlow = preferencesDataStore.getBoolean(Keys.AUTO_PLAY, true)
        val defaultSpeedFlow = preferencesDataStore.getFloat(Keys.DEFAULT_SPEED, 1.0f)
        val qualityFlow = preferencesDataStore.getString(Keys.DEFAULT_QUALITY, PlaybackQualityPreference.AUTO.storageValue)

        return combine(
            aiSearchFlow,
            aiCommentFlow,
            autoPlayFlow,
            defaultSpeedFlow,
            qualityFlow
        ) { aiSearch, aiComment, autoPlay, speed, quality ->
            SettingsPreference(
                aiSearchEnabled = aiSearch,
                aiCommentEnabled = aiComment,
                autoPlayEnabled = autoPlay,
                defaultSpeed = speed,
                defaultQuality = PlaybackQualityPreference.fromStorageValue(quality)
            )
        }.distinctUntilChanged()
    }

    override suspend fun updateAiSearch(enabled: Boolean) {
        persist { preferencesDataStore.putBoolean(Keys.AI_SEARCH, enabled) }
    }

    override suspend fun updateAiComment(enabled: Boolean) {
        persist { preferencesDataStore.putBoolean(Keys.AI_COMMENT, enabled) }
    }

    override suspend fun updateAutoPlay(enabled: Boolean) {
        persist { preferencesDataStore.putBoolean(Keys.AUTO_PLAY, enabled) }
    }

    override suspend fun updateDefaultSpeed(speed: Float) {
        persist { preferencesDataStore.putFloat(Keys.DEFAULT_SPEED, speed) }
    }

    override suspend fun updateDefaultQuality(quality: PlaybackQualityPreference) {
        persist { preferencesDataStore.putString(Keys.DEFAULT_QUALITY, quality.storageValue) }
    }

    private suspend fun persist(block: suspend () -> Unit) {
        withContext(Dispatchers.IO) { block() }
    }

    private object Keys {
        const val AI_SEARCH = "settings_ai_search"
        const val AI_COMMENT = "settings_ai_comment"
        const val AUTO_PLAY = "settings_auto_play"
        const val DEFAULT_SPEED = "settings_default_speed"
        const val DEFAULT_QUALITY = "settings_default_quality"
    }
}


