package com.ucw.beatu.business.settings.domain.usecase

import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
class UpdateDefaultSpeedUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(speed: Float) = repository.updateDefaultSpeed(speed)
}


