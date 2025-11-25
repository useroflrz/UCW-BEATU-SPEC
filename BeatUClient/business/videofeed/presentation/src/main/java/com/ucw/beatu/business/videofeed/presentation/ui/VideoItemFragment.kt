package com.ucw.beatu.business.videofeed.presentation.ui

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

/**
 * 单个视频项 Fragment
 * 用于在 ViewPager2 中显示单个视频
 */
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
        
        // 初始化视图
        playerView = view.findViewById(R.id.player_view)
        playButton = view.findViewById(R.id.iv_play_button)
        
        // 更新视频信息
        videoItem?.let { item ->
            view.findViewById<android.widget.TextView>(R.id.tv_video_title)?.text = item.title
            view.findViewById<android.widget.TextView>(R.id.tv_channel_name)?.text = item.authorName
            view.findViewById<android.widget.TextView>(R.id.tv_like_count)?.text = item.likeCount.toString()
            view.findViewById<android.widget.TextView>(R.id.tv_comment_count)?.text = item.commentCount.toString()
            view.findViewById<android.widget.TextView>(R.id.tv_favorite_count)?.text = item.favoriteCount.toString()
            view.findViewById<android.widget.TextView>(R.id.tv_share_count)?.text = item.shareCount.toString()
        }
        
        // 观察 ViewModel 状态
        observeViewModel()
        
        // 设置播放按钮点击事件
        playButton?.setOnClickListener {
            viewModel.togglePlayPause()
        }
        
        // 设置底部交互按钮点击事件
        view.findViewById<View>(R.id.iv_like)?.setOnClickListener {
            // TODO: 点赞功能
        }
        
        view.findViewById<View>(R.id.iv_favorite)?.setOnClickListener {
            // TODO: 收藏功能
        }
        
        view.findViewById<View>(R.id.iv_comment)?.setOnClickListener {
            // TODO: 打开评论
        }
        
        view.findViewById<View>(R.id.iv_share)?.setOnClickListener {
            // TODO: 分享功能
        }
        
        view.findViewById<View>(R.id.iv_fullscreen)?.setOnClickListener {
            openLandscapeMode()
        }
        
        // 延迟加载视频，确保视图完全初始化
        view.post {
            if (isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                loadVideo()
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 更新播放器显示状态
                    playerView?.visibility = View.VISIBLE
                    
                    // 更新播放按钮显示状态（播放时隐藏，暂停时显示）
                    playButton?.visibility = if (state.isPlaying) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                    
                    // 处理错误
                    state.error?.let { error ->
                        Log.e(TAG, "播放错误: $error")
                    }
                }
            }
        }
    }
    
    private fun loadVideo() {
        if (!isAdded || view == null || playerView == null || videoItem == null) {
            Log.w(TAG, "Fragment not ready, skip loading video")
            return
        }
        
        try {
            val item = videoItem!!
            Log.d(TAG, "Loading video: ${item.id} - ${item.videoUrl}")
            
            playerView?.let { pv ->
                viewModel.playVideo(item.id, item.videoUrl)
                viewModel.preparePlayer(item.id, item.videoUrl, pv)
            } ?: run {
                Log.e(TAG, "PlayerView is null, cannot load video")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading video", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (!navigatingToLandscape) {
            viewModel.pause()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (isAdded && viewModel.uiState.value.currentVideoId != null) {
            viewModel.onHostResume(playerView)
        }
        navigatingToLandscape = false
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.releaseCurrentPlayer()
        playerView = null
        playButton = null
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

        val actionId = NavigationHelper.getResourceId(
            requireContext(),
            NavigationIds.ACTION_FEED_TO_LANDSCAPE
        )
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
        return runCatching { parentFragment?.findNavController() ?: findNavController() }
            .getOrNull()
    }
}

