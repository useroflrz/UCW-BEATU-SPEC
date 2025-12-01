package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.model.FeedContentType
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.viewmodel.VideoItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 图文+音乐 Fragment
 *
 * - 只负责展示图片轮播和文案
 * - 背景音乐通过 VideoItemViewModel 的 audio-only 播放能力实现
 * - 不复用 VideoControlsView，因此不会出现视频进度条
 */
@AndroidEntryPoint
class ImagePostFragment : Fragment() {

    companion object {
        private const val TAG = "ImagePostFragment"
        private const val ARG_VIDEO_ITEM = "image_post_item"

        fun newInstance(item: VideoItem): ImagePostFragment {
            return ImagePostFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VIDEO_ITEM, item)
                }
            }
        }
    }

    private val viewModel: VideoItemViewModel by viewModels()

    private var viewPager: ViewPager2? = null
    private var firstFrameImage: ImageView? = null
    private var titleText: TextView? = null
    private var authorText: TextView? = null
    private var pageIndicator: TextView? = null
    private var pageProgressLayout: LinearLayout? = null
    private var likeIcon: ImageView? = null
    private var favoriteIcon: ImageView? = null
    private var commentIcon: ImageView? = null
    private var shareIcon: ImageView? = null
    private var likeCountText: TextView? = null
    private var favoriteCountText: TextView? = null
    private var commentCountText: TextView? = null
    private var shareCountText: TextView? = null

    private var imagePost: VideoItem? = null
    private var autoScrollJob: Job? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePost = arguments?.let {
            BundleCompat.getParcelable(it, ARG_VIDEO_ITEM, VideoItem::class.java)
        }
        if (imagePost?.type != FeedContentType.IMAGE_POST) {
            Log.w(TAG, "ImagePostFragment created with non IMAGE_POST item: $imagePost")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.vp_images)
        firstFrameImage = view.findViewById(R.id.iv_first_frame)
        titleText = view.findViewById(R.id.tv_video_title)
        authorText = view.findViewById(R.id.tv_channel_name)
        pageIndicator = view.findViewById(R.id.tv_page_indicator)
        pageProgressLayout = view.findViewById(R.id.layout_page_progress)
        likeIcon = view.findViewById(R.id.iv_like)
        favoriteIcon = view.findViewById(R.id.iv_favorite)
        commentIcon = view.findViewById(R.id.iv_comment)
        shareIcon = view.findViewById(R.id.iv_share)
        likeCountText = view.findViewById(R.id.tv_like_count)
        favoriteCountText = view.findViewById(R.id.tv_favorite_count)
        commentCountText = view.findViewById(R.id.tv_comment_count)
        shareCountText = view.findViewById(R.id.tv_share_count)

        val item = imagePost
        if (item == null) {
            Log.e(TAG, "imagePost is null")
            return
        }

        // 标题和作者
        titleText?.text = item.title
        authorText?.text = item.authorName

        val urls = item.imageUrls.takeIf { it.isNotEmpty() }
        if (urls == null) {
            // 没有图片时，隐藏 ViewPager，只保留文案
            viewPager?.isVisible = false
            pageIndicator?.isVisible = false
        } else {
            // 在 ViewPager2 完成首帧初始化之前先将其隐藏，避免预布局阶段的错误页面被用户看到
            viewPager?.visibility = View.INVISIBLE
            // 首帧遮罩：先加载第一张图片到覆盖层，确保用户第一次进入时不会看到错误页内容
            showFirstFrame(urls.first())
            setupImagePager(urls)
        }

        // 初始化互动区：点赞/收藏/评论/分享 数字
        likeCountText?.text = item.likeCount.toString()
        favoriteCountText?.text = item.favoriteCount.toString()
        commentCountText?.text = item.commentCount.toString()
        shareCountText?.text = item.shareCount.toString()

        // 初始化互动状态到 ViewModel
        viewModel.initInteractionState(
            isLiked = false,
            isFavorited = false,
            likeCount = item.likeCount.toLong(),
            favoriteCount = item.favoriteCount.toLong()
        )

        // 互动按钮点击事件
        likeIcon?.setOnClickListener {
            viewModel.toggleLike()
        }
        favoriteIcon?.setOnClickListener {
            viewModel.toggleFavorite()
        }
        // 评论/分享目前只预留入口，后续接评论弹层/分享面板
        commentIcon?.setOnClickListener {
            Log.d(TAG, "comment clicked (TODO: 打开评论弹层)")
        }
        shareIcon?.setOnClickListener {
            Log.d(TAG, "share clicked (TODO: 打开分享入口)")
        }

        // 观察播放状态用于后续扩展（目前可以先不做 UI 显示）
        observePlaybackState()
    }

    override fun onStart() {
        super.onStart()
        // 启动 BGM
        startBgmIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (isViewVisibleOnScreen()) {
            startAutoScroll()
            viewModel.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
        viewModel.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 释放 PageChangeCallback，避免内存泄漏
        viewPager?.let { pager ->
            pageChangeCallback?.let { pager.unregisterOnPageChangeCallback(it) }
        }
        pageChangeCallback = null
        stopAutoScroll()
        viewPager = null
        firstFrameImage = null
        titleText = null
        authorText = null
        pageIndicator = null
        pageProgressLayout = null
        likeIcon = null
        favoriteIcon = null
        commentIcon = null
        shareIcon = null
        likeCountText = null
        favoriteCountText = null
        commentCountText = null
        shareCountText = null
    }

    fun onParentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            startBgmIfNeeded()
            startAutoScroll()
            viewModel.resume()
        } else {
            stopAutoScroll()
            viewModel.pause()
        }
    }

    fun checkVisibilityAndPlay() {
        if (isViewVisibleOnScreen()) {
            startBgmIfNeeded()
            startAutoScroll()
            viewModel.resume()
        } else {
            stopAutoScroll()
            viewModel.pause()
        }
    }

    private fun setupImagePager(urls: List<String>) {
        val pager = viewPager ?: return
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
            // 注意：首帧遮罩是否移除，由 showFirstFrame 的图片加载回调决定，
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

    private fun startBgmIfNeeded() {
        val item = imagePost ?: return
        val bgmUrl = item.bgmUrl
        if (bgmUrl.isNullOrBlank()) return
        // 图文+BGM 场景：确保始终使用 audio-only 播放，并允许无限循环
        if (viewModel.uiState.value.currentVideoId == null) {
            viewModel.prepareAudioOnly(item.id, bgmUrl)
        }
    }

    private fun startAutoScroll() {
        val pager = viewPager ?: return
        if (autoScrollJob?.isActive == true) return
        autoScrollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && viewPager != null) {
                // 如果首张图片还没有成功显示（例如仍在占位图状态），则延迟轮播启动
                val firstImageLoaded = (pager.adapter as? androidx.recyclerview.widget.RecyclerView.Adapter<*>) != null
                if (!firstImageLoaded) {
                    delay(300L)
                    continue
                }
                delay(3000L)
                if (!isViewVisibleOnScreen()) continue
                val p = viewPager ?: break
                val itemCount = p.adapter?.itemCount ?: 0
                if (itemCount <= 1) continue
                // 无限轮播：到达最后一张后，从头回到第 0 张
                val nextIndex = (p.currentItem + 1) % itemCount
                p.setCurrentItem(nextIndex, true)
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    private fun observePlaybackState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 播放错误日志
                    state.error?.let { Log.e(TAG, "BGM error: $it") }

                    // 同步互动状态到底部 UI
                    likeCountText?.text = state.likeCount.toString()
                    favoriteCountText?.text = state.favoriteCount.toString()

                    val highlightColor = 0xFFFFEB3B.toInt()
                    val normalColor = 0xFFFFFFFF.toInt()
                    likeIcon?.setColorFilter(if (state.isLiked) highlightColor else normalColor)
                    favoriteIcon?.setColorFilter(if (state.isFavorited) highlightColor else normalColor)
                }
            }
        }
    }

    private fun isViewVisibleOnScreen(): Boolean {
        val v = view ?: return false
        if (!isAdded || !isVisible || v.visibility != View.VISIBLE) return false
        val rect = android.graphics.Rect()
        val visible = v.getGlobalVisibleRect(rect)
        if (!visible) return false
        val area = v.width * v.height
        val visibleArea = rect.width() * rect.height()
        if (area <= 0) return false
        val ratio = visibleArea.toFloat() / area.toFloat()
        return ratio >= 0.1f
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
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
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
}


