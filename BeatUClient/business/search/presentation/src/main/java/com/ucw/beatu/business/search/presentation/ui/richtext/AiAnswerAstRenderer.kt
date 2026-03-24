package com.ucw.beatu.business.search.presentation.ui.richtext

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UpdateAppearance
import android.graphics.Typeface
import io.noties.markwon.Markwon
import kotlin.math.max

/**
 * AI 回答渲染器（单块）：每次只渲染一个 markdown block。
 */
object AiAnswerAstRenderer {

    @Volatile
    private var markwon: Markwon? = null

    fun renderBlock(
        context: Context,
        blockMarkdown: String,
        keywords: Collection<String> = emptyList(),
        onKeywordClick: ((String) -> Unit)? = null
    ): CharSequence {
        if (blockMarkdown.isBlank()) return ""
        val builder = SpannableStringBuilder(getMarkwon(context).toMarkdown(blockMarkdown))
        applyKeywordHighlight(builder, 0, builder.length, keywords, onKeywordClick)
        return builder
    }

    fun renderArtTitle(text: String): CharSequence {
        if (text.isBlank()) return text
        val styled = SpannableString(text)
        styled.setSpan(
            GradientTextSpan(
                intArrayOf(0xFF7C4DFF.toInt(), 0xFF00E5FF.toInt(), 0xFF69F0AE.toInt())
            ),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        styled.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return styled
    }

    fun reset() {
        markwon = null
    }

    private fun getMarkwon(context: Context): Markwon {
        markwon?.let { return it }
        return synchronized(this) {
            markwon ?: Markwon.create(context.applicationContext).also { markwon = it }
        }
    }

    private fun applyKeywordHighlight(
        builder: SpannableStringBuilder,
        start: Int,
        end: Int,
        keywords: Collection<String>,
        onKeywordClick: ((String) -> Unit)?
    ) {
        if (keywords.isEmpty() || start >= end) return
        val text = builder.subSequence(start, end).toString()
        keywords
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .forEach { keyword ->
                var cursor = 0
                while (cursor < text.length) {
                    val idx = text.indexOf(keyword, startIndex = cursor, ignoreCase = true)
                    if (idx < 0) break
                    val globalStart = start + idx
                    val globalEnd = globalStart + keyword.length
                    builder.setSpan(
                        ForegroundColorSpan(0xFFFFC107.toInt()),
                        globalStart,
                        globalEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    if (onKeywordClick != null) {
                        builder.setSpan(
                            KeywordClickableSpan(keyword, onKeywordClick),
                            globalStart,
                            globalEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    cursor = max(idx + keyword.length, cursor + 1)
                }
            }
    }

    private class KeywordClickableSpan(
        private val keyword: String,
        private val onKeywordClick: (String) -> Unit
    ) : ClickableSpan() {
        override fun onClick(widget: android.view.View) {
            onKeywordClick(keyword)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
            ds.color = 0xFFFFC107.toInt()
        }
    }

    private class GradientTextSpan(
        private val colors: IntArray
    ) : CharacterStyle(), UpdateAppearance {
        override fun updateDrawState(tp: TextPaint) {
            val width = tp.measureText("AI回答").coerceAtLeast(1f)
            tp.shader = LinearGradient(
                0f,
                0f,
                width,
                0f,
                colors,
                null,
                Shader.TileMode.CLAMP
            )
        }
    }
}

