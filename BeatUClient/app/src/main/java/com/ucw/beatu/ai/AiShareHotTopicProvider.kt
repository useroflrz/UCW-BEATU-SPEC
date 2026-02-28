package com.ucw.beatu.ai

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log

/**
 * 增长向 Provider：向外部分享链路提供只读热门话题摘要。
 */
class AiShareHotTopicProvider : ContentProvider() {

    private lateinit var matcher: UriMatcher

    override fun onCreate(): Boolean {
        matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, AiSearchComponentContract.PATH_SHARE_HOT, CODE_SHARE_HOT)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (matcher.match(uri) != CODE_SHARE_HOT) {
            throw IllegalArgumentException("Unsupported uri: $uri")
        }
        val columns = arrayOf(COL_ID, COL_TOPIC, COL_SOURCE, COL_CLICK_URL)
        val cursor = MatrixCursor(columns)
        val context = context ?: return cursor
        val history = AiSearchHistoryStore.readAll(context)
        val dynamicTopic = history.firstOrNull()?.query?.takeIf { it.isNotBlank() }
        val topics = buildList {
            add(dynamicTopic ?: "AI创作灵感")
            add("提升视频完播率")
            add("新手短视频脚本")
        }.distinct()
        topics.forEachIndexed { index, topic ->
            cursor.addRow(
                arrayOf(
                    index.toLong(),
                    topic,
                    "provider",
                    "beatu://ai/hot?q=${Uri.encode(topic)}"
                )
            )
        }
        Log.d(TAG, "share_provider_query_success: topicCount=${topics.size}")
        return cursor
    }

    override fun getType(uri: Uri): String {
        return when (matcher.match(uri)) {
            CODE_SHARE_HOT -> "vnd.android.cursor.dir/vnd.$authority.${AiSearchComponentContract.PATH_SHARE_HOT}"
            else -> throw IllegalArgumentException("Unsupported uri: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    private val authority: String
        get() = "${context?.packageName.orEmpty()}.ai.share.provider"

    companion object {
        private const val TAG = "AiShareTopicProvider"
        private const val CODE_SHARE_HOT = 2001
        const val COL_ID = "_id"
        const val COL_TOPIC = "topic"
        const val COL_SOURCE = "source"
        const val COL_CLICK_URL = "click_url"
    }
}

