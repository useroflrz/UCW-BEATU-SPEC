package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
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
class ImagePostFragment : BaseFeedItemFragment() {

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

    private var imageCarouselController: ImageCarouselController? = null
    private var titleText: TextView? = null
    private var authorText: TextView? = null
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
        imageCarouselController = ImageCarouselController(this, view)
        titleText = view.findViewById(R.id.tv_video_title)
        authorText = view.findViewById(R.id.tv_channel_name)
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
            // 没有图片时，仅保留文案和互动区
            view.findViewById<View>(R.id.vp_images)?.isVisible = false
            view.findViewById<View>(R.id.tv_page_indicator)?.isVisible = false
        } else {
            imageCarouselController?.bindImages(urls)
        }

        // 初始化互动区：点赞/收藏/评论/分享 数字
        likeCountText?.text = item.likeCount.toString()
        favoriteCountText?.text = item.favoriteCount.toString()
        commentCountText?.text = item.commentCount.toString()
        shareCountText?.text = item.shareCount.toString()

        // 初始化互动状态到 ViewModel（使用后端返回的真实状态）
        viewModel.initInteractionState(
            videoId = item.id,
            isLiked = item.isLiked,
            isFavorited = item.isFavorited,
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
        imageCarouselController?.onDestroyView()
        stopAutoScroll()
        imageCarouselController = null
        titleText = null
        authorText = null
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
        if (autoScrollJob?.isActive == true) return
        launchRepeatingJob(::autoScrollJob) {
            while (isActive) {
                if (!isViewVisibleOnScreen()) {
                    delay(300L)
                    continue
                }
                imageCarouselController?.startAutoScroll()
                break
            }
        }
    }
    
    private fun stopAutoScroll() {
        cancelJob(::autoScrollJob)
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

                    // 与视频页的互动区保持视觉风格一致
                    val likeActiveColor = 0xFFFF4D4F.toInt()
                    val favoriteActiveColor = 0xFFFFD700.toInt()
                    val normalColor = 0xFFFFFFFF.toInt()
                    likeIcon?.setColorFilter(if (state.isLiked) likeActiveColor else normalColor)
                    favoriteIcon?.setColorFilter(if (state.isFavorited) favoriteActiveColor else normalColor)
                }
            }
        }
    }
}


