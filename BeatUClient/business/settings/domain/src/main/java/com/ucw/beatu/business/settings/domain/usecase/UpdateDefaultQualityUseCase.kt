package com.ucw.beatu.business.settings.domain.usecase

import com.ucw.beatu.business.settings.domain.model.PlaybackQualityPreference
import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
class UpdateDefaultQualityUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(quality: PlaybackQualityPreference) =
        repository.updateDefaultQuality(quality)
}


