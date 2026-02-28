package com.ucw.beatu.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 四大组件-BroadcastReceiver：接收 AI 搜索结果广播并转存到 Service。
 */
class AiSearchEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AiSearchComponentContract.ACTION_AI_SEARCH_RESULT) return
        val serviceIntent = Intent(context, AiSearchEventService::class.java).apply {
            action = AiSearchComponentContract.ACTION_PERSIST_AI_SEARCH
            putExtra(
                AiSearchComponentContract.EXTRA_QUERY,
                intent.getStringExtra(AiSearchComponentContract.EXTRA_QUERY).orEmpty()
            )
            putExtra(
                AiSearchComponentContract.EXTRA_ANSWER,
                intent.getStringExtra(AiSearchComponentContract.EXTRA_ANSWER).orEmpty()
            )
            putExtra(
                AiSearchComponentContract.EXTRA_KEYWORDS,
                intent.getStringArrayExtra(AiSearchComponentContract.EXTRA_KEYWORDS)
            )
        }
        context.startService(serviceIntent)
    }
}

