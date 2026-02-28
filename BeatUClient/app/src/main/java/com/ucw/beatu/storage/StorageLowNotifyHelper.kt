package com.ucw.beatu.storage

import android.content.Context
import android.widget.Toast

/**
 * 存储空间低提醒：在合适时机展示 Toast 或通知。
 */
object StorageLowNotifyHelper {

    private const val PREF_NAME = "storage_low_prefs"
    private const val KEY_LAST_NOTIFY_TIME = "last_notify_time"
    private const val NOTIFY_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 小时内不重复提醒

    /**
     * 若存储空间低且距上次提醒超过冷却时间，则显示 Toast
     */
    fun notifyIfLow(context: Context) {
        if (!StorageMonitor.isStorageLow(context)) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_NOTIFY_TIME, 0L)
        if (now - last < NOTIFY_COOLDOWN_MS) return
        prefs.edit().putLong(KEY_LAST_NOTIFY_TIME, now).apply()

        val availableMb = StorageMonitor.getAvailableSpaceMb(context)
        val cacheMb = StorageMonitor.getCacheDirSizeMb(context)
        Toast.makeText(
            context,
            "存储空间不足（剩余 ${availableMb}MB），建议清理缓存以释放空间",
            Toast.LENGTH_LONG
        ).show()
    }
}
