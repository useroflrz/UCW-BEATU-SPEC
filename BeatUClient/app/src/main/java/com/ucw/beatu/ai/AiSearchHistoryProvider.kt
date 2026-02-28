package com.ucw.beatu.ai

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * 四大组件-ContentProvider：对内提供 AI 搜索历史查询能力。
 */
class AiSearchHistoryProvider : ContentProvider() {

    private lateinit var matcher: UriMatcher

    override fun onCreate(): Boolean {
        matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, AiSearchComponentContract.PATH_HISTORY, CODE_HISTORY)
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
        if (matcher.match(uri) != CODE_HISTORY) {
            throw IllegalArgumentException("Unsupported uri: $uri")
        }
        val columns = arrayOf(COL_ID, COL_QUERY, COL_ANSWER_PREVIEW, COL_KEYWORDS, COL_TIMESTAMP)
        val cursor = MatrixCursor(columns)
        val context = context ?: return cursor
        val items = AiSearchHistoryStore.readAll(context)
        items.forEachIndexed { index, item ->
            cursor.addRow(
                arrayOf(
                    index.toLong(),
                    item.query,
                    item.answerPreview,
                    item.keywords.joinToString("、"),
                    item.timestamp
                )
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String {
        return when (matcher.match(uri)) {
            CODE_HISTORY -> "vnd.android.cursor.dir/vnd.$authority.${AiSearchComponentContract.PATH_HISTORY}"
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
        get() = "${context?.packageName.orEmpty()}${AiSearchComponentContract.AUTHORITY_SUFFIX}"

    companion object {
        private const val CODE_HISTORY = 1001
        const val COL_ID = "_id"
        const val COL_QUERY = "query"
        const val COL_ANSWER_PREVIEW = "answer_preview"
        const val COL_KEYWORDS = "keywords"
        const val COL_TIMESTAMP = "timestamp"
    }
}

