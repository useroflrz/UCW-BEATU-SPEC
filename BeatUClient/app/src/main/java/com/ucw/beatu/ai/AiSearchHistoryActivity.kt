package com.ucw.beatu.ai

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ucw.beatu.R
import java.util.Date

/**
 * 四大组件-Activity：展示 AI 搜索历史（来自 ContentProvider）。
 */
class AiSearchHistoryActivity : AppCompatActivity() {

    private lateinit var tvHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_search_history)
        tvHistory = findViewById(R.id.tv_ai_search_history)
        loadHistory()
    }

    private fun loadHistory() {
        val uri = Uri.parse("content://${packageName}${AiSearchComponentContract.AUTHORITY_SUFFIX}/${AiSearchComponentContract.PATH_HISTORY}")
        val text = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.toDisplayText()
        } ?: "暂无 AI 搜索历史"
        tvHistory.text = text
    }

    private fun Cursor.toDisplayText(): CharSequence {
        if (!moveToFirst()) return "暂无 AI 搜索历史"
        val queryIndex = getColumnIndexOrThrow(AiSearchHistoryProvider.COL_QUERY)
        val previewIndex = getColumnIndexOrThrow(AiSearchHistoryProvider.COL_ANSWER_PREVIEW)
        val keywordsIndex = getColumnIndexOrThrow(AiSearchHistoryProvider.COL_KEYWORDS)
        val timestampIndex = getColumnIndexOrThrow(AiSearchHistoryProvider.COL_TIMESTAMP)

        val lines = StringBuilder("AI 搜索历史（长按 AI 回答区域可进入本页）\n\n")
        do {
            val query = getString(queryIndex)
            val preview = getString(previewIndex)
            val keywords = getString(keywordsIndex)
            val timestamp = getLong(timestampIndex)
            val time = DateFormat.format("MM-dd HH:mm", Date(timestamp))
            lines.append("【").append(time).append("】\n")
                .append("Query: ").append(query).append('\n')
                .append("关键词: ").append(if (keywords.isBlank()) "-" else keywords).append('\n')
                .append("摘要: ").append(preview).append("\n\n")
        } while (moveToNext())

        return lines.toString()
    }
}

