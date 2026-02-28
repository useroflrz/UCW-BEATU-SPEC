package com.ucw.beatu.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 增长触发 Receiver：
 * - 监听 BOOT_COMPLETED / CONNECTIVITY_CHANGE / MY_PACKAGE_REPLACED；
 * - 做节流与幂等，避免短时间重复触发。
 */
class AiGrowthTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (!shouldTrigger(context, action)) return
        val triggerIntent = Intent(context, AiGrowthEngagementService::class.java).apply {
            this.action = AiSearchComponentContract.ACTION_TRIGGER_GROWTH_ENGAGEMENT
            putExtra(AiSearchComponentContract.EXTRA_TRIGGER, action)
        }
        context.startService(triggerIntent)
    }

    private fun shouldTrigger(context: Context, action: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "last_$action"
        val now = System.currentTimeMillis()
        val last = prefs.getLong(key, 0L)
        if (now - last < TRIGGER_THROTTLE_MS) return false
        prefs.edit().putLong(key, now).apply()
        return true
    }

    companion object {
        private const val PREF_NAME = "ai_growth_trigger_prefs"
        private const val TRIGGER_THROTTLE_MS = 10 * 60 * 1000L
    }
}

