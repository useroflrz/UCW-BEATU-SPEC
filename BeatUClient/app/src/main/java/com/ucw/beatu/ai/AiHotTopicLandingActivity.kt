package com.ucw.beatu.ai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ucw.beatu.MainActivity
import com.ucw.beatu.R

/**
 * 增长落地页 Activity：
 * 承接 DeepLink / Push，展示热门话题并回流主搜索结果页。
 */
class AiHotTopicLandingActivity : AppCompatActivity() {

    private lateinit var tvOpenTopicA: TextView
    private lateinit var tvOpenTopicB: TextView
    private lateinit var tvOpenTopicC: TextView
    private lateinit var tvSource: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_hot_topic_landing)
        tvOpenTopicA = findViewById(R.id.tv_topic_a)
        tvOpenTopicB = findViewById(R.id.tv_topic_b)
        tvOpenTopicC = findViewById(R.id.tv_topic_c)
        tvSource = findViewById(R.id.tv_topic_source)

        val source = intent.getStringExtra(AiSearchComponentContract.EXTRA_SOURCE).orEmpty()
        val deepLinkQuery = intent.data?.getQueryParameter("q").orEmpty()
        tvSource.text = if (source.isBlank()) "来源：直接访问/深链" else "来源：$source"
        Log.d(TAG, "landing_open: source=${source.ifBlank { "direct" }}, deepLinkQuery=$deepLinkQuery")

        tvOpenTopicA.setOnClickListener { openSearch("AI创作灵感", source) }
        tvOpenTopicB.setOnClickListener { openSearch("提升视频完播率", source) }
        tvOpenTopicC.setOnClickListener { openSearch("新手短视频脚本", source) }

        if (deepLinkQuery.isNotBlank()) {
            openSearch(deepLinkQuery, "deep_link")
            return
        }
        val queryFromPush = intent.getStringExtra(AiSearchComponentContract.EXTRA_QUERY).orEmpty()
        if (queryFromPush.isNotBlank()) {
            openSearch(queryFromPush, source.ifBlank { "push" })
        }
    }

    private fun openSearch(query: String, source: String) {
        Log.d(TAG, "landing_click_topic: query=$query, source=$source")
        val intent = Intent(this, MainActivity::class.java).apply {
            action = AiSearchComponentContract.ACTION_OPEN_AI_TOPIC
            putExtra(AiSearchComponentContract.EXTRA_QUERY, query)
            putExtra(AiSearchComponentContract.EXTRA_SOURCE, source)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "AiHotTopicLanding"
    }
}

