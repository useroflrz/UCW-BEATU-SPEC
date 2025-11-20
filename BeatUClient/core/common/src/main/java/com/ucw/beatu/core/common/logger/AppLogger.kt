package com.ucw.beatu.core.common.logger

import android.util.Log

object AppLogger {
    private const val GLOBAL_TAG = "BeatU"

    var isDebug: Boolean = true

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebug) Log.d(composeTag(tag), message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        Log.i(composeTag(tag), message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(composeTag(tag), message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(composeTag(tag), message, throwable)
    }

    private fun composeTag(tag: String): String = "$GLOBAL_TAG-$tag"
}
