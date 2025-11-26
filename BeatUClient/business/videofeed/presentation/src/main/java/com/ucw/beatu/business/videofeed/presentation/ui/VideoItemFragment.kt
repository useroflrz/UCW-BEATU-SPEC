@file:OptIn(UnstableApi::class)

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
import androidx.media3.common.util.UnstableApi
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
            view.findViewById<android.widget.TextView>(R.id.tv_like_count)?.text = item.likeCount.toString()
            view.findViewById<android.widget.TextView>(R.id.tv_comment_count)?.text = item.commentCount.toString()
            view.findViewById<android.widget.TextView>(R.id.tv_favorite_count)?.text = item.favoriteCount.toString()
            view.findViewById<android.widget.TextView>(R.id.tv_share_count)?.text = item.shareCount.toString()
        }

        observeViewModel()

        playButton?.setOnClickListener { viewModel.togglePlayPause() }
        view.findViewById<View>(R.id.iv_like)?.setOnClickListener { /* TODO */ }
        view.findViewById<View>(R.id.iv_favorite)?.setOnClickListener { /* TODO */ }
        view.findViewById<View>(R.id.iv_comment)?.setOnClickListener { /* TODO */ }
        view.findViewById<View>(R.id.iv_share)?.setOnClickListener { /* TODO */ }
        view.findViewById<View>(R.id.iv_fullscreen)?.setOnClickListener { openLandscapeMode() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    playerView?.visibility = View.VISIBLE
                    playButton?.visibility = if (state.isPlaying) View.GONE else View.VISIBLE
                    state.error?.let { error -> Log.e(TAG, "播放错误: $error") }
                }
            }
        }
    }

    // ✅ 修复：onStart() 逻辑
    override fun onStart() {
        super.onStart()
        if (viewModel.uiState.value.currentVideoId != null) {
            // 横屏返回，恢复播放器
            reattachPlayer()
        } else {
            // 首次加载
            preparePlayerForFirstTime()
        }
    }

    override fun onResume() {
        super.onResume()
        navigatingToLandscape = false
        // ✅ 修复：不再自动播放，由 onStart() 或 onParentVisibilityChanged() 控制
    }

    override fun onPause() {
        super.onPause()
        if (!navigatingToLandscape && hasPreparedPlayer) {
            viewModel.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerView?.player = null
        viewModel.releaseCurrentPlayer()
        playerView = null
        playButton = null
        hasPreparedPlayer = false
    }

    fun onParentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            startPlaybackIfNeeded()
        } else if (hasPreparedPlayer) {
            viewModel.pause()
        }
    }

    fun onParentTabVisibilityChanged(isVisible: Boolean) {
        onParentVisibilityChanged(isVisible)
    }

    private fun startPlaybackIfNeeded(forcePrepare: Boolean = false) {
        if (!isAdded || playerView == null || videoItem == null) {
            Log.w(TAG, "Fragment not ready, skip startPlayback")
            return
        }

        if (!hasPreparedPlayer || forcePrepare) {
            preparePlayerForFirstTime()
        } else {
            viewModel.resume()
        }
    }

    // ✅ 修复：首次加载逻辑
    private fun preparePlayerForFirstTime() {
        val item = videoItem ?: return
        val pv = playerView ?: return
        Log.d(TAG, "Preparing video for playback: ${item.id}")
        viewModel.playVideo(item.id, item.videoUrl)
        viewModel.preparePlayer(item.id, item.videoUrl, pv)
        hasPreparedPlayer = true
    }

    // ✅ 修复：重绑定播放器逻辑
    private fun reattachPlayer() {
        if (!isAdded || playerView == null) return
        viewModel.onHostResume(playerView) // 确保调用 ViewModel 的恢复逻辑
        hasPreparedPlayer = true
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