package com.ucw.beatu.business.videofeed.presentation.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.viewmodel.VideoItemViewModel
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoItemFragment : Fragment() {

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
    private var playButton: View? = null
    private var videoItem: VideoItem? = null
    private var navigatingToLandscape = false
    private var hasPreparedPlayer = false

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
        playButton = view.findViewById(R.id.iv_play_button)

        videoItem?.let { item ->
            view.findViewById<android.widget.TextView>(R.id.tv_video_title)?.text = item.title
            view.findViewById<android.widget.TextView>(R.id.tv_channel_name)?.text = item.authorName
            
            // 初始化互动状态
            viewModel.initInteractionState(
                isLiked = false, // TODO: 从VideoItem中获取实际状态
                isFavorited = false, // TODO: 从VideoItem中获取实际状态
                likeCount = item.likeCount.toLong(),
                favoriteCount = item.favoriteCount.toLong()
            )
        }

        observeViewModel()

        playButton?.setOnClickListener { viewModel.togglePlayPause() }
        view.findViewById<View>(R.id.iv_like)?.setOnClickListener { viewModel.toggleLike() }
        view.findViewById<View>(R.id.iv_favorite)?.setOnClickListener { viewModel.toggleFavorite() }
        view.findViewById<View>(R.id.iv_comment)?.setOnClickListener { /* TODO: 打开评论弹层 */ }
        view.findViewById<View>(R.id.iv_share)?.setOnClickListener { /* TODO: 打开分享弹层 */ }
        view.findViewById<View>(R.id.iv_fullscreen)?.setOnClickListener { openLandscapeMode() }

    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    playerView?.visibility = View.VISIBLE
                    playButton?.visibility = if (state.isPlaying) View.GONE else View.VISIBLE
                    
                    // 更新互动状态UI
                    view?.findViewById<android.widget.TextView>(R.id.tv_like_count)?.text = state.likeCount.toString()
                    view?.findViewById<android.widget.TextView>(R.id.tv_favorite_count)?.text = state.favoriteCount.toString()
                    
                    // 更新点赞/收藏图标状态（TODO: 需要根据实际布局调整）
                    // view.findViewById<ImageView>(R.id.iv_like)?.setImageResource(
                    //     if (state.isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
                    // )
                    // view.findViewById<ImageView>(R.id.iv_favorite)?.setImageResource(
                    //     if (state.isFavorited) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
                    // )
                    
                    state.error?.let { error -> Log.e(TAG, "播放错误: $error") }
                }
            }
        }
    }

    // ✅ 修复：onStart() 逻辑 - 只准备播放器，不立即播放
    override fun onStart() {
        super.onStart()
        if (viewModel.uiState.value.currentVideoId != null) {
            // 横屏返回，恢复播放器
            reattachPlayer()
        } else {
            // 首次加载 - 只准备播放器，不立即播放
            preparePlayerForFirstTime()
        }
        // ✅ 修复：不在这里立即播放，等待 Fragment 真正可见时再播放（由 handlePageSelected() 触发）
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
        playButton = null
    }

    fun onParentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            startPlaybackIfNeeded()
        } else if (hasPreparedPlayer) {
            Log.d(TAG, "onParentVisibilityChanged: fragment hidden, pausing ${videoItem?.id}")
            viewModel.pause()
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
    
    /**
     * 检查可见性并在需要时暂停：如果 View 不在屏幕上可见则暂停
     */
    private fun checkVisibilityAndPauseIfNeeded() {
        if (!isViewVisibleOnScreen() && hasPreparedPlayer) {
            viewModel.pause()
            Log.d(TAG, "Video not visible on screen in onResume, paused")
        }
    }
    
    /**
     * 检测 View 是否真正在屏幕上可见
     * 使用 getGlobalVisibleRect 来检测 View 是否在屏幕范围内
     */
    private fun isViewVisibleOnScreen(): Boolean {
        val view = view ?: return false
        if (!isAdded || !isVisible || view.visibility != View.VISIBLE) {
            return false
        }
        
        // 检查 View 是否在屏幕上可见
        val rect = android.graphics.Rect()
        val isVisible = view.getGlobalVisibleRect(rect)
        
        if (!isVisible) {
            return false
        }
        
        // 检查可见区域是否足够大（至少 10% 可见）
        val viewArea = view.width * view.height
        val visibleArea = rect.width() * rect.height()
        val visibilityRatio = if (viewArea > 0) {
            visibleArea.toFloat() / viewArea.toFloat()
        } else {
            0f
        }
        
        val isSignificantlyVisible = visibilityRatio >= 0.1f
        
        if (!isSignificantlyVisible) {
            Log.d(TAG, "View visibility ratio too low: $visibilityRatio")
        }
        
        return isSignificantlyVisible
    }

    private fun startPlaybackIfNeeded(forcePrepare: Boolean = false) {
        if (!isAdded || playerView == null || videoItem == null) {
            Log.w(TAG, "Fragment not ready, skip startPlayback")
            return
        }

        val needsReprepare = forcePrepare || !hasPreparedPlayer || playerView?.player == null
        if (needsReprepare) {
            Log.d(TAG, "startPlaybackIfNeeded: (re)preparing player (force=$forcePrepare, hasPrepared=$hasPreparedPlayer, playerView.player=${playerView?.player})")
            preparePlayerForFirstTime()
        } else {
            Log.d(TAG, "startPlaybackIfNeeded: player already prepared and attached, resuming")
        }
        // Fragment 已可见，确保播放状态恢复
        viewModel.resume()
    }

    // ✅ 修复：首次加载逻辑
    private fun preparePlayerForFirstTime() {
        val item = videoItem ?: return
        val pv = playerView ?: return
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
}