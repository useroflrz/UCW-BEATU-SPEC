package com.ucw.beatu.business.videofeed.presentation.ui

import android.graphics.Rect
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty0

/**
 * Feed 单条内容（视频 / 图文）的通用基类：
 *
 * - 提供统一的“是否在屏幕上可见”判断
 * - 提供简单的 Job 启动/取消模板，避免在 Fragment 里重复写样板代码
 *
 * 不关心具体 UI，只做通用工具能力。
 */
@AndroidEntryPoint
abstract class BaseFeedItemFragment : Fragment() {

    /**
     * 当前 Fragment 根视图是否“足够可见”：
     * - 已经添加到 Activity
     * - Fragment 自身可见
     * - globalVisibleRect 覆盖面积 >= 10%
     */
    fun isViewVisibleOnScreen(): Boolean {
        val v = view ?: return false
        if (!isAdded || !isVisible || v.visibility != View.VISIBLE) return false
        val rect = Rect()
        val visible = v.getGlobalVisibleRect(rect)
        if (!visible) return false
        val area = v.width * v.height
        val visibleArea = rect.width() * rect.height()
        if (area <= 0) return false
        val ratio = visibleArea.toFloat() / area.toFloat()
        return ratio >= 0.1f
    }

    /**
     * 启动一个与 viewLifecycleOwner 绑定的协程 Job：
     * - 如果之前有 Job，会先取消再重建
     * - 典型用法：自动轮播、定时上报等
     */
    fun launchRepeatingJob(
        jobRef: KMutableProperty0<Job?>,
        block: suspend CoroutineScope.() -> Unit
    ) {
        jobRef.get()?.cancel()
        jobRef.set(
            viewLifecycleOwner.lifecycleScope.launch(block = block)
        )
    }

    /**
     * 安全取消并清空一个 Job 引用。
     */
    fun cancelJob(jobRef: KMutableProperty0<Job?>) {
        jobRef.get()?.cancel()
        jobRef.set(null)
    }
}


