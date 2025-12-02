package com.ucw.beatu.business.videofeed.presentation.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.ucw.beatu.business.videofeed.presentation.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 图文轮播区域的通用控制器：
 *
 * - 负责 ViewPager2 + 首帧遮罩 + 页码文案 + 底部进度条
 * - 不直接依赖具体 Fragment，只通过回调访问可见性与协程 Job 能力
 *
 * 这样 ImagePostFragment 只关注业务数据，把图片展示细节托管给本类。
 */
class ImageCarouselController(
    private val host: BaseFeedItemFragment,
    root: View
) {

    companion object {
        private const val TAG = "ImageCarouselController"
    }

    private val viewPager: ViewPager2? = root.findViewById(R.id.vp_images)
    private val firstFrameImage: ImageView? = root.findViewById(R.id.iv_first_frame)
    private val pageIndicator: TextView? = root.findViewById(R.id.tv_page_indicator)
    private val pageProgressLayout: LinearLayout? = root.findViewById(R.id.layout_page_progress)

    private var autoScrollJob: Job? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    /**
     * 使用一组图片 URL 初始化轮播：
     * - 首帧遮罩
     * - 页码 / 进度条
     */
    fun bindImages(urls: List<String>) {
        if (urls.isEmpty()) {
            viewPager?.isVisible = false
            pageIndicator?.isVisible = false
            return
        }

        // 在 ViewPager2 完成首帧初始化之前先将其隐藏，避免预布局阶段的错误页面被用户看到
        viewPager?.visibility = View.INVISIBLE
        showFirstFrame(urls.first())
        setupImagePager(urls)
    }

    /**
     * 启动自动轮播，由宿主 Fragment 在合适的生命周期节点调用。
     */
    fun startAutoScroll() {
        val pager = viewPager ?: return
        if (autoScrollJob?.isActive == true) return
        host.launchRepeatingJob(::autoScrollJob) {
            while (isActive && viewPager != null) {
                // 如果首张图片还没有成功显示（例如仍在占位图状态），则延迟轮播启动
                val firstImageLoaded =
                    (pager.adapter as? androidx.recyclerview.widget.RecyclerView.Adapter<*>) != null
                if (!firstImageLoaded) {
                    delay(300L)
                    continue
                }
                delay(3000L)
                if (!host.isViewVisibleOnScreen()) continue
                val p = viewPager ?: break
                val itemCount = p.adapter?.itemCount ?: 0
                if (itemCount <= 1) continue
                // 无限轮播：到达最后一张后，从头回到第 0 张
                val nextIndex = (p.currentItem + 1) % itemCount
                p.setCurrentItem(nextIndex, true)
            }
        }
    }

    /**
     * 停止自动轮播。
     */
    fun stopAutoScroll() {
        host.cancelJob(::autoScrollJob)
    }

    /**
     * 宿主销毁 View 时调用，释放回调，避免内存泄漏。
     */
    fun onDestroyView() {
        viewPager?.let { pager ->
            pageChangeCallback?.let { pager.unregisterOnPageChangeCallback(it) }
        }
        pageChangeCallback = null
        stopAutoScroll()
    }

    private fun setupImagePager(urls: List<String>) {
        val pager = viewPager ?: run {
            Log.w(TAG, "setupImagePager: viewPager is null")
            return
        }
        // 初始化底部的“页进度条”，用 N 段细条表示 N 张图
        initPageProgress(urls.size)

        // 确保不会重复注册 PageChangeCallback
        pageChangeCallback?.let { pager.unregisterOnPageChangeCallback(it) }

        pager.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<ImageViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ImageViewHolder {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_page, parent, false)
                return ImageViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
                holder.bind(urls[position])
            }

            override fun getItemCount(): Int = urls.size
        }
        pager.offscreenPageLimit = 1

        // 标记是否已经完成首帧初始化，避免 ViewPager2 在内部预布局阶段触发一次“错误页码”
        var isInitialized = false

        // 通过 OnPageChangeCallback 统一更新页码和进度条
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 在我们手动完成首帧 1/n 初始化之前，忽略系统预先分发的回调
                if (!isInitialized) return
                updatePageIndicator(position, urls.size)
            }
        }
        pager.registerOnPageChangeCallback(callback)
        pageChangeCallback = callback

        // 为了避免 ViewPager2 在预布局阶段短暂展示“上一次复用的页面”（可能是最后一张图），
        // 将首帧位置设置延迟到布局完成之后再执行。
        pager.post {
            // 初始停留在第 0 张，立即展示 1/n 的指示信息
            pager.setCurrentItem(0, false)
            updatePageIndicator(0, urls.size)
            // 标记初始化完成，后续再响应用户/自动轮播触发的页切换
            isInitialized = true
            // 首帧和 ViewPager 状态都稳定后，再让 ViewPager 真正显示出来
            pager.visibility = View.VISIBLE
            // 首帧遮罩是否移除，由 showFirstFrame 的图片加载回调决定，
            // 这样可以确保用户看到的第一帧就是在线图，而不是占位图。
        }
    }

    /**
     * 首帧遮罩逻辑：
     * - 第一次进入图文页时，先用一个独立的 ImageView 覆盖在 ViewPager 上方，只加载第一张图片
     * - 等第一张图片加载完毕 / 或失败后，再交给 ViewPager 正常展示和轮播
     */
    private fun showFirstFrame(firstImageUrl: String) {
        val imageView = firstFrameImage ?: return
        // 首帧阶段不显示占位图：先保持 INVISIBLE，等网络图真正加载成功后再显示
        imageView.visibility = View.INVISIBLE
        imageView.setImageDrawable(null)
        imageView.load(firstImageUrl) {
            crossfade(false)
            listener(
                onSuccess = { _, _ ->
                    // 图片成功加载后再展示首帧，并交给 ViewPager 接管显示
                    imageView.post {
                        imageView.visibility = View.GONE
                    }
                },
                onError = { _, _ ->
                    // 加载失败时也立即移除遮罩，交给 ViewPager 自己的占位/错误逻辑处理
                    imageView.post { imageView.visibility = View.GONE }
                }
            )
        }
    }

    /**
     * 初始化底部图文进度条：根据图片数量创建 N 段细条
     */
    private fun initPageProgress(count: Int) {
        val container = pageProgressLayout ?: return
        container.removeAllViews()
        if (count <= 0) return
        container.weightSum = count.toFloat()
        val dp2 = (container.resources.displayMetrics.density * 2).toInt()
        repeat(count) { _ ->
            val v = View(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    leftMargin = dp2
                    rightMargin = dp2
                }
                setBackgroundColor(0x33FFFFFF) // 默认未选中：半透明白色
            }
            container.addView(v)
        }
    }

    /**
     * 更新当前页指示：上方“2/5”文案 + 底部细条高亮
     */
    private fun updatePageIndicator(index: Int, total: Int) {
        if (total <= 0) return
        pageIndicator?.text = "${index + 1}/$total"
        val container = pageProgressLayout ?: return
        val childCount = container.childCount
        if (childCount != total) return
        for (i in 0 until childCount) {
            val child = container.getChildAt(i)
            // 选中页更亮，未选中页更暗
            val color = if (i == index) 0xFFFFFFFF.toInt() else 0x33FFFFFF
            child.setBackgroundColor(color)
        }
    }

    private class ImageViewHolder(itemView: View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.iv_image)

        fun bind(url: String) {
            // 使用 Coil 加载网络图片，首帧不使用任何占位图，避免占位图闪现
            // 先清空上一轮绑定时残留的图片，避免 ViewHolder 复用时短暂出现“上一张图”
            imageView.setImageDrawable(null)
            imageView.load(url) {
                // 关闭 crossfade，避免轮播到下一张时出现“突然闪一下之前的缩略帧”
                crossfade(false)
            }
        }
    }
}


