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
import androidx.lifecycle.ViewModelProvider
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
    private var lastFullScreenClickTime = 0L
    private var isRestoringFromLandscape = false

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
         // 注意：标题/频道名称等现在定义在 shared:designsystem 的 VideoControlsView 布局中
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
            // 设置圆形裁剪
            channelAvatarView?.apply {
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                clipToOutline = true
            }
            val placeholderRes = com.ucw.beatu.shared.designsystem.R.drawable.ic_avatar_placeholder
            val authorAvatarUrl = item.authorAvatar
            
            // 尝试获取数据来源（通过 parentFragment 获取 RecommendViewModel）
            val videoSource = try {
                val parent = parentFragment
                if (parent is com.ucw.beatu.business.videofeed.presentation.ui.RecommendFragment) {
                    val recommendViewModel = ViewModelProvider(parent)[
                        com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel::class.java
                    ]
                    recommendViewModel.getVideoSource(item.id)
                } else {
                    "unknown"
                }
            } catch (e: Exception) {
                Log.w(TAG, "onViewCreated: 无法获取数据来源，${e.message}")
                "unknown"
            }
            
            // 打印作者头像和数据来源信息
            Log.d(TAG, "onViewCreated: 视频ID=${item.id}, 标题=${item.title}, 数据来源=$videoSource, authorAvatar=${authorAvatarUrl ?: "null"}, authorName=${item.authorName}")
            
            if (authorAvatarUrl.isNullOrBlank()) {
                Log.w(TAG, "onViewCreated: 作者头像为空，使用占位图，视频ID=${item.id}, 数据来源=$videoSource")
                channelAvatarView?.setImageResource(placeholderRes)
            } else {
                Log.d(TAG, "onViewCreated: 加载作者头像，视频ID=${item.id}, authorAvatar=$authorAvatarUrl, 数据来源=$videoSource")
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
                // 图文内容不显示横屏按钮
                fullScreenButton?.visibility = View.GONE
            } else {
                // 视频内容：显示播放器，隐藏图文容器
                playerView?.visibility = View.VISIBLE
                imagePager?.visibility = View.GONE
                // 所有视频内容都显示横屏按钮
                fullScreenButton?.visibility = View.VISIBLE
            }
        }
        
        // 设置横屏按钮点击监听（所有视频内容都支持横屏）
        fullScreenButton?.setOnClickListener {
            handleFullScreenButtonClick()
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
            val itemId = item?.id
            if (itemId != null) {
                // 如果正在从横屏恢复，跳过 onStart 的逻辑，让 restorePlayerFromLandscape 处理
                if (isRestoringFromLandscape) {
                    Log.d(TAG, "onStart: 正在从横屏恢复，跳过，等待 restorePlayerFromLandscape 处理")
                    return
                }
                
                // 检查播放器当前的内容是否匹配当前的 videoItem
                val player = playerView?.player
                val currentMediaItem = (player as? androidx.media3.common.Player)?.currentMediaItem
                val currentTag = currentMediaItem?.localConfiguration?.tag as? String
                val contentMismatch = currentTag != null && currentTag != itemId
                
                // 从横屏返回时，播放器池中可能已经有该视频的播放器
                // 或者有播放会话需要恢复
                // 总是尝试重新绑定，preparePlayer 会检查是否有会话并正确处理
                Log.d(TAG, "onStart: 准备播放器，videoId=$itemId, currentVideoId=${viewModel.uiState.value.currentVideoId}, hasPreparedPlayer=$hasPreparedPlayer, contentMismatch=$contentMismatch")
                if (viewModel.uiState.value.currentVideoId != null || hasPreparedPlayer || contentMismatch) {
                    // 横屏返回或内容不匹配，恢复播放器
                    if (contentMismatch) {
                        Log.w(TAG, "onStart: 播放器内容不匹配 (当前=$currentTag, 目标=$itemId)，强制重新绑定")
                    } else {
                        Log.d(TAG, "onStart: 检测到已有播放器或已准备，重新绑定")
                    }
                    reattachPlayer()
                } else {
                    // 首次加载 - 只准备播放器，不立即播放
                    Log.d(TAG, "onStart: 首次加载，准备播放器")
                    preparePlayerForFirstTime()
                }
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
            // 检查播放器当前的内容是否匹配当前的 videoItem
            val player = playerView?.player
            val currentMediaItem = (player as? androidx.media3.common.Player)?.currentMediaItem
            val currentTag = currentMediaItem?.localConfiguration?.tag as? String
            val contentMismatch = currentTag != null && currentTag != item.id
            
            val needsReprepare = forcePrepare || !hasPreparedPlayer || playerView?.player == null || contentMismatch
            if (needsReprepare) {
                if (contentMismatch) {
                    Log.w(TAG, "startPlaybackIfNeeded: 播放器内容不匹配 (当前=$currentTag, 目标=${item.id})，强制重新准备")
                }
                Log.d(
                    TAG,
                    "startPlaybackIfNeeded: (re)preparing player (force=$forcePrepare, hasPrepared=$hasPreparedPlayer, playerView.player=${playerView?.player}, contentMismatch=$contentMismatch)"
                )
                preparePlayerForFirstTime()
            } else {
                Log.d(TAG, "startPlaybackIfNeeded: player already prepared and attached, resuming")
                // Fragment 已可见，确保播放状态恢复
                // 从横屏返回时，即使会话中 playWhenReady=false，如果 Fragment 可见也应该播放
                viewModel.resume()
            }
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
        
        // 获取数据来源
        val videoSource = try {
            val parent = parentFragment
            if (parent is com.ucw.beatu.business.videofeed.presentation.ui.RecommendFragment) {
                val recommendViewModel = ViewModelProvider(parent)[
                    com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel::class.java
                ]
                recommendViewModel.getVideoSource(item.id)
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
        
        Log.d(TAG, "preparePlayerForFirstTime: 准备播放视频，视频ID=${item.id}, 视频URL=${item.videoUrl}, 数据来源=$videoSource, authorAvatar=${item.authorAvatar ?: "null"}")
        viewModel.playVideo(item.id, item.videoUrl)
        viewModel.preparePlayer(item.id, item.videoUrl, pv)
        hasPreparedPlayer = true
        Log.d(TAG, "preparePlayerForFirstTime: hasPreparedPlayer=$hasPreparedPlayer, playerView.player=${pv.player}")
    }

    // ✅ 修复：重绑定播放器逻辑（统一走 preparePlayer，让 PlaybackSession 决定是否续播）
    private fun reattachPlayer() {
        if (!isAdded) {
            Log.w(TAG, "reattachPlayer: Fragment not added, skip")
            return
        }
        val item = videoItem ?: run {
            Log.w(TAG, "reattachPlayer: videoItem is null, skip")
            return
        }
        val pv = playerView ?: run {
            Log.w(TAG, "reattachPlayer: playerView is null, skip")
            return
        }
        if (item.type == FeedContentType.IMAGE_POST) {
            // 图文内容目前不支持横竖屏热切换，忽略重绑定
            Log.d(TAG, "reattachPlayer: skip for image post ${item.id}")
            return
        }
        Log.d(TAG, "reattachPlayer: re-preparing video ${item.id}")
        // 确保 PlayerView 已经准备好，然后再绑定播放器
        // 从横屏返回竖屏时，需要等待 View 布局完成
        pv.post {
            if (isAdded && pv != null) {
                // 确保 PlayerView 的 player 为 null，避免绑定冲突
                if (pv.player != null && pv.player !== viewModel.mediaPlayer()) {
                    Log.d(TAG, "reattachPlayer: 清理 PlayerView 上之前的播放器")
                    pv.player = null
                }
                // 直接调用 preparePlayer，它会检查是否有会话并正确处理
                // preparePlayer 内部会调用 playVideo 来设置状态，但不会释放播放器（因为播放器在池中）
                viewModel.preparePlayer(item.id, item.videoUrl, pv)
                hasPreparedPlayer = true
                Log.d(TAG, "reattachPlayer: hasPreparedPlayer=$hasPreparedPlayer, playerView.player=${pv.player}")
            } else {
                Log.w(TAG, "reattachPlayer: Fragment not added or PlayerView is null after post")
            }
        }
    }

    /**
     * 处理横屏按钮点击事件
     * 添加防抖处理和状态检查
     */
    private fun handleFullScreenButtonClick() {
        // 防抖处理：500ms内只响应一次点击
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFullScreenClickTime < 500) {
            Log.d(TAG, "handleFullScreenButtonClick: 点击过快，忽略")
            return
        }
        lastFullScreenClickTime = currentTime

        // 检查是否正在导航中
        if (navigatingToLandscape) {
            Log.d(TAG, "handleFullScreenButtonClick: 正在导航中，忽略")
            return
        }

        // 检查视频项是否有效
        val item = videoItem ?: run {
            Log.e(TAG, "handleFullScreenButtonClick: videoItem is null")
            return
        }

        // 检查是否为图文内容
        if (item.type == FeedContentType.IMAGE_POST) {
            Log.d(TAG, "handleFullScreenButtonClick: 图文内容不支持横屏")
            return
        }

        // 执行横屏切换（所有视频内容都支持横屏）
        openLandscapeMode()
    }

    fun openLandscapeMode() {
        val navController = findParentNavController()
        if (navController == null) {
            Log.e(TAG, "NavController not found, cannot open landscape mode")
            return
        }
        val item = videoItem ?: run {
            Log.e(TAG, "openLandscapeMode: videoItem null")
            return
        }

        // 优化：先保存会话和切换播放器（这些操作很快）
        viewModel.persistPlaybackSession()
        viewModel.mediaPlayer()?.let { player ->
            PlayerView.switchTargetView(player, playerView, null)
        }

        // 根据当前导航目的地选择正确的action
        val currentDestId = navController.currentDestination?.id
        val userWorksViewerDestId = NavigationHelper.getResourceId(requireContext(), NavigationIds.USER_WORKS_VIEWER)

        val actionId = if (currentDestId == userWorksViewerDestId) {
            // 从用户作品观看页面导航到横屏
            NavigationHelper.getResourceId(requireContext(), NavigationIds.ACTION_USER_WORKS_VIEWER_TO_LANDSCAPE)
        } else {
            // 从Feed页面导航到横屏（默认）
            // 优化：预先获取资源ID，避免在导航时查找
            NavigationHelper.getResourceId(requireContext(), NavigationIds.ACTION_FEED_TO_LANDSCAPE)
        }

        if (actionId == 0) {
            Log.e(TAG, "Navigation action not found: currentDestId=$currentDestId, userWorksViewerDestId=$userWorksViewerDestId")
            return
        }

        // 如果从用户作品观看页面导航（包括搜索、历史、收藏、点赞等），获取视频列表并传递
        val videoList = if (currentDestId == userWorksViewerDestId) {
            val router = RouterRegistry.getUserWorksViewerRouter()
            val list = router?.getCurrentVideoList()
            Log.d(TAG, "openLandscapeMode: from userWorksViewer, videoListSize=${list?.size ?: 0}")
            list
        } else {
            Log.d(TAG, "openLandscapeMode: from feed or other page, no video list restriction")
            null
        }

        val currentIndex = if (currentDestId == userWorksViewerDestId) {
            val router = RouterRegistry.getUserWorksViewerRouter()
            val index = router?.getCurrentVideoIndex() ?: 0
            Log.d(TAG, "openLandscapeMode: from userWorksViewer, currentIndex=$index")
            index
        } else {
            0
        }

        // 优化：预先构建参数
        val args = bundleOf(
            LandscapeLaunchContract.EXTRA_VIDEO_ID to item.id,
            LandscapeLaunchContract.EXTRA_VIDEO_URL to item.videoUrl,
            LandscapeLaunchContract.EXTRA_VIDEO_TITLE to item.title,
            LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR to item.authorName,
            LandscapeLaunchContract.EXTRA_VIDEO_LIKE to item.likeCount,
            LandscapeLaunchContract.EXTRA_VIDEO_COMMENT to item.commentCount,
            LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE to item.favoriteCount,
            LandscapeLaunchContract.EXTRA_VIDEO_SHARE to item.shareCount
        ).apply {
            // 如果有视频列表，传递视频列表和当前索引
            if (videoList != null) {
                putParcelableArrayList(LandscapeLaunchContract.EXTRA_VIDEO_LIST, ArrayList(videoList))
                putInt(LandscapeLaunchContract.EXTRA_CURRENT_INDEX, currentIndex)
            }
        }

        navigatingToLandscape = true

        // 优化：使用post延迟导航，让播放器切换先完成，减少卡顿
        view?.post {
            runCatching { navController.navigate(actionId, args) }
                .onFailure {
                    navigatingToLandscape = false
                    Log.e(TAG, "Failed to navigate to landscape fragment", it)
                }
        }
    }

    /**
     * 从横屏返回后恢复播放器
     * 由于是popBackStack，生命周期函数不会触发，需要手动调用此方法
     */
    fun restorePlayerFromLandscape() {
        if (!isAdded || videoItem == null) {
            Log.w(TAG, "restorePlayerFromLandscape: Fragment not ready, skip")
            return
        }

        val item = videoItem ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            // 图文内容不支持横竖屏切换
            Log.d(TAG, "restorePlayerFromLandscape: skip for image post ${item.id}")
            return
        }

        val pv = playerView ?: run {
            Log.w(TAG, "restorePlayerFromLandscape: playerView is null, skip")
            return
        }

        Log.d(TAG, "restorePlayerFromLandscape: 恢复播放器，videoId=${item.id}")

        // 设置标志，防止 onStart 重复处理
        isRestoringFromLandscape = true

        // 优化：使用post延迟执行，确保View已经布局完成，减少卡顿
        pv.post {
            if (!isAdded || pv == null) {
                Log.w(TAG, "restorePlayerFromLandscape: Fragment not added or PlayerView is null after post")
                isRestoringFromLandscape = false
                return@post
            }

            // 使用onHostResume方法，它会检查是否有播放会话并恢复
            // onHostResume会从PlaybackSessionStore中获取会话信息，并绑定播放器到view
            viewModel.onHostResume(pv)
            hasPreparedPlayer = true
            Log.d(TAG, "restorePlayerFromLandscape: 播放器已恢复，playerView.player=${pv.player}")

            // 优化：延迟恢复播放，让UI先渲染完成
            pv.postDelayed({
                if (isAdded && isViewVisibleOnScreen()) {
                    viewModel.resume()
                }
                // 恢复完成后，重置标志
                isRestoringFromLandscape = false
            }, 100) // 延迟100ms，让UI先渲染，并确保播放器完全恢复
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