package com.ucw.beatu.business.settings.domain.usecase

data class SettingsUseCases(
    val observeSettings: ObserveSettingsUseCase,
    val updateAiSearch: UpdateAiSearchSettingUseCase,
    val updateAiComment: UpdateAiCommentSettingUseCase,
    val updateAutoPlay: UpdateAutoPlaySettingUseCase,
    val updateDefaultSpeed: UpdateDefaultSpeedUseCase,
    val updateDefaultQuality: UpdateDefaultQualityUseCase
)


