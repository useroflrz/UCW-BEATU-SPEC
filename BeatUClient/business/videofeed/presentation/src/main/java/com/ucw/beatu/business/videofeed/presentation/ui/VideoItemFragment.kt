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
import java.util.ArrayList
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
import com.ucw.beatu.shared.common.util.NumberFormatter
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
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.user.domain.model.User
import javax.inject.Inject

@AndroidEntryPoint
class VideoItemFragment : BaseFeedItemFragment() {
    
    @Inject
    lateinit var userRepository: UserRepository

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
    private var isRestoringFromLandscape = false
    private var isRestoringFromUserWorksViewer = false

    // 用户信息展示相关
    private var userInfoOverlay: View? = null
    private var rootLayout: ConstraintLayout? = null
    private var isUserInfoVisible = false
    private var userProfileFragment: Fragment? = null
    private var userProfileDialogFragment: UserProfileDialogFragment? = null
    
    // 全屏按钮相关
    private var fullScreenButton: android.widget.ImageView? = null
    private var lastFullScreenClickTime: Long = 0L

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
         
         // 获取全屏按钮
         fullScreenButton = sharedControlsRoot?.findViewById(com.ucw.beatu.shared.designsystem.R.id.iv_fullscreen)

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
            )?.text = NumberFormatter.formatCount(item.likeCount)
             val shareCountTextView = sharedControlsRoot?.findViewById<android.widget.TextView>(
                 com.ucw.beatu.shared.designsystem.R.id.tv_share_count
             )
             shareCountTextView?.text = NumberFormatter.formatCount(item.shareCount)
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_favorite_count
            )?.text = NumberFormatter.formatCount(item.favoriteCount)
            sharedControlsRoot?.findViewById<android.widget.TextView>(
                com.ucw.beatu.shared.designsystem.R.id.tv_comment_count
            )?.text = NumberFormatter.formatCount(item.commentCount)
            
            // 初始化互动状态
            viewModel.initInteractionState(
                videoId = item.id,
                isLiked = item.isLiked,
                isFavorited = item.isFavorited,
                likeCount = item.likeCount.toLong(),
                favoriteCount = item.favoriteCount.toLong()
            )

            val isImagePost = item.type == FeedContentType.IMAGE_POST

            // 图文内容：隐藏视频播放器，显示图片轮播；保留统一的底部交互区
            if (isImagePost) {
                playerView?.visibility = View.GONE
                imagePager?.visibility = View.VISIBLE
                setupImagePager(item.imageUrls)
            } else {
                // 视频内容：显示播放器，隐藏图文容器
                playerView?.visibility = View.VISIBLE
                imagePager?.visibility = View.GONE
                // ✅ 横屏按钮显示逻辑：
                // - 主页（RecommendFragment）：只有 LANDSCAPE 视频才显示横屏按钮
                // - 个人弹窗（UserWorksViewerFragment）：所有 LANDSCAPE 视频都显示横屏按钮（确保可以切换横屏）
                val isInUserWorksViewer = RouterRegistry.getUserWorksViewerRouter() != null
                fullScreenButton?.visibility = if (item.orientation == VideoOrientation.LANDSCAPE) {
                    // LANDSCAPE 视频：在主页和个人弹窗中都显示横屏按钮
                    View.VISIBLE
                } else {
                    // PORTRAIT 视频：不显示横屏按钮（主页和个人弹窗都遵循此规则）
                    View.GONE
                }
                Log.d(TAG, "设置横屏按钮可见性: orientation=${item.orientation}, isInUserWorksViewer=$isInUserWorksViewer, visibility=${fullScreenButton?.visibility}")
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

            override fun onLandscapeClicked() {
                openLandscapeMode()
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
        var previousIsPlaying = false
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
                    
                    // 当播放状态从 false 变为 true 时，打印当前播放视频信息
                    if (state.isPlaying && !previousIsPlaying) {
                        val item = videoItem
                        if (item != null) {
                            Log.d(TAG, "开始播放: ID=${item.id}, 标题=${item.title}")
                        }
                    }
                    previousIsPlaying = state.isPlaying

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
                // ✅ 修复：如果正在从横屏恢复，跳过 onStart 的逻辑，让 restorePlayerFromLandscape 处理
                if (isRestoringFromLandscape) {
                    Log.d(TAG, "onStart: 正在从横屏恢复，跳过，等待 restorePlayerFromLandscape 处理")
                    return
                }
                
                // ✅ 修复：如果检测到有播放会话，且 isRestoringFromLandscape 为 false，
                // 说明可能是从横屏返回，但 restorePlayerFromLandscape() 还没有被调用
                // 先设置标志，跳过处理，等待 restorePlayerFromLandscape() 处理
                val hasSession = viewModel.peekPlaybackSession(itemId) != null
                if (hasSession) {
                    Log.d(TAG, "onStart: 检测到播放会话，可能是从横屏返回，设置 isRestoringFromLandscape=true，等待 restorePlayerFromLandscape 处理，videoId=$itemId")
                    isRestoringFromLandscape = true
                    return
                }
                
                // 检查播放器当前的内容是否匹配当前的 videoItem
                val player = playerView?.player
                val currentMediaItem = (player as? androidx.media3.common.Player)?.currentMediaItem
                val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
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
        
        // ✅ 修复：从横屏返回后，检查是否有播放会话需要恢复
        // 如果存在播放会话，说明是从横屏返回的，需要恢复播放
        val item = videoItem
        if (item != null && item.type != FeedContentType.IMAGE_POST) {
            val session = viewModel.uiState.value.currentVideoId?.let { videoId ->
                // 使用 peek 而不是 consume，因为可能需要在其他地方使用
                // 实际上，preparePlayer 和 onHostResume 会 consume 会话
                // 这里只是检查是否存在会话，如果存在则触发恢复
                viewModel.mediaPlayer()?.let { player ->
                    // 检查播放器是否已经准备好，如果准备好了，说明可能已经恢复了
                    // 如果没有准备好，可能需要手动触发恢复
                    null
                }
            }
            
            // ✅ 修复：延迟检查，确保 Fragment 已经完全恢复
            view?.post {
                if (isResumed) {
                    // 检查是否有播放会话需要恢复
                    val videoId = item.id
                    val hasSession = viewModel.uiState.value.currentVideoId == videoId
                    Log.d(TAG, "onResume: 检查播放会话，videoId=$videoId, hasSession=$hasSession, currentVideoId=${viewModel.uiState.value.currentVideoId}")
                    
                    // 如果当前视频ID匹配，说明可能已经恢复了，直接检查可见性并播放
                    // 否则，可能需要手动触发恢复
                    if (hasSession) {
                        Log.d(TAG, "onResume: 播放会话已存在，检查可见性并播放")
                        checkVisibilityAndPlay()
                    } else {
                        // 如果没有会话，可能是首次加载，正常处理
                        Log.d(TAG, "onResume: 无播放会话，正常检查可见性并播放")
                        checkVisibilityAndPlay()
                    }
                }
            }
        } else {
            // 延迟检查 View 是否在屏幕上可见，确保 View 已经布局完成
            view?.post {
                if (isResumed) {
                    checkVisibilityAndPlay()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 停止图文轮播自动滚动
        stopImageAutoScroll()
        if (!navigatingToLandscape && hasPreparedPlayer) {
            Log.d(TAG, "暂停播放: ${videoItem?.id}")
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
            Log.d(TAG, "Fragment隐藏，暂停: ${videoItem?.id}")
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
            val currentTag = currentMediaItem?.localConfiguration?.tag as? Long  // ✅ 修改：tag 现在是 Long
            val contentMismatch = currentTag != null && currentTag != item.id
            
            val needsReprepare = forcePrepare || !hasPreparedPlayer || playerView?.player == null || contentMismatch
            if (needsReprepare) {
                if (contentMismatch) {
                    Log.w(TAG, "播放器内容不匹配，重新准备: ${item.id}")
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
                Log.d(TAG, "恢复播放: ${item.id}")
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
        Log.d(TAG, "重新绑定播放器: ${item.id}")
        // 确保 PlayerView 已经准备好，然后再绑定播放器
        // 从横屏返回竖屏时，需要等待 View 布局完成
        pv.post {
            if (isAdded) {
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
                Log.w(TAG, "reattachPlayer: Fragment not added after post")
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

        // 检查视频 orientation，只有 LANDSCAPE 视频才支持横屏
        if (item.orientation != VideoOrientation.LANDSCAPE) {
            Log.d(TAG, "handleFullScreenButtonClick: 当前视频不是 LANDSCAPE，不支持横屏")
            return
        }

        // 执行横屏切换（只有 LANDSCAPE 视频支持横屏）
        openLandscapeMode()
    }

    /**
     * 打开横屏模式
     * @param videoList 视频列表（可选，如果提供则横屏页面使用固定列表模式，不会跳出列表）
     * @param currentIndex 当前视频索引（可选，与 videoList 一起使用）
     */
    fun openLandscapeMode(videoList: List<com.ucw.beatu.shared.common.model.VideoItem>? = null, currentIndex: Int? = null) {
        val navController = findParentNavController()
        if (navController == null) {
            Log.e(TAG, "NavController not found, cannot open landscape mode")
            return
        }
        val item = videoItem ?: run {
            Log.e(TAG, "openLandscapeMode: videoItem null")
            return
        }
        
        // ✅ 对齐逻辑：通知父 Fragment 进入横屏模式，确保按钮横屏和自然横屏逻辑一致
        (parentFragment as? RecommendFragment)?.notifyEnterLandscapeMode()
        
        // ✅ 修复：如果从 UserWorksViewerFragment 调用，也通知它进入横屏模式
        val userWorksRouter = RouterRegistry.getUserWorksViewerRouter()
        if (userWorksRouter != null) {
            Log.d(TAG, "openLandscapeMode: 通知 UserWorksViewerFragment 进入横屏模式")
            userWorksRouter.notifyEnterLandscapeMode()
        } else {
            Log.d(TAG, "openLandscapeMode: UserWorksViewerRouter 未注册，可能不在 UserWorksViewerFragment 中")
        }

        // ✅ 修复：确保在保存会话时获取最新的播放进度
        // 先暂停播放器，确保进度不会继续更新，然后保存会话
        viewModel.mediaPlayer()?.let { player ->
            // 获取最新的播放进度（在暂停前）
            val currentPosition = player.currentPosition
            Log.d(TAG, "openLandscapeMode: 保存播放会话，当前播放位置=${currentPosition}ms")

            // 保存会话（会获取最新的播放进度）
            val session = viewModel.persistPlaybackSession()
            if (session != null) {
                Log.d(TAG, "openLandscapeMode: 播放会话已保存，视频ID=${session.videoId}，位置=${session.positionMs}ms，是否准备播放=${session.playWhenReady}")
            } else {
                Log.w(TAG, "openLandscapeMode: 播放会话保存失败，可能播放器未准备好")
            }

            // 解绑播放器，但不释放（保持播放器在池中）
            PlayerView.switchTargetView(player, playerView, null)
        } ?: run {
            Log.w(TAG, "openLandscapeMode: 播放器为null，无法保存会话")
        }

        // ✅ 修复：根据当前导航目的地动态选择正确的 action
        val context = requireContext()
        val currentDestId = navController.currentDestination?.id
        val userWorksViewerDestId = NavigationHelper.getResourceId(context, NavigationIds.USER_WORKS_VIEWER)

        // 根据当前所在的 destination 选择正确的 action
        val actionId = if (currentDestId == userWorksViewerDestId) {
            // 从用户作品观看页面导航到横屏
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_USER_WORKS_VIEWER_TO_LANDSCAPE)
        } else {
            // 从Feed页面导航到横屏（默认）
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_FEED_TO_LANDSCAPE)
        }

        if (actionId == 0) {
            Log.e(TAG, "Navigation action not found: currentDestId=$currentDestId, userWorksViewerDestId=$userWorksViewerDestId")
            return
        }

        // ✅ 修复：如果从 UserWorksViewerFragment 调用且未提供视频列表，尝试从 Router 获取
        val finalVideoList = videoList ?: run {
            if (currentDestId == userWorksViewerDestId) {
                val userWorksRouter = RouterRegistry.getUserWorksViewerRouter()
                userWorksRouter?.getCurrentVideoList()?.also {
                    Log.d(TAG, "openLandscapeMode: 从 UserWorksViewerRouter 获取视频列表，数量=${it.size}")
                }
            } else {
                null
            }
        }

        val finalCurrentIndex = currentIndex ?: run {
            if (currentDestId == userWorksViewerDestId) {
                val userWorksRouter = RouterRegistry.getUserWorksViewerRouter()
                userWorksRouter?.getCurrentVideoIndex()?.also {
                    Log.d(TAG, "openLandscapeMode: 从 UserWorksViewerRouter 获取当前索引=$it")
                }
            } else {
                null
            }
        }

        // 优化：预先构建参数
        val args = Bundle().apply {
            // ✅ 修复：videoId 现在是 Long 类型，使用 putLong 而不是 bundleOf（bundleOf 可能不支持 Long）
            putLong(LandscapeLaunchContract.EXTRA_VIDEO_ID, item.id)
            putString(LandscapeLaunchContract.EXTRA_VIDEO_URL, item.videoUrl)
            putString(LandscapeLaunchContract.EXTRA_VIDEO_TITLE, item.title)
            putString(LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR, item.authorName)
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_LIKE, item.likeCount.toInt())
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_COMMENT, item.commentCount.toInt())
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE, item.favoriteCount.toInt())
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_SHARE, item.shareCount.toInt())
            
            // ✅ 新增：记录来源页面，用于退出时返回到正确的页面
            val sourceDestinationId = navController.currentDestination?.id ?: 0
            putInt(LandscapeLaunchContract.EXTRA_SOURCE_DESTINATION, sourceDestinationId)
            Log.d(TAG, "openLandscapeMode: 记录来源页面 ID=$sourceDestinationId")
        }

        // ✅ 新增：如果提供了视频列表，传递给横屏页面（用于固定列表模式，防止跳出列表）
        if (finalVideoList != null && finalVideoList.isNotEmpty()) {
            val videoListParcelable = ArrayList(finalVideoList)
            args.putParcelableArrayList(LandscapeLaunchContract.EXTRA_VIDEO_LIST, videoListParcelable)
            val index = finalCurrentIndex ?: finalVideoList.indexOfFirst { it.id == item.id }.let {
                if (it == -1) 0 else it
            }
            args.putInt(LandscapeLaunchContract.EXTRA_CURRENT_INDEX, index.coerceIn(0, finalVideoList.lastIndex))
            Log.d(TAG, "openLandscapeMode: 传递视频列表到横屏页面，数量=${finalVideoList.size}，当前索引=$index")
        }

        navigatingToLandscape = true
        
        // 优化：使用post延迟导航，让播放器切换先完成，减少卡顿
        view?.post {
            runCatching { navController.navigate(actionId, args) }
                .onFailure {
                    navigatingToLandscape = false
                    // ✅ 对齐逻辑：如果导航失败，需要通知父 Fragment 退出横屏模式
                    (parentFragment as? RecommendFragment)?.notifyExitLandscapeMode()
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
        
        Log.d(TAG, "restorePlayerFromLandscape: ✅ 开始恢复播放器，videoId=${item.id}, videoUrl=${item.videoUrl}")
        
        // ✅ 修复：先检查播放会话是否存在
        val sessionBeforePlay = viewModel.peekPlaybackSession(item.id)
        Log.d(TAG, "restorePlayerFromLandscape: 检查播放会话，videoId=${item.id}，会话存在=${sessionBeforePlay != null}，会话位置=${sessionBeforePlay?.positionMs ?: 0}ms")
        
        // 设置标志，防止 onStart 重复处理
        isRestoringFromLandscape = true

        // ✅ 修复：先设置currentVideoId，确保onHostResume能正确获取会话信息
        // 注意：playVideo 是异步的，需要等待 currentVideoId 设置完成后再调用 onHostResume
        Log.d(TAG, "restorePlayerFromLandscape: 调用 playVideo，videoId=${item.id}")
        viewModel.playVideo(item.id, item.videoUrl)

        // 优化：使用post延迟执行，确保View已经布局完成，并等待 playVideo 完成
        pv.post {
            if (!isAdded) {
                Log.w(TAG, "restorePlayerFromLandscape: Fragment not added after post")
                isRestoringFromLandscape = false
                return@post
            }

            // ✅ 修复：等待 playVideo 完成（currentVideoId 设置完成）
            // 使用 viewLifecycleOwner.lifecycleScope 确保在正确的生命周期中执行
            viewLifecycleOwner.lifecycleScope.launch {
                // 等待 playVideo 完成：轮询检查 currentVideoId 是否已设置
                var retryCount = 0
                val maxRetries = 50 // 最多等待 500ms (50 * 10ms)
                while (retryCount < maxRetries) {
                    val currentVideoId = viewModel.uiState.value.currentVideoId
                    if (currentVideoId == item.id) {
                        Log.d(TAG, "restorePlayerFromLandscape: playVideo 完成，currentVideoId=$currentVideoId，重试次数=$retryCount")
                        break
                    }
                    delay(10)
                    retryCount++
                }

                // 再次检查 Fragment 是否仍然有效
                if (!isAdded) {
                    Log.w(TAG, "restorePlayerFromLandscape: Fragment not added after delay")
                    isRestoringFromLandscape = false
                    return@launch
                }

                // 验证 currentVideoId 是否已设置
                val finalVideoId = viewModel.uiState.value.currentVideoId
                if (finalVideoId != item.id) {
                    Log.w(TAG, "restorePlayerFromLandscape: ⚠️ currentVideoId 未正确设置，期望=${item.id}，实际=$finalVideoId，继续尝试恢复")
                }

                // ✅ 修复：playVideo() 会触发 preparePlayer()，而 preparePlayer() 已经处理了播放会话
                // 所以这里只需要调用 onHostResume() 来绑定播放器到 PlayerView，不需要再次获取会话
                // 但是，如果 preparePlayer() 没有找到会话（可能已经被消费），我们需要检查一下
                val sessionBeforeResume = viewModel.peekPlaybackSession(item.id)
                Log.d(TAG, "restorePlayerFromLandscape: 调用 onHostResume 前，检查播放会话，videoId=${item.id}，会话存在=${sessionBeforeResume != null}，会话位置=${sessionBeforeResume?.positionMs ?: 0}ms")
                
                // ✅ 修复：如果 preparePlayer() 已经处理了会话，onHostResume() 只需要绑定播放器
                // 如果 preparePlayer() 没有找到会话，onHostResume() 会尝试再次获取（虽然可能已经被消费）
                Log.d(TAG, "restorePlayerFromLandscape: 调用 onHostResume，videoId=${item.id}，用于绑定播放器到 PlayerView")
                viewModel.onHostResume(pv)
                hasPreparedPlayer = true
                
                // ✅ 修复：延迟检查播放位置，确保 seekTo 已完成
                // 由于 preparePlayer() 中的 applyPlaybackSession() 可能还在等待播放器准备好，
                // 我们需要等待一段时间后再检查位置
                pv.postDelayed({
                    val restoredPosition = viewModel.mediaPlayer()?.currentPosition ?: 0
                    val expectedPosition = sessionBeforeResume?.positionMs ?: 0
                    val positionDiff = kotlin.math.abs(restoredPosition - expectedPosition)
                    Log.d(TAG, "restorePlayerFromLandscape: 播放器已恢复，playerView.player=${pv.player}, currentPosition=${restoredPosition}ms, expectedPosition=${expectedPosition}ms, diff=${positionDiff}ms")
                    
                    // ✅ 修复：如果位置差异较大，可能是 seekTo 还没有执行，再等待一下
                    if (sessionBeforeResume != null && positionDiff > 100) {
                        Log.w(TAG, "restorePlayerFromLandscape: 位置差异较大，可能 seekTo 还未执行，等待更长时间")
                        pv.postDelayed({
                            val finalPosition = viewModel.mediaPlayer()?.currentPosition ?: 0
                            val finalDiff = kotlin.math.abs(finalPosition - expectedPosition)
                            Log.d(TAG, "restorePlayerFromLandscape: 延迟检查后，currentPosition=${finalPosition}ms, expectedPosition=${expectedPosition}ms, diff=${finalDiff}ms")
                        }, 200)
                    }
                }, 200) // 延迟200ms，确保 seekTo 已完成

                // ✅ 修复：不需要额外调用 resume()，因为 applyPlaybackSession 已经根据会话的 playWhenReady 设置了播放状态
                // 如果会话中 playWhenReady = true，applyPlaybackSession 会调用 player.play()
                // 如果会话中 playWhenReady = false，applyPlaybackSession 会调用 player.pause()
                // 延迟重置标志，确保会话恢复完成
                pv.postDelayed({
                    // 恢复完成后，重置标志
                    isRestoringFromLandscape = false
                    val finalPosition = viewModel.mediaPlayer()?.currentPosition ?: 0
                    Log.d(TAG, "restorePlayerFromLandscape: 恢复完成，当前播放位置=${finalPosition}ms")
                }, 50) // 延迟50ms，让UI先渲染
            }
        }
    }

    /**
     * 从用户弹窗返回后恢复播放器
     * 由于是popBackStack，生命周期函数不会触发，需要手动调用此方法
     * 使用与 restorePlayerFromLandscape() 相同的逻辑
     */
    fun restorePlayerFromUserWorksViewer() {
        if (!isAdded || videoItem == null) {
            Log.w(TAG, "restorePlayerFromUserWorksViewer: Fragment not ready, skip")
            return
        }

        val item = videoItem ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            // 图文内容不支持此恢复逻辑
            Log.d(TAG, "restorePlayerFromUserWorksViewer: skip for image post ${item.id}")
            return
        }

        val pv = playerView ?: run {
            Log.w(TAG, "restorePlayerFromUserWorksViewer: playerView is null, skip")
            return
        }

        Log.d(TAG, "restorePlayerFromUserWorksViewer: 恢复播放器，videoId=${item.id}")

        // 设置标志，防止 onStart 重复处理
        isRestoringFromUserWorksViewer = true

        // 先设置currentVideoId，确保onHostResume能正确获取会话信息
        // 注意：必须先调用playVideo设置currentVideoId，否则onHostResume会返回
        viewModel.playVideo(item.id, item.videoUrl)

        // 优化：使用post延迟执行，确保View已经布局完成，减少卡顿
        pv.post {
            if (!isAdded) {
                Log.w(TAG, "restorePlayerFromLandscape: Fragment not added after post")
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
            }, 50) // 延迟50ms，让UI先渲染
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

        // 异步获取用户完整数据
        val userId = if (authorId.isNotEmpty()) authorId else authorName
        lifecycleScope.launch {
            try {
                // 优先通过 userId 获取用户数据（因为 userId 更准确）
                var user: User? = null
                
                // 1. 如果 authorId 不为空，优先通过 authorId 查询
                if (authorId.isNotEmpty()) {
                    user = userRepository.getUserById(authorId)
                    Log.d(TAG, "Trying to get user by ID: $authorId, result: ${if (user != null) "found" else "not found"}")
                }
                
                // 2. 如果通过 authorId 没找到，尝试通过 authorName 查询（可能是用户名）
                if (user == null && authorName.isNotEmpty() && authorName != authorId) {
                    user = userRepository.getUserByName(authorName)
                    Log.d(TAG, "Trying to get user by name: $authorName, result: ${if (user != null) "found" else "not found"}")
                }
                
                // 3. 如果还是没找到，且 authorName 看起来像用户ID（纯数字），尝试作为用户ID查询
                if (user == null && authorName.isNotEmpty() && authorName.all { it.isDigit() }) {
                    user = userRepository.getUserById(authorName)
                    Log.d(TAG, "Trying to get user by ID (from authorName): $authorName, result: ${if (user != null) "found" else "not found"}")
                }
                
                if (user != null) {
                    // 如果获取到用户数据，创建包含完整数据的 Bundle
                    Log.d(TAG, "User found: id=${user.id}, name=${user.name}, avatarUrl=${user.avatarUrl}")
                    val userData = Bundle().apply {
                        putString("id", user.id)
                        putString("name", user.name)
                        putString("avatarUrl", user.avatarUrl)
                        putString("bio", user.bio)
                        putLong("likesCount", user.likesCount)
                        putLong("followingCount", user.followingCount)
                        putLong("followersCount", user.followersCount)
                    }
                    // 使用完整用户数据创建弹窗
                    userProfileDialogFragment = UserProfileDialogFragment.newInstanceWithData(userData)
                } else {
                    // 如果获取不到用户数据，仍然尝试使用 userId 创建弹窗（UserProfileFragment 会尝试从远程获取）
                    Log.w(TAG, "User not found in local DB: userId=$userId, authorName=$authorName, will try remote fetch")
                    userProfileDialogFragment = UserProfileDialogFragment.newInstance(userId, authorName)
                }
            } catch (e: Exception) {
                // 获取用户数据失败，使用 fallback
                Log.e(TAG, "Failed to get user data: userId=$userId, authorName=$authorName", e)
                userProfileDialogFragment = UserProfileDialogFragment.newInstance(userId, authorName)
            }
            
            // 设置弹窗监听器
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

            if (router != null && currentUserId == userId) {
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

        // ✅ 修复：在导航到用户弹窗前，先保存播放会话，确保返回时能恢复
        // 使用与横屏切换相同的逻辑
        if (hasPreparedPlayer && videoItem?.type != FeedContentType.IMAGE_POST) {
            Log.d(TAG, "navigateToUserWorksViewer: 保存播放会话，videoId=${videoItem?.id}")
            viewModel.persistPlaybackSession()
            // 解绑播放器，但不释放（保持播放器在池中）
            viewModel.mediaPlayer()?.let { player ->
                PlayerView.switchTargetView(player, playerView, null)
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

        // ✅ 修复：记录来源页面 ID 和来源视频 ID，用于返回时决定返回到哪里并定位到原来的视频
        val currentDestinationId = navController.currentDestination?.id ?: 0
        val currentVideoId = videoItem?.id ?: -1L
        val bundle = bundleOf(
            "user_id" to userId,
            "initial_index" to initialIndex,
            "video_list" to videoItems,
            "source_destination" to currentDestinationId, // ✅ 修复：传递来源页面 ID
            "source_video_id" to currentVideoId // ✅ 修复：传递来源视频 ID，用于返回时定位到原来的视频
        )
        Log.d(TAG, "navigateToUserWorksViewer: 传递来源页面 ID=$currentDestinationId, 来源视频 ID=$currentVideoId")
        navController.navigate(actionId, bundle)
    }

}