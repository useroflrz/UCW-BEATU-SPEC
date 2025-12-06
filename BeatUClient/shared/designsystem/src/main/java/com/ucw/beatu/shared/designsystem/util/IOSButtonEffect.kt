package com.ucw.beatu.shared.designsystem.util

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * iOS 风格的按钮交互动效工具类
 * 提供按下缩放和透明度变化效果
 */
object IOSButtonEffect {
    
    private const val SCALE_DOWN = 0.95f
    private const val ALPHA_DOWN = 0.7f
    private const val ANIMATION_DURATION = 150L
    
    /**
     * 为 View 添加 iOS 风格的点击效果
     * @param view 要添加效果的 View
     * @param onClickListener 点击监听器（可选）
     */
    fun applyIOSEffect(view: View, onClickListener: View.OnClickListener? = null) {
        var scaleAnimator: ObjectAnimator? = null
        var alphaAnimator: ObjectAnimator? = null
        
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 按下时：缩放 + 透明度变化
                    scaleAnimator?.cancel()
                    alphaAnimator?.cancel()
                    
                    scaleAnimator = ObjectAnimator.ofFloat(v, "scaleX", 1f, SCALE_DOWN).apply {
                        duration = ANIMATION_DURATION
                        interpolator = OvershootInterpolator(0.5f)
                    }
                    val scaleYAnimator = ObjectAnimator.ofFloat(v, "scaleY", 1f, SCALE_DOWN).apply {
                        duration = ANIMATION_DURATION
                        interpolator = OvershootInterpolator(0.5f)
                    }
                    
                    alphaAnimator = ObjectAnimator.ofFloat(v, "alpha", 1f, ALPHA_DOWN).apply {
                        duration = ANIMATION_DURATION
                    }
                    
                    scaleAnimator?.start()
                    scaleYAnimator.start()
                    alphaAnimator?.start()
                    
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 释放时：恢复原状
                    scaleAnimator?.cancel()
                    alphaAnimator?.cancel()
                    
                    val currentScaleX = v.scaleX
                    val currentScaleY = v.scaleY
                    val currentAlpha = v.alpha
                    
                    ObjectAnimator.ofFloat(v, "scaleX", currentScaleX, 1f).apply {
                        duration = ANIMATION_DURATION
                        interpolator = OvershootInterpolator(1.2f)
                        start()
                    }
                    ObjectAnimator.ofFloat(v, "scaleY", currentScaleY, 1f).apply {
                        duration = ANIMATION_DURATION
                        interpolator = OvershootInterpolator(1.2f)
                        start()
                    }
                    ObjectAnimator.ofFloat(v, "alpha", currentAlpha, 1f).apply {
                        duration = ANIMATION_DURATION
                        start()
                    }
                    
                    // 如果是在按钮范围内释放，触发点击事件
                    if (event.action == MotionEvent.ACTION_UP && 
                        event.x >= 0 && event.x <= v.width && 
                        event.y >= 0 && event.y <= v.height) {
                        // 只调用传入的监听器，避免重复触发
                        onClickListener?.onClick(v)
                        // 如果没有传入监听器，才调用 performClick
                        if (onClickListener == null) {
                            v.performClick()
                        }
                    }
                    
                    true
                }
                else -> false
            }
        }
        
        // 如果提供了点击监听器，也设置到 View 上（用于无障碍访问）
        // 但注意：实际的点击处理应该通过 onTouchListener 中的 onClickListener 回调
        onClickListener?.let {
            view.setOnClickListener { v ->
                // 这里不直接调用，避免重复触发
                // 实际的点击处理在 onTouchListener 中完成
            }
        }
    }
    
    /**
     * 为 View 添加 iOS 风格的点击效果（简化版，只处理点击事件）
     */
    fun applyIOSClickEffect(view: View) {
        applyIOSEffect(view, null)
    }
}
