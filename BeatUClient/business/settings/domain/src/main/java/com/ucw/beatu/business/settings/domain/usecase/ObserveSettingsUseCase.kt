package com.ucw.beatu.business.settings.domain.usecase

import com.ucw.beatu.business.settings.domain.model.SettingsPreference
import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<SettingsPreference> = repository.observeSettings()
}


