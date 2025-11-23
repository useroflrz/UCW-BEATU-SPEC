package com.ucw.beatu.business.search.presentation.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * 流式布局 - 用于搜索标签自动换行
 */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var horizontalSpacing = 8
    private var verticalSpacing = 8

    init {
        horizontalSpacing = (8 * resources.displayMetrics.density).toInt()
        verticalSpacing = (8 * resources.displayMetrics.density).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var totalHeight = 0
        var currentLineWidth = 0
        var currentLineHeight = 0
        var maxWidth = 0

        val childCount = childCount

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) {
                continue
            }

            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (currentLineWidth + childWidth + horizontalSpacing > widthSize - paddingStart - paddingEnd) {
                // 需要换行
                totalHeight += currentLineHeight + verticalSpacing
                maxWidth = maxWidth.coerceAtLeast(currentLineWidth)
                currentLineWidth = childWidth
                currentLineHeight = childHeight
            } else {
                // 同一行
                if (currentLineWidth > 0) {
                    currentLineWidth += horizontalSpacing
                }
                currentLineWidth += childWidth
                currentLineHeight = currentLineHeight.coerceAtLeast(childHeight)
            }
        }

        totalHeight += currentLineHeight
        maxWidth = maxWidth.coerceAtLeast(currentLineWidth)

        val measuredWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> (maxWidth + paddingStart + paddingEnd).coerceAtMost(widthSize)
            else -> maxWidth + paddingStart + paddingEnd
        }

        val measuredHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> (totalHeight + paddingTop + paddingBottom).coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
            else -> totalHeight + paddingTop + paddingBottom
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = width - paddingStart - paddingEnd
        var currentLeft = paddingStart
        var currentTop = paddingTop
        var currentLineHeight = 0

        val childCount = childCount

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) {
                continue
            }

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (currentLeft + childWidth > width) {
                // 需要换行
                currentTop += currentLineHeight + verticalSpacing
                currentLeft = paddingStart
                currentLineHeight = childHeight
            } else {
                currentLineHeight = currentLineHeight.coerceAtLeast(childHeight)
            }

            child.layout(
                currentLeft,
                currentTop,
                currentLeft + childWidth,
                currentTop + childHeight
            )

            currentLeft += childWidth + horizontalSpacing
        }
    }
}
