package com.ucw.beatu.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AiSearchHistoryEntry(
    val query: String,
    val answerPreview: String,
    val keywords: List<String>,
    val timestamp: Long
)

object AiSearchHistoryStore {
    private const val PREF_NAME = "ai_search_component_prefs"
    private const val KEY_HISTORY = "ai_search_history"
    private const val MAX_ITEMS = 30

    fun append(
        context: Context,
        query: String,
        answer: String,
        keywords: List<String>
    ) {
        if (query.isBlank() || answer.isBlank()) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existing = readAll(context).toMutableList()
        val preview = answer.trim().take(220)
        existing.add(
            0,
            AiSearchHistoryEntry(
                query = query.trim(),
                answerPreview = preview,
                keywords = keywords,
                timestamp = System.currentTimeMillis()
            )
        )
        val dedup = existing
            .distinctBy { "${it.query}|${it.answerPreview}" }
            .take(MAX_ITEMS)
        prefs.edit().putString(KEY_HISTORY, toJson(dedup)).apply()
    }

    fun readAll(context: Context): List<AiSearchHistoryEntry> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "[]").orEmpty()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        AiSearchHistoryEntry(
                            query = obj.optString("query"),
                            answerPreview = obj.optString("answerPreview"),
                            keywords = obj.optJSONArray("keywords")?.let(::toKeywords) ?: emptyList(),
                            timestamp = obj.optLong("timestamp")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun toJson(items: List<AiSearchHistoryEntry>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("query", item.query)
                    .put("answerPreview", item.answerPreview)
                    .put("keywords", JSONArray(item.keywords))
                    .put("timestamp", item.timestamp)
            )
        }
        return arr.toString()
    }

    private fun toKeywords(array: JSONArray): List<String> {
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }
}

