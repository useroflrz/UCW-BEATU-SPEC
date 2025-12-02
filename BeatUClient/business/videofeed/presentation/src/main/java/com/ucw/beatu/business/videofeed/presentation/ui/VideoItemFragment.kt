package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import androidx.viewpager2.widget.ViewPager2
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.model.FeedContentType
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.model.VideoOrientation
import com.ucw.beatu.business.videofeed.presentation.viewmodel.VideoItemViewModel
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.designsystem.widget.VideoControlsView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoItemFragment : BaseFeedItemFragment() {

    companion object {
        private const val TAG = "VideoItemFragment"
        private const val ARG_VIDEO_ITEM = "video_item"

        fun newInstance(videoItem: VideoItem): VideoItemFragment {
            return VideoItemFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VIDEO_ITEM, videoItem)
                }
            }
        }
    }

    private val viewModel: VideoItemViewModel by viewModels()

    private var playerView: PlayerView? = null
    private var imagePager: ViewPager2? = null
    private var controlsView: VideoControlsView? = null
    private var videoItem: VideoItem? = null
    private var navigatingToLandscape = false
    private var hasPreparedPlayer = false
    private var imageAutoScrollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoItem = arguments?.let {
            BundleCompat.getParcelable(it, ARG_VIDEO_ITEM, VideoItem::class.java)
        }
        if (videoItem == null) {
            Log.e(TAG, "VideoItem is null!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.item_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerView = view.findViewById(R.id.player_view)
        imagePager = view.findViewById(R.id.image_pager)
        controlsView = view.findViewById(R.id.video_controls)
        // 注意：全屏按钮及标题/频道名称等现在定义在 shared:designsystem 的 VideoControlsView 布局中
        val sharedControlsRoot = controlsView
        val fullScreenButton = sharedControlsRoot?.findViewById<View>(
            com.ucw.beatu.shared.designsystem.R.id.iv_fullscreen
        )

        videoItem?.let { item ->
            // 通过 VideoControlsView 内部的 TextView 展示标题与频道名称
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_video_title
            )?.text = item.title
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_channel_name
            )?.text = item.authorName
            // 四个互动按钮下方的计数文案（与截图风格一致）
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_like_count
            )?.text = item.likeCount.toString()
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_share_count
            )?.text = item.shareCount.toString()
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_favorite_count
            )?.text = item.favoriteCount.toString()
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_comment_count
            )?.text = item.commentCount.toString()
            
            // 初始化互动状态
            viewModel.initInteractionState(
                isLiked = false, // TODO: 从VideoItem中获取实际状态
                isFavorited = false, // TODO: 从VideoItem中获取实际状态
                likeCount = item.likeCount.toLong(),
                favoriteCount = item.favoriteCount.toLong()
            )

            val isLandscapeVideo = item.orientation == VideoOrientation.LANDSCAPE
            val isImagePost = item.type == FeedContentType.IMAGE_POST

            // 图文内容：隐藏视频播放器，显示图片轮播；保留统一的底部交互区
            if (isImagePost) {
                playerView?.visibility = View.GONE
                imagePager?.visibility = View.VISIBLE
                setupImagePager(item.imageUrls)
                // 图文内容不支持横屏全屏按钮
                fullScreenButton?.visibility = View.GONE
            } else {
                // 视频内容：显示播放器，隐藏图文容器
                playerView?.visibility = View.VISIBLE
                imagePager?.visibility = View.GONE
                fullScreenButton?.visibility = if (isLandscapeVideo) View.VISIBLE else View.GONE
            }
        }

        observeViewModel()

        controlsView?.listener = object : VideoControlsView.VideoControlsListener {
            override fun onPlayPauseClicked() {
                viewModel.togglePlayPause()
            }

            override fun onLikeClicked() {
                viewModel.toggleLike()
            }

            override fun onFavoriteClicked() {
                viewModel.toggleFavorite()
            }

            override fun onCommentClicked() {
                // TODO: 打开评论弹层
            }

            override fun onShareClicked() {
                // TODO: 打开分享弹层
            }

            override fun onSeekRequested(positionMs: Long) {
                viewModel.seekTo(positionMs)
            }
        }
        fullScreenButton?.setOnClickListener { openLandscapeMode() }

        // A：点视频区域 = 播放/暂停切换（仅视频内容生效）
        playerView?.setOnClickListener {
            viewModel.togglePlayPause()
        }

    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    playerView?.visibility = View.VISIBLE

                    controlsView?.state = VideoControlsView.VideoControlsState(
                        isPlaying = state.isPlaying,
                        isLoading = state.isLoading,
                        currentPositionMs = state.currentPositionMs,
                        durationMs = state.durationMs,
                        isLiked = state.isLiked,
                        isFavorited = state.isFavorited,
                        likeCount = state.likeCount,
                        favoriteCount = state.favoriteCount
                    )
                    
                    state.error?.let { error -> Log.e(TAG, "播放错误: $error") }
                }
            }
        }
    }

    // ✅ 修复：onStart() 逻辑 - 只准备播放器，不立即播放
    override fun onStart() {
        super.onStart()
        val item = videoItem
        if (item?.type == FeedContentType.IMAGE_POST) {
            // 图文内容：此处仅确保后续可见时准备音频，无需绑定 PlayerView
            // 真实播放时机仍由 checkVisibilityAndPlay/startPlaybackIfNeeded 控制
        } else {
            if (viewModel.uiState.value.currentVideoId != null) {
                // 横屏返回，恢复播放器
                reattachPlayer()
            } else {
                // 首次加载 - 只准备播放器，不立即播放
                preparePlayerForFirstTime()
            }
            // ✅ 修复：不在这里立即播放，等待 Fragment 真正可见时再播放（由 handlePageSelected() 触发）
        }
    }

    override fun onResume() {
        super.onResume()
        navigatingToLandscape = false
        // 延迟检查 View 是否在屏幕上可见，确保 View 已经布局完成
        view?.post {
            if (isResumed) {
                checkVisibilityAndPlay()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 停止图文轮播自动滚动
        stopImageAutoScroll()
        if (!navigatingToLandscape && hasPreparedPlayer) {
            Log.d(TAG, "onPause: pausing video ${videoItem?.id}")
            viewModel.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerView?.player = null
        if (!navigatingToLandscape) {
            viewModel.releaseCurrentPlayer()
            hasPreparedPlayer = false
        } else {
            Log.d(TAG, "onDestroyView: skip release because navigatingToLandscape=true")
        }
        playerView = null
        controlsView = null
    }

    fun onParentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            startPlaybackIfNeeded()
            if (videoItem?.type == FeedContentType.IMAGE_POST) {
                startImageAutoScroll()
            }
        } else if (hasPreparedPlayer) {
            Log.d(TAG, "onParentVisibilityChanged: fragment hidden, pausing ${videoItem?.id}")
            viewModel.pause()
            stopImageAutoScroll()
        }
    }

    fun onParentTabVisibilityChanged(isVisible: Boolean) {
        onParentVisibilityChanged(isVisible)
    }
    
    /**
     * 检查可见性并播放：如果 View 在屏幕上可见则播放，否则暂停
     */
    fun checkVisibilityAndPlay() {
        if (isViewVisibleOnScreen()) {
            Log.d(TAG, "checkVisibilityAndPlay: Fragment is visible, starting playback")
            startPlaybackIfNeeded()
        } else {
            if (hasPreparedPlayer) {
                viewModel.pause()
                Log.d(TAG, "Video not visible on screen, paused")
            }
        }
    }
    
    private fun startPlaybackIfNeeded(forcePrepare: Boolean = false) {
        if (!isAdded || videoItem == null) {
            Log.w(TAG, "Fragment not ready, skip startPlayback")
            return
        }

        val item = videoItem ?: return

        if (item.type == FeedContentType.IMAGE_POST) {
            // 图文+音乐：为 BGM 准备“音频-only”播放器
            val bgmUrl = item.bgmUrl
            if (!hasPreparedPlayer && !bgmUrl.isNullOrBlank()) {
                Log.d(TAG, "startPlaybackIfNeeded: preparing audio-only BGM for image post ${item.id}")
                viewModel.prepareAudioOnly(item.id, bgmUrl)
                hasPreparedPlayer = true
            }
            // Fragment 已可见，恢复/启动音频播放
            viewModel.resume()
        } else {
            if (playerView == null) {
                Log.w(TAG, "startPlaybackIfNeeded: playerView is null for video item, skip")
                return
            }
            val needsReprepare = forcePrepare || !hasPreparedPlayer || playerView?.player == null
            if (needsReprepare) {
                Log.d(
                    TAG,
                    "startPlaybackIfNeeded: (re)preparing player (force=$forcePrepare, hasPrepared=$hasPreparedPlayer, playerView.player=${playerView?.player})"
                )
                preparePlayerForFirstTime()
            } else {
                Log.d(TAG, "startPlaybackIfNeeded: player already prepared and attached, resuming")
            }
            // Fragment 已可见，确保播放状态恢复
            viewModel.resume()
        }
    }

    // ✅ 修复：首次加载逻辑
    private fun preparePlayerForFirstTime() {
        val item = videoItem ?: return
        val pv = playerView ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            // 图文内容不在此处准备播放器（仅使用音频-only 模式）
            Log.d(TAG, "preparePlayerForFirstTime: skip for image post ${item.id}")
            return
        }
        Log.d(TAG, "Preparing video for playback: ${item.id}")
        viewModel.playVideo(item.id, item.videoUrl)
        viewModel.preparePlayer(item.id, item.videoUrl, pv)
        hasPreparedPlayer = true
        Log.d(TAG, "preparePlayerForFirstTime: hasPreparedPlayer=$hasPreparedPlayer, playerView.player=${pv.player}")
    }

    // ✅ 修复：重绑定播放器逻辑（统一走 preparePlayer，让 PlaybackSession 决定是否续播）
    private fun reattachPlayer() {
        if (!isAdded) return
        val item = videoItem ?: return
        val pv = playerView ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            // 图文内容目前不支持横竖屏热切换，忽略重绑定
            Log.d(TAG, "reattachPlayer: skip for image post ${item.id}")
            return
        }
        Log.d(TAG, "reattachPlayer: re-preparing video ${item.id}")
        viewModel.preparePlayer(item.id, item.videoUrl, pv)
        hasPreparedPlayer = true
        Log.d(TAG, "reattachPlayer: hasPreparedPlayer=$hasPreparedPlayer, playerView.player=${pv.player}")
    }

    private fun openLandscapeMode() {
        val navController = findParentNavController()
        if (navController == null) {
            Log.e(TAG, "NavController not found, cannot open landscape mode")
            return
        }
        val item = videoItem ?: run {
            Log.e(TAG, "openLandscapeMode: videoItem null")
            return
        }
        viewModel.persistPlaybackSession()
        viewModel.mediaPlayer()?.let { player ->
            PlayerView.switchTargetView(player, playerView, null)
        }

        val actionId = NavigationHelper.getResourceId(requireContext(), NavigationIds.ACTION_FEED_TO_LANDSCAPE)
        if (actionId == 0) {
            Log.e(TAG, "Navigation action not found: ${NavigationIds.ACTION_FEED_TO_LANDSCAPE}")
            return
        }

        val args = bundleOf(
            LandscapeLaunchContract.EXTRA_VIDEO_ID to item.id,
            LandscapeLaunchContract.EXTRA_VIDEO_URL to item.videoUrl,
            LandscapeLaunchContract.EXTRA_VIDEO_TITLE to item.title,
            LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR to item.authorName,
            LandscapeLaunchContract.EXTRA_VIDEO_LIKE to item.likeCount,
            LandscapeLaunchContract.EXTRA_VIDEO_COMMENT to item.commentCount,
            LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE to item.favoriteCount,
            LandscapeLaunchContract.EXTRA_VIDEO_SHARE to item.shareCount
        )

        navigatingToLandscape = true
        runCatching { navController.navigate(actionId, args) }
            .onFailure {
                navigatingToLandscape = false
                Log.e(TAG, "Failed to navigate to landscape fragment", it)
            }
    }

    private fun findParentNavController(): NavController? {
        return runCatching { parentFragment?.findNavController() ?: findNavController() }.getOrNull()
    }

    // ✅ 保留：仅用于设置 currentVideoId
    private fun loadVideo() {
        val item = videoItem ?: return
        viewModel.playVideo(item.id, item.videoUrl)
    }

    /**
     * 初始化图文轮播（ViewPager2）
     */
    private fun setupImagePager(imageUrls: List<String>) {
        val pager = imagePager ?: return
        if (imageUrls.isEmpty()) {
            pager.visibility = View.GONE
            return
        }
        pager.adapter = ImagePagerAdapter(imageUrls)
        pager.offscreenPageLimit = 1
        // 让初始位置远离边界，避免无限轮播时过早到达尽头
        pager.setCurrentItem(imageUrls.size * 1000, false)
        // 准备好后，如果当前 Fragment 可见，则启动自动轮播
        if (isViewVisibleOnScreen()) {
            startImageAutoScroll()
        }
    }

    private inner class ImagePagerAdapter(
        private val images: List<String>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ImageViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ImageViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_page, parent, false)
            return ImageViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(images[position % images.size])
        }

        override fun getItemCount(): Int = if (images.isEmpty()) 0 else Int.MAX_VALUE
    }

    private class ImageViewHolder(itemView: View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        private val imageView: android.widget.ImageView =
            itemView.findViewById(R.id.iv_image)

        fun bind(url: String) {
            // 当前为静态调试阶段：用不同背景色区分“张数”，方便验证轮播是否生效
            val color = when (bindingAdapterPosition % 3) {
                0 -> android.graphics.Color.DKGRAY
                1 -> android.graphics.Color.BLUE
                else -> android.graphics.Color.MAGENTA
            }
            imageView.setBackgroundColor(color)
            // TODO: 接入 Coil/Glide 按 url 加载真实图片
        }
    }

    /**
     * 启动图文轮播的自动滚动
     */
    private fun startImageAutoScroll() {
        val pager = imagePager ?: return
        if (imageAutoScrollJob?.isActive == true) return
        imageAutoScrollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && imagePager != null) {
                delay(3000L)
                if (!isViewVisibleOnScreen()) continue
                val p = imagePager ?: break
                if (p.adapter?.itemCount ?: 0 <= 1) continue
                p.currentItem = p.currentItem + 1
            }
        }
    }

    /**
     * 停止图文轮播的自动滚动
     */
    private fun stopImageAutoScroll() {
        imageAutoScrollJob?.cancel()
        imageAutoScrollJob = null
    }
}