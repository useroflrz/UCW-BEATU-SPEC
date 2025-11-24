package com.ucw.beatu.business.settings.domain.usecase

import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
class UpdateAiSearchSettingUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.updateAiSearch(enabled)
}


