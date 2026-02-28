package com.ucw.beatu.ai

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 四大组件-Service：异步持久化 AI 搜索结果摘要。
 */
class AiSearchEventService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AiSearchComponentContract.ACTION_PERSIST_AI_SEARCH) {
            val query = intent.getStringExtra(AiSearchComponentContract.EXTRA_QUERY).orEmpty()
            val answer = intent.getStringExtra(AiSearchComponentContract.EXTRA_ANSWER).orEmpty()
            val keywords = intent.getStringArrayExtra(AiSearchComponentContract.EXTRA_KEYWORDS)?.toList().orEmpty()
            AiSearchHistoryStore.append(
                context = applicationContext,
                query = query,
                answer = answer,
                keywords = keywords
            )
        }
        stopSelfResult(startId)
        return START_NOT_STICKY
    }
}

