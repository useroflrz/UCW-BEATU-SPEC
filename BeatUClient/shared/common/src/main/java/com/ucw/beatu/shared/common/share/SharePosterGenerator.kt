package com.ucw.beatu.shared.common.share
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.graphics.applyCanvas

object SharePosterGenerator {
    private const val POSTER_WIDTH = 720
    private const val POSTER_HEIGHT = 1280

    fun generate(
        coverView: View,
        title: String,
        author: String,
        shareUrl: String
    ): Bitmap {
        val poster = Bitmap.createBitmap(POSTER_WIDTH, POSTER_HEIGHT, Bitmap.Config.ARGB_8888)
        val coverBitmap = captureViewBitmap(coverView)
        val qrSize = (POSTER_WIDTH * 0.25f).toInt()
        val qrBitmap = QrCodeGenerator.generate(shareUrl, qrSize)

        val bgColor = Color.parseColor("#F5F5F5")
        val white = Color.WHITE
        val black = Color.parseColor("#222222")
        val gray = Color.parseColor("#666666")

        poster.applyCanvas {
            drawColor(bgColor)
            val padding = 32f
            val coverRect = RectF(padding, padding, POSTER_WIDTH - padding, POSTER_HEIGHT * 0.7f)
            val clipPath = Path().apply { addRoundRect(coverRect, 24f, 24f, Path.Direction.CW) }
            save()
            clipPath(clipPath)
            coverBitmap?.let { src ->
                val srcRatio = src.width.toFloat() / src.height
                val dstRatio = coverRect.width() / coverRect.height()
                val drawRect = if (srcRatio > dstRatio) {
                    val scale = coverRect.height() / src.height
                    val drawWidth = src.width * scale
                    RectF(
                        coverRect.centerX() - drawWidth / 2,
                        coverRect.top,
                        coverRect.centerX() + drawWidth / 2,
                        coverRect.bottom
                    )
                } else {
                    val scale = coverRect.width() / src.width
                    val drawHeight = src.height * scale
                    RectF(
                        coverRect.left,
                        coverRect.centerY() - drawHeight / 2,
                        coverRect.right,
                        coverRect.centerY() + drawHeight / 2
                    )
                }
                drawBitmap(src, Rect(0, 0, src.width, src.height), drawRect, null)
            }
            restore()

            val infoRect = RectF(padding, coverRect.bottom + 24f, POSTER_WIDTH - padding, POSTER_HEIGHT - padding)
            val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white }
            val infoPath = Path().apply { addRoundRect(infoRect, 24f, 24f, Path.Direction.CW) }
            drawPath(infoPath, infoPaint)

            val contentPadding = 32f
            val textStartX = infoRect.left + contentPadding
            var textBaseline = infoRect.top + contentPadding + 4f
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = black
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            }
            val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gray
                textSize = 32f
            }
            val titleLines = breakTextToLines(title, titlePaint, infoRect.width() * 0.6f, 2)
            for (line in titleLines) {
                drawText(line, textStartX, textBaseline, titlePaint)
                textBaseline += titlePaint.textSize + 12f
            }
            textBaseline += 4f
            drawText(author, textStartX, textBaseline, authorPaint)

            val qrLeft = infoRect.right - contentPadding - qrSize
            val qrTop = infoRect.top + contentPadding
            val qrRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            drawBitmap(qrBitmap, null, qrRect, null)
            val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gray
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            drawText("扫码查看视频", qrRect.centerX(), qrRect.bottom + 40f, hintPaint)
        }
        return poster
    }

    private fun captureViewBitmap(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) return null
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun breakTextToLines(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        val len = text.length
        while (start < len && result.size < maxLines) {
            val count = paint.breakText(text, start, len, true, maxWidth, null)
            if (count <= 0) break
            val end = start + count
            result.add(text.substring(start, end))
            start = end
        }
        if (start < len && result.isNotEmpty()) {
            var last = result.last() + "…"
            while (paint.measureText(last) > maxWidth && last.length > 1) {
                last = last.dropLast(2) + "…"
            }
            result[result.lastIndex] = last
        }
        return result
    }
}
