package com.ucw.beatu.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ucw.beatu.R

/**
 * 增长触达编排 Service：
 * 1) 承接系统触发（开机/网络恢复/升级）；
 * 2) 生成轻量召回通知（点击进入话题落地页）。
 */
class AiGrowthEngagementService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action.orEmpty()
        val trigger = intent?.getStringExtra(AiSearchComponentContract.EXTRA_TRIGGER).orEmpty()
        when (action) {
            AiSearchComponentContract.ACTION_TRIGGER_GROWTH_ENGAGEMENT,
            AiSearchComponentContract.ACTION_BOOTSTRAP_GROWTH -> {
                val normalizedTrigger = trigger.ifBlank { action }
                if (shouldEmit(normalizedTrigger)) {
                    pushTopicRecall(normalizedTrigger)
                } else {
                    Log.d(TAG, "skip growth recall due to cooldown, trigger=$normalizedTrigger")
                }
            }
        }
        stopSelfResult(startId)
        return START_NOT_STICKY
    }

    private fun pushTopicRecall(trigger: String) {
        createChannelIfNeeded()
        val query = mapTriggerToTopic(trigger)
        val landingIntent = Intent(this, AiHotTopicLandingActivity::class.java).apply {
            action = AiSearchComponentContract.ACTION_OPEN_AI_TOPIC
            putExtra(AiSearchComponentContract.EXTRA_QUERY, query)
            putExtra(AiSearchComponentContract.EXTRA_SOURCE, "growth_push:$trigger")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            query.hashCode(),
            landingIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("发现AI热门话题")
            .setContentText("点击查看“$query”并立即继续搜索")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "push growth recall, trigger=$trigger, query=$query")
    }

    private fun shouldEmit(trigger: String): Boolean {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val key = "emit_$trigger"
        val last = prefs.getLong(key, 0L)
        if (now - last < NOTIFICATION_COOLDOWN_MS) return false
        prefs.edit().putLong(key, now).apply()
        return true
    }

    private fun mapTriggerToTopic(trigger: String): String {
        return when {
            trigger.contains("BOOT_COMPLETED") -> "开机后AI灵感推荐"
            trigger.contains("CONNECTIVITY_CHANGE") -> "网络恢复后热门选题"
            trigger.contains("MY_PACKAGE_REPLACED") -> "版本升级新功能指南"
            trigger.contains("app_start") -> "AI创作灵感"
            else -> "今日AI热门话题"
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI增长触达",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "AI 搜索话题召回与活动触达"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "AiGrowthEngageSvc"
        private const val CHANNEL_ID = "ai_growth_engagement"
        private const val NOTIFICATION_ID = 60101
        private const val PREF_NAME = "ai_growth_service_prefs"
        private const val NOTIFICATION_COOLDOWN_MS = 30 * 60 * 1000L
    }
}

