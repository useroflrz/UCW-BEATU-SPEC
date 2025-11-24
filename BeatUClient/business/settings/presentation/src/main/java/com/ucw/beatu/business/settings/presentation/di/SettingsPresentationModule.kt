package com.ucw.beatu.business.settings.presentation.di

import com.ucw.beatu.business.settings.domain.repository.SettingsRepository
import com.ucw.beatu.business.settings.domain.usecase.ObserveSettingsUseCase
import com.ucw.beatu.business.settings.domain.usecase.SettingsUseCases
import com.ucw.beatu.business.settings.domain.usecase.UpdateAiCommentSettingUseCase
import com.ucw.beatu.business.settings.domain.usecase.UpdateAiSearchSettingUseCase
import com.ucw.beatu.business.settings.domain.usecase.UpdateAutoPlaySettingUseCase
import com.ucw.beatu.business.settings.domain.usecase.UpdateDefaultQualityUseCase
import com.ucw.beatu.business.settings.domain.usecase.UpdateDefaultSpeedUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object SettingsPresentationModule {

    @Provides
    fun provideSettingsUseCases(
        repository: SettingsRepository
    ): SettingsUseCases {
        return SettingsUseCases(
            observeSettings = ObserveSettingsUseCase(repository),
            updateAiSearch = UpdateAiSearchSettingUseCase(repository),
            updateAiComment = UpdateAiCommentSettingUseCase(repository),
            updateAutoPlay = UpdateAutoPlaySettingUseCase(repository),
            updateDefaultSpeed = UpdateDefaultSpeedUseCase(repository),
            updateDefaultQuality = UpdateDefaultQualityUseCase(repository)
        )
    }
}


