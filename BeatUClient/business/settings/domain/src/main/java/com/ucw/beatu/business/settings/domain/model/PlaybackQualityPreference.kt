package com.ucw.beatu.business.settings.domain.model

/**
 * 播放清晰度偏好（与 iOS 风格菜单保持一致）
 */
enum class PlaybackQualityPreference(val storageValue: String, val label: String) {
    AUTO("auto", "自动"),
    HD_1080("1080p", "1080P 高清"),
    HD_720("720p", "720P 准高清"),
    SD_480("480p", "480P 标清"),
    SD_360("360p", "360P 流畅");

    companion object {
        fun fromStorageValue(value: String?): PlaybackQualityPreference {
            if (value.isNullOrBlank()) return AUTO
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }
    }
}


