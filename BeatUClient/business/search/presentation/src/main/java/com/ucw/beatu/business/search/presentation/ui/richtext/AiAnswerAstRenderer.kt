package com.ucw.beatu.business.search.presentation.ui.richtext

import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UpdateAppearance
import android.graphics.Typeface
import kotlin.math.max

/**
 * AI 回答渲染器：将文本解析为轻量 AST，并渲染为富文本。
 * 支持：
 * - 标题：# / ## / ###
 * - 粗体：**text**
 * - 代码：`code`
 * - 关键字高亮：按关键词表全局高亮（可点击）
 */
object AiAnswerAstRenderer {

    private sealed class AstNode {
        data class Title(val level: Int, val text: String) : AstNode()
        data class Paragraph(val chunks: List<Chunk>) : AstNode()
    }

    private sealed class Chunk {
        data class Plain(val text: String) : Chunk()
        data class Bold(val text: String) : Chunk()
        data class Code(val text: String) : Chunk()
    }

    fun render(
        answer: String,
        keywords: Collection<String> = emptyList(),
        onKeywordClick: ((String) -> Unit)? = null
    ): CharSequence {
        if (answer.isBlank()) return ""
        val builder = SpannableStringBuilder()
        val ast = parse(answer)
        ast.forEachIndexed { index, node ->
            when (node) {
                is AstNode.Title -> appendTitle(builder, node)
                is AstNode.Paragraph -> appendParagraph(builder, node, keywords, onKeywordClick)
            }
            if (index < ast.lastIndex) builder.append('\n')
        }
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

    private fun parse(input: String): List<AstNode> {
        return input
            .replace("\r\n", "\n")
            .split('\n')
            .map { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("### ") -> AstNode.Title(3, trimmed.removePrefix("### ").trim())
                    trimmed.startsWith("## ") -> AstNode.Title(2, trimmed.removePrefix("## ").trim())
                    trimmed.startsWith("# ") -> AstNode.Title(1, trimmed.removePrefix("# ").trim())
                    else -> AstNode.Paragraph(parseInline(line))
                }
            }
    }

    private fun parseInline(line: String): List<Chunk> {
        if (line.isEmpty()) return listOf(Chunk.Plain(""))
        val chunks = mutableListOf<Chunk>()
        var cursor = 0
        while (cursor < line.length) {
            when {
                line.startsWith("**", cursor) -> {
                    val end = line.indexOf("**", startIndex = cursor + 2)
                    if (end > cursor + 2) {
                        chunks += Chunk.Bold(line.substring(cursor + 2, end))
                        cursor = end + 2
                    } else {
                        chunks += Chunk.Plain(line.substring(cursor))
                        break
                    }
                }
                line[cursor] == '`' -> {
                    val end = line.indexOf('`', startIndex = cursor + 1)
                    if (end > cursor + 1) {
                        chunks += Chunk.Code(line.substring(cursor + 1, end))
                        cursor = end + 1
                    } else {
                        chunks += Chunk.Plain(line.substring(cursor))
                        break
                    }
                }
                else -> {
                    val nextBold = line.indexOf("**", startIndex = cursor).takeIf { it >= 0 } ?: line.length
                    val nextCode = line.indexOf('`', startIndex = cursor).takeIf { it >= 0 } ?: line.length
                    val next = minOf(nextBold, nextCode)
                    chunks += Chunk.Plain(line.substring(cursor, next))
                    cursor = next
                }
            }
        }
        return chunks
    }

    private fun appendTitle(builder: SpannableStringBuilder, title: AstNode.Title) {
        val start = builder.length
        builder.append(title.text)
        val end = builder.length
        val scale = when (title.level) {
            1 -> 1.24f
            2 -> 1.16f
            else -> 1.10f
        }
        builder.setSpan(RelativeSizeSpan(scale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(0xFF80DEEA.toInt()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun appendParagraph(
        builder: SpannableStringBuilder,
        paragraph: AstNode.Paragraph,
        keywords: Collection<String>,
        onKeywordClick: ((String) -> Unit)?
    ) {
        paragraph.chunks.forEach { chunk ->
            when (chunk) {
                is Chunk.Plain -> appendPlain(builder, chunk.text, keywords, onKeywordClick)
                is Chunk.Bold -> {
                    val start = builder.length
                    builder.append(chunk.text)
                    val end = builder.length
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(0xFFFFFFFF.toInt()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    applyKeywordHighlight(builder, start, end, keywords, onKeywordClick)
                }
                is Chunk.Code -> {
                    val start = builder.length
                    builder.append(chunk.text)
                    val end = builder.length
                    builder.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(0xFFBBDEFB.toInt()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun appendPlain(
        builder: SpannableStringBuilder,
        text: String,
        keywords: Collection<String>,
        onKeywordClick: ((String) -> Unit)?
    ) {
        val start = builder.length
        builder.append(text)
        val end = builder.length
        builder.setSpan(ForegroundColorSpan(0xFFF5F5F5.toInt()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        applyKeywordHighlight(builder, start, end, keywords, onKeywordClick)
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

