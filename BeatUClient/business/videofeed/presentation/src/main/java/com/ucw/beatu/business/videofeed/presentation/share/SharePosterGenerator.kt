package com.ucw.beatu.business.videofeed.presentation.share

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.graphics.applyCanvas
import kotlin.math.min

/**
 * 生成“封面 + 二维码”分享图的工具。
 *
 * 设计思路（适合分享到聊天 / 朋友圈）：
 * - 整体竖版海报（例如 720 x 1280）
 * - 上方大区域绘制视频封面截图
 * - 下方白色信息栏：标题、作者、二维码 + 提示文案
 */
object SharePosterGenerator {

    private const val POSTER_WIDTH = 720
    private const val POSTER_HEIGHT = 1280

    /**
     * @param context  用于拿到资源中的颜色 / 字体等
     * @param coverView 当前正在展示视频封面的 View（如 PlayerView 或 ImageView），会直接截图
     * @param title    视频标题
     * @param author   作者 / 频道名称
     * @param shareUrl 用于生成二维码的链接（可以是 H5 或 App Deeplink）
     */
    fun generate(
        context: Context,
        coverView: View,
        title: String,
        author: String,
        shareUrl: String
    ): Bitmap {
        // 1. 创建整张海报 Bitmap
        val poster = Bitmap.createBitmap(
            POSTER_WIDTH,
            POSTER_HEIGHT,
            Bitmap.Config.ARGB_8888
        )

        // 2. 先截取封面 View 的 Bitmap
        val coverBitmap = captureViewBitmap(coverView)

        // 3. 生成二维码 Bitmap
        val qrSize = (POSTER_WIDTH * 0.25f).toInt() // 宽度的 25%
        val qrBitmap = QrCodeGenerator.generate(shareUrl, qrSize)

        // 4. 用 Canvas 合成
        val bgColor = Color.parseColor("#F5F5F5")
        val white = Color.WHITE
        val black = Color.parseColor("#222222")
        val gray = Color.parseColor("#666666")

        poster.applyCanvas {
            drawColor(bgColor)

            val padding = 32f

            // 4.1 绘制上半部分封面（带圆角）
            val coverTop = padding
            val coverLeft = padding
            val coverRight = POSTER_WIDTH - padding
            val coverBottom = POSTER_HEIGHT * 0.7f

            val coverRect = RectF(coverLeft, coverTop, coverRight, coverBottom)

            // 先画一个圆角矩形裁剪区域
            val radius = 24f
            val path = Path().apply {
                addRoundRect(coverRect, radius, radius, Path.Direction.CW)
            }
            save()
            clipPath(path)

            coverBitmap?.let { src ->
                val srcRatio = src.width.toFloat() / src.height
                val dstRatio = coverRect.width() / coverRect.height()

                val dst = RectF(coverRect)

                val drawRect = if (srcRatio > dstRatio) {
                    // 源偏宽：按高度等比缩放，左右裁剪
                    val scale = coverRect.height() / src.height
                    val drawWidth = src.width * scale
                    RectF(
                        coverRect.centerX() - drawWidth / 2,
                        coverRect.top,
                        coverRect.centerX() + drawWidth / 2,
                        coverRect.bottom
                    )
                } else {
                    // 源偏高：按宽度等比缩放，上下裁剪
                    val scale = coverRect.width() / src.width
                    val drawHeight = src.height * scale
                    RectF(
                        coverRect.left,
                        coverRect.centerY() - drawHeight / 2,
                        coverRect.right,
                        coverRect.centerY() + drawHeight / 2
                    )
                }

                val srcRect = Rect(0, 0, src.width, src.height)
                drawBitmap(src, srcRect, drawRect, null)
            }

            restore()

            // 4.2 绘制下半部分信息白底
            val infoTop = coverBottom + 24f
            val infoRect = RectF(
                padding,
                infoTop,
                POSTER_WIDTH - padding,
                POSTER_HEIGHT - padding
            )

            val infoRadius = 24f
            val infoPath = Path().apply {
                addRoundRect(infoRect, infoRadius, infoRadius, Path.Direction.CW)
            }
            val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = white
            }
            drawPath(infoPath, infoPaint)

            // 4.3 在信息区里绘制标题、作者和二维码
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

            val maxTitleWidth = infoRect.width() * 0.6f
            val titleLines = breakTextToLines(title, titlePaint, maxTitleWidth, maxLines = 2)
            for (line in titleLines) {
                drawText(line, textStartX, textBaseline, titlePaint)
                textBaseline += titlePaint.textSize + 12f
            }

            textBaseline += 4f
            drawText(author, textStartX, textBaseline, authorPaint)

            // 4.4 右侧绘制二维码及说明
            val qrLeft = infoRect.right - contentPadding - qrSize
            val qrTop = infoRect.top + contentPadding
            val qrRect = RectF(
                qrLeft,
                qrTop,
                qrLeft + qrSize,
                qrTop + qrSize
            )

            drawBitmap(qrBitmap, null, qrRect, null)

            val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = gray
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            drawText(
                "扫码查看视频",
                qrRect.centerX(),
                qrRect.bottom + 40f,
                hintPaint
            )
        }

        return poster
    }

    private fun captureViewBitmap(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) return null
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)
        return bmp
    }

    /**
     * 将一段文本按最大宽度拆成多行，简单处理中文/英文混排即可。
     */
    private fun breakTextToLines(
        text: String,
        paint: Paint,
        maxWidth: Float,
        maxLines: Int
    ): List<String> {
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        var start = 0
        val len = text.length

        while (start < len && result.size < maxLines) {
            val count = paint.breakText(
                text,
                start,
                len,
                true,
                maxWidth,
                null
            )
            if (count <= 0) break
            val end = start + count
            result.add(text.substring(start, end))
            start = end
        }

        // 如果被截断，最后一行末尾加省略号
        if (start < len && result.isNotEmpty()) {
            val last = result.last()
            val ellipsis = "…"
            var newLast = last + ellipsis
            while (paint.measureText(newLast) > maxWidth && newLast.isNotEmpty()) {
                newLast = newLast.dropLast(2) + ellipsis
            }
            result[result.lastIndex] = newLast
        }

        return result
    }
}


