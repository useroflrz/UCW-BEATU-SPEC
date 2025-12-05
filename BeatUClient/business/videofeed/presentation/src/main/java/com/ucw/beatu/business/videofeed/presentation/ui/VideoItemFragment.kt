package com.ucw.beatu.business.videofeed.presentation.ui

import android.content.Intent
import android.graphics.Bitmap
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
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation
import com.ucw.beatu.business.videofeed.presentation.viewmodel.VideoItemViewModel
import com.ucw.beatu.business.videofeed.presentation.share.SharePosterGenerator
import com.ucw.beatu.business.videofeed.presentation.share.ShareImageUtils
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.designsystem.widget.VideoControlsView
import coil.load
import com.ucw.beatu.shared.router.RouterRegistry
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import androidx.fragment.app.Fragment
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

    // 用户信息展示相关
    private var userInfoOverlay: View? = null
    private var rootLayout: ConstraintLayout? = null
    private var isUserInfoVisible = false
    private var userProfileFragment: Fragment? = null
    private var userProfileDialogFragment: UserProfileDialogFragment? = null

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

        rootLayout = view as? ConstraintLayout
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
            val channelNameView = sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_channel_name
            )
            channelNameView?.text = item.authorName

            // 作者头像：使用后端返回的 authorAvatar URL，通过 Coil 加载
            val channelAvatarView = sharedControlsRoot?.findViewById<android.widget.ImageView>(
                com.ucw.beatu.shared.designsystem.R.id.iv_channel_avatar
            )
            val placeholderRes = com.ucw.beatu.shared.designsystem.R.drawable.ic_avatar_placeholder
            val authorAvatarUrl = item.authorAvatar
            if (authorAvatarUrl.isNullOrBlank()) {
                channelAvatarView?.setImageResource(placeholderRes)
            } else {
                channelAvatarView?.load(authorAvatarUrl) {
                    crossfade(true)
                    placeholder(placeholderRes)
                    error(placeholderRes)
                }
            }

            // 主页：点击作者头像/昵称显示用户信息（半屏展示）
            val avatarView = sharedControlsRoot?.findViewById<android.widget.ImageView>(
                com.ucw.beatu.shared.designsystem.R.id.iv_channel_avatar
            )
            val authorClickListener = View.OnClickListener {
                showUserInfoOverlay(item.authorId, item.authorName)
            }
            avatarView?.setOnClickListener(authorClickListener)
            channelNameView?.setOnClickListener(authorClickListener)

            // 四个互动按钮下方的计数文案（与截图风格一致）
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_like_count
            )?.text = item.likeCount.toString()
             val shareCountTextView = sharedControlsRoot?.findViewById<android.widget.TextView>(
                 com.ucw.beatu.shared.designsystem.R.id.tv_share_count
             )
             shareCountTextView?.text = item.shareCount.toString()
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_favorite_count
            )?.text = item.favoriteCount.toString()
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_comment_count
            )?.text = item.commentCount.toString()
            
            // 初始化互动状态
            viewModel.initInteractionState(
                videoId = item.id,
                isLiked = item.isLiked,
                isFavorited = item.isFavorited,
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
                val item = videoItem ?: return
                VideoCommentsDialogFragment
                    .newInstance(item.id, item.commentCount)
                    .show(parentFragmentManager, "video_comments_dialog")
            }

             override fun onShareClicked() {
                 val item = videoItem ?: return
                 // 当前展示的分享计数，本地乐观累加
                 val shareCountTextView = sharedControlsRoot?.findViewById<android.widget.TextView>(
                     com.ucw.beatu.shared.designsystem.R.id.tv_share_count
                 )
                 var currentShareCount: Long =
                     shareCountTextView?.text?.toString()?.toLongOrNull() ?: item.shareCount.toLong()
                val dialog = VideoShareDialogFragment.newInstance(
                    videoId = item.id,
                    title = item.title,
                    playUrl = item.videoUrl
                )
                dialog.shareActionListener = object : VideoShareDialogFragment.ShareActionListener {
                     override fun onSharePoster() {
                         // 上报分享 + 本地乐观更新计数
                         viewModel.reportShare()
                         currentShareCount += 1
                         shareCountTextView?.text = currentShareCount.toString()

                         // 生成“封面 + 二维码”分享图，并调用系统分享图片
                         val pv = playerView
                         if (pv == null) {
                             Log.e(TAG, "PlayerView is null, cannot capture cover for share poster")
                             return
                         }
                         val context = requireContext()
                         val shareUrl = item.videoUrl // 若有专门的 H5 / DeepLink，可替换为专用链接
                         val posterBitmap: Bitmap = SharePosterGenerator.generate(
                             context = context,
                             coverView = pv,
                             title = item.title,
                             author = item.authorName,
                             shareUrl = shareUrl
                         )

                         // 把 Bitmap 保存到 Cache 并通过 FileProvider 分享
                         ShareImageUtils.shareBitmap(
                             context = context,
                             bitmap = posterBitmap,
                             fileName = "beatu_share_${item.id}.jpg",
                             chooserTitle = "分享图片"
                         )
                     }

                    override fun onShareLink() {
                         viewModel.reportShare()
                         currentShareCount += 1
                         shareCountTextView?.text = currentShareCount.toString()
                        // 使用系统分享，分享视频标题 + 播放链接
                        val context = requireContext()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${item.title}\n${item.videoUrl}"
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "分享视频")
                        )
                    }
                }
                dialog.show(parentFragmentManager, "video_share_options")
            }

            override fun onSeekRequested(positionMs: Long) {
                viewModel.seekTo(positionMs)
            }
        }
        fullScreenButton?.setOnClickListener { openLandscapeMode() }

        // A：点视频区域 = 播放/暂停切换（仅视频内容生效）
        // 如果用户信息覆盖层可见，点击视频区域则关闭覆盖层；否则切换播放/暂停
        playerView?.setOnClickListener {
            if (isUserInfoVisible) {
                hideUserInfoOverlay()
            } else {
                viewModel.togglePlayPause()
            }
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

    /**
     * 显示用户信息弹窗（类似评论弹窗，从底部弹出）
     * 视频缩小到上半部分，用户信息从底部弹出占据下半部分
     */
    private fun showUserInfoOverlay(authorId: String, authorName: String) {
        if (isUserInfoVisible) {
            hideUserInfoOverlay()
            return
        }

        val layout = rootLayout ?: return
        val player = playerView ?: return

        isUserInfoVisible = true

        // 不暂停视频播放，继续播放

        // 使用 ConstraintSet 实现布局动画：视频缩小到上半部分
        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)

        // 视频播放器缩小到上半部分（50%高度）
        constraintSet.clear(R.id.player_view, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.player_view, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.player_view, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.constrainPercentHeight(R.id.player_view, 0.5f)

        // 同时调整 image_pager 和 video_controls 的高度，确保它们也缩小到上半部分
        constraintSet.clear(R.id.image_pager, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.image_pager, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.image_pager, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.constrainPercentHeight(R.id.image_pager, 0.5f)

        constraintSet.clear(R.id.video_controls, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.video_controls, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.video_controls, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.constrainPercentHeight(R.id.video_controls, 0.5f)

        // 应用动画
        TransitionManager.beginDelayedTransition(layout)
        constraintSet.applyTo(layout)

        // 显示用户信息 DialogFragment（从底部弹出）
        val userId = if (authorId.isNotEmpty()) authorId else authorName
        userProfileDialogFragment = UserProfileDialogFragment.newInstance(userId, authorName)
        userProfileDialogFragment?.setOnDismissListener {
            hideUserInfoOverlay()
        }
        // 设置视频点击回调，当在个人主页中点击视频时，关闭Dialog并导航到视频播放器
        userProfileDialogFragment?.setOnVideoClickListener { userId, authorName, videoItems, initialIndex ->
            // 关闭Dialog
            hideUserInfoOverlay()
            // 导航到视频播放器
            navigateToUserWorksViewer(userId, videoItems, initialIndex)
        }
        userProfileDialogFragment?.show(parentFragmentManager, "UserProfileDialog")
    }

    /**
     * 隐藏用户信息弹窗（恢复全屏视频）
     */
    private fun hideUserInfoOverlay() {
        if (!isUserInfoVisible) return

        // 关闭 DialogFragment
        userProfileDialogFragment?.dismissAllowingStateLoss()
        userProfileDialogFragment = null

        val layout = rootLayout ?: return

        isUserInfoVisible = false

        // 使用 ConstraintSet 恢复全屏布局
        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)

        // 视频播放器恢复全屏
        constraintSet.clear(R.id.player_view, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.player_view, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.player_view, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(R.id.player_view, 1.0f)

        // 同时恢复 image_pager 和 video_controls 的全屏高度
        constraintSet.clear(R.id.image_pager, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.image_pager, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.image_pager, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(R.id.image_pager, 1.0f)

        constraintSet.clear(R.id.video_controls, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.video_controls, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.video_controls, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(R.id.video_controls, 1.0f)

        // 应用动画
        TransitionManager.beginDelayedTransition(layout)
        constraintSet.applyTo(layout)

        // 不恢复播放（因为从未暂停），继续播放
    }

    /**
     * 导航到用户作品播放器
     */
    private fun navigateToUserWorksViewer(userId: String, videoItems: ArrayList<VideoItem>, initialIndex: Int) {
        val navController = findParentNavController()
        if (navController == null) {
            Log.e(TAG, "NavController not found, cannot open user works viewer")
            return
        }

        // 检查当前是否已经在 userWorksViewer 页面
        val currentDestination = navController.currentDestination
        val userWorksViewerDestinationId = NavigationHelper.getResourceId(
            requireContext(),
            NavigationIds.USER_WORKS_VIEWER
        )
        val isInUserWorksViewer = currentDestination?.id == userWorksViewerDestinationId

        if (isInUserWorksViewer) {
            // 已经在 userWorksViewer 页面，检查是否是同一个用户
            val router = RouterRegistry.getUserWorksViewerRouter()
            val currentUserId = router?.getCurrentUserId()

            if (currentUserId == userId && router != null) {
                // 同一个用户，通过 Router 切换到对应的视频
                Log.d(TAG, "Already in user works viewer for user $userId, switching to video at index $initialIndex")
                val success = router.switchToVideo(initialIndex)
                if (success) {
                    // 成功切换，关闭用户信息弹窗
                    hideUserInfoOverlay()
                    return
                } else {
                    Log.w(TAG, "Failed to switch video in UserWorksViewer, falling back to navigation")
                }
            } else {
                // 不同用户，允许导航到新用户的作品列表
                Log.d(TAG, "Already in user works viewer but different user (current: $currentUserId, target: $userId), allowing navigation")
            }
        }

        val context = requireContext()
        val currentDestId = navController.currentDestination?.id
        val userWorksDestId = NavigationHelper.getResourceId(context, NavigationIds.USER_WORKS_VIEWER)
        val searchResultDestId = NavigationHelper.getResourceId(context, NavigationIds.SEARCH_RESULT)
        val searchResultVideoDestId = NavigationHelper.getResourceId(context, NavigationIds.SEARCH_RESULT_VIDEO_VIEWER)
        val userProfileDestId = NavigationHelper.getResourceId(context, NavigationIds.USER_PROFILE)

        Log.d(
            TAG,
            "navigateToUserWorksViewer: currentDestId=$currentDestId, " +
                "userWorksDestId=$userWorksDestId, searchResultDestId=$searchResultDestId, " +
                "searchResultVideoDestId=$searchResultVideoDestId, userProfileDestId=$userProfileDestId, " +
                "targetUserId=$userId, initialIndex=$initialIndex, videoListSize=${videoItems.size}"
        )

        // 如果当前已经在 UserWorksViewer，保持在同一目的地上叠加新入参（不 pop），
        // 这样返回键能回到之前的用户作品（搜索来源希望回到上一层视频页）

        // 根据当前所在的 destination 精确选择 action，且仅使用当前目的地下存在的 action
        val candidateIds = listOf(
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_SEARCH_RESULT_TO_USER_WORKS_VIEWER),
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_USER_PROFILE_TO_USER_WORKS_VIEWER),
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_FEED_TO_USER_WORKS_VIEWER)
        ).filter { it != 0 }

        val usableActionId = candidateIds.firstOrNull { actionId ->
            navController.currentDestination?.getAction(actionId) != null
        }

        val actionId = usableActionId ?: userWorksDestId

        if (actionId == 0) {
            Log.e(TAG, "Navigation action not found for user works viewer (currentDest=$currentDestId)")
            return
        }

        Log.d(
            TAG,
            "navigateToUserWorksViewer: using actionId=$actionId, usableActionId=$usableActionId, candidates=$candidateIds"
        )

        val bundle = bundleOf(
            "user_id" to userId,
            "initial_index" to initialIndex,
            "video_list" to videoItems
        )
        navController.navigate(actionId, bundle)
    }

}