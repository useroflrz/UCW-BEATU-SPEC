package com.ucw.beatu.business.videofeed.presentation.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ucw.beatu.business.videofeed.presentation.R

/**
 * 水滴形状的Tab指示器
 * 默认状态：小圆点
 * 滑动时：像水滴一样拉长
 * 切换完成后：聚合成小圆点
 */
class TabIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt() // 白色
        style = Paint.Style.FILL
    }

    // 圆点半径
    private val dotRadius = 3f.dpToPx()
    
    // 位置进度：用于计算指示器的位置（0=起点，1=终点）
    private var positionProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    
    // 宽度进度：用于计算指示器的长度（0=圆点，1=完全拉长）
    private var widthProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    // 目标位置（相对于指示器View的坐标）
    private var targetX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    private var targetY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    // 起始位置（相对于指示器View的坐标）
    private var startX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    private var startY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    
    // Tab位置（用于直接定位）- 存储TextView的中心坐标
    private var followTabX: Float = 0f
    private var followTabY: Float = 0f
    private var recommendTabX: Float = 0f
    private var recommendTabY: Float = 0f
    private var meTabX: Float = 0f
    private var meTabY: Float = 0f

    // 动画
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        // 设置背景透明，只绘制指示器
        setBackgroundColor(0x00000000.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 计算当前X和Y位置（相对于View的坐标）- 使用位置进度进行线性插值
        val currentX = startX + (targetX - startX) * positionProgress
        val currentY = startY + (targetY - startY) * positionProgress
        
        // 确保坐标在View范围内
        val clampedX = currentX.coerceIn(dotRadius, width - dotRadius)
        val clampedY = currentY.coerceIn(dotRadius, height - dotRadius)
        
        if (widthProgress <= 0.01f) {
            // 绘制圆点（起点和终点都是圆点）
            canvas.drawCircle(clampedX, clampedY, dotRadius, paint)
        } else {
            // 绘制水滴形状（滑动时拉长）- 使用宽度进度控制长度
            drawDropletShape(canvas, clampedX, clampedY, widthProgress)
        }
    }

    /**
     * 绘制水滴形状
     * @param canvas 画布
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param progress 拉长进度（0-1）
     */
    private fun drawDropletShape(canvas: Canvas, centerX: Float, centerY: Float, progress: Float) {
        // 计算拉长的宽度（从圆点到完全拉长）
        val minWidth = dotRadius * 2
        val maxWidth = 28f.dpToPx() // 最大宽度
        val width = minWidth + (maxWidth - minWidth) * progress
        
        // 高度（保持较小，形成水滴效果）
        val height = dotRadius * 2 * (1f - progress * 0.2f) // 拉长时稍微变细
        
        // 使用Path绘制水滴形状（圆角矩形，两端是半圆）
        val path = Path()
        val left = centerX - width / 2
        val right = centerX + width / 2
        val top = centerY - height / 2
        val bottom = centerY + height / 2
        
        // 圆角半径（高度的一半，形成胶囊形状）
        val cornerRadius = height / 2
        
        // 绘制圆角矩形（胶囊形状，类似水滴拉长）
        path.addRoundRect(
            left, top, right, bottom,
            cornerRadius, cornerRadius,
            android.graphics.Path.Direction.CW
        )
        
        canvas.drawPath(path, paint)
    }

    /**
     * 设置滑动进度
     * @param positionProgress 位置进度值（0-1），用于计算指示器的位置，0表示在起点，1表示在终点
     * @param widthProgress 宽度进度值（0-1），用于计算指示器的长度，0表示圆点状态，1表示完全拉长状态
     * @param startX 起始位置的中心X坐标（相对于指示器View）
     * @param startY 起始位置的中心Y坐标（相对于指示器View）
     * @param targetX 目标位置的中心X坐标（相对于指示器View）
     * @param targetY 目标位置的中心Y坐标（相对于指示器View）
     */
    fun setScrollProgress(
        positionProgress: Float,
        widthProgress: Float,
        startX: Float,
        startY: Float,
        targetX: Float,
        targetY: Float
    ) {
        this.startX = startX
        this.startY = startY
        this.targetX = targetX
        this.targetY = targetY
        // 位置进度：线性插值，直接使用
        this.positionProgress = positionProgress.coerceIn(0f, 1f)
        // 宽度进度：当接近0时强制为0（确保圆点状态）
        this.widthProgress = if (widthProgress <= 0.01f) 0f else widthProgress.coerceIn(0f, 1f)
    }

    /**
     * 动画到目标位置（切换完成后聚合成圆点）
     */
    fun animateToTarget() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(positionProgress, 1f).apply {
            duration = 200
            addUpdateListener { animation ->
                this@TabIndicatorView.positionProgress = animation.animatedValue as Float
            }
            start()
        }
    }

    /**
     * 重置为圆点状态
     */
    fun resetToDot() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(widthProgress, 0f).apply {
            duration = 200
            addUpdateListener { animation ->
                this@TabIndicatorView.widthProgress = animation.animatedValue as Float
            }
            start()
        }
    }

    /**
     * 直接设置为圆点状态（无动画）
     */
    fun setToDot() {
        animator?.cancel()
        widthProgress = 0f
    }
    
    /**
     * 设置Tab位置（用于初始化）
     * @param followX 关注Tab的中心X坐标（相对于指示器View）
     * @param followY 关注Tab的中心Y坐标（相对于指示器View）
     * @param recommendX 推荐Tab的中心X坐标（相对于指示器View）
     * @param recommendY 推荐Tab的中心Y坐标（相对于指示器View）
     * @param meX 我Tab的中心X坐标（相对于指示器View）
     * @param meY 我Tab的中心Y坐标（相对于指示器View）
     */
    fun setTabPositions(followX: Float, followY: Float, recommendX: Float, recommendY: Float, meX: Float, meY: Float) {
        followTabX = followX
        followTabY = followY
        recommendTabX = recommendX
        recommendTabY = recommendY
        meTabX = meX
        meTabY = meY
        invalidate()
    }
    
    /**
     * 移动到指定Tab位置（用于页面切换完成时）
     */
    fun moveToTab(tabIndex: Int) {
        val (targetTabX, targetTabY) = when (tabIndex) {
            0 -> followTabX to followTabY
            1 -> recommendTabX to recommendTabY
            2 -> meTabX to meTabY
            else -> recommendTabX to recommendTabY
        }
        startX = targetTabX
        startY = targetTabY
        targetX = targetTabX
        targetY = targetTabY
        resetToDot()
    }

    /**
     * dp转px
     */
    private fun Float.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }
}

