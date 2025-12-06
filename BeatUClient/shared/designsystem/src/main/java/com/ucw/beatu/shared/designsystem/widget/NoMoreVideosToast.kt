package com.ucw.beatu.shared.designsystem.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 显示"没有更多视频"提示的 Toast 视图
 * 自动淡入淡出，几秒后自动消失
 */
class NoMoreVideosToast @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var textView: TextView? = null
    private var showAnimator: ValueAnimator? = null
    private var hideAnimator: ValueAnimator? = null
    private var autoHideRunnable: Runnable? = null

    companion object {
        private const val TEXT = "没有更多视频"
        private const val AUTO_HIDE_DELAY_MS = 2000L // 2秒后自动消失
        private const val ANIMATION_DURATION_MS = 300L
    }

    init {
        setupView()
    }

    private fun setupView() {
        // 创建 TextView
        textView = TextView(context).apply {
            text = TEXT
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(
                (32 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (32 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000")) // 半透明黑色背景
                cornerRadius = 24 * resources.displayMetrics.density
            }
        }

        // 添加到 FrameLayout
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        addView(textView, layoutParams)

        // 初始状态：不可见
        alpha = 0f
        visibility = View.GONE
    }

    /**
     * 显示提示，带淡入动画
     */
    fun show() {
        android.util.Log.d("NoMoreVideosToast", "show() called")
        // 取消之前的自动隐藏
        autoHideRunnable?.let { removeCallbacks(it) }
        autoHideRunnable = null

        // 取消之前的动画
        showAnimator?.cancel()
        hideAnimator?.cancel()

        // 显示视图
        visibility = View.VISIBLE
        bringToFront()
        android.util.Log.d("NoMoreVideosToast", "show() visibility=$visibility, parent=${parent != null}")

        // 淡入动画
        showAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                alpha = animation.animatedValue as Float
            }
            start()
        }

        // 设置自动隐藏
        autoHideRunnable = Runnable {
            hide()
        }
        postDelayed(autoHideRunnable!!, AUTO_HIDE_DELAY_MS)
    }

    /**
     * 隐藏提示，带淡出动画
     */
    fun hide() {
        // 取消自动隐藏
        autoHideRunnable?.let { removeCallbacks(it) }
        autoHideRunnable = null

        // 取消之前的动画
        showAnimator?.cancel()
        hideAnimator?.cancel()

        // 淡出动画
        hideAnimator = ValueAnimator.ofFloat(alpha, 0f).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                alpha = animation.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理资源
        showAnimator?.cancel()
        hideAnimator?.cancel()
        autoHideRunnable?.let { removeCallbacks(it) }
    }
}

