package com.ucw.beatu.business.settings.domain.model

/**
 * 用户设置聚合模型，统一由 Data 层管理持久化。
 */
data class SettingsPreference(
    val aiSearchEnabled: Boolean = false,
    val aiCommentEnabled: Boolean = false,
    val autoPlayEnabled: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val defaultQuality: PlaybackQualityPreference = PlaybackQualityPreference.AUTO
)


