package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 推荐页面Fragment
 * 显示推荐视频内容，集成视频播放器
 */
@AndroidEntryPoint
class RecommendFragment : Fragment() {

    companion object {
        private const val TAG = "RecommendFragment"
    }

    private val viewModel: RecommendViewModel by viewModels()
    
    private var playerView: PlayerView? = null
    private var playButton: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Fragment created")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Creating view")
        try {
            val view = inflater.inflate(R.layout.fragment_recommend, container, false)
            Log.d(TAG, "onCreateView: View created successfully")
            return view
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView: Error creating view", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: View created")
        
        try {
            // 初始化视图
            playerView = view.findViewById(R.id.player_view)
            playButton = view.findViewById(R.id.iv_play_button)
            Log.d(TAG, "onViewCreated: Views initialized - playerView=${playerView != null}, playButton=${playButton != null}")
            
            // 观察 ViewModel 状态
            observeViewModel()
            Log.d(TAG, "onViewCreated: ViewModel observer set up")
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated: Error setting up views", e)
            throw e
        }
        
        // 设置视频播放区域点击事件
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
            // TODO: 全屏播放
        }
        
        // 延迟加载视频，确保视图完全初始化
        view.post {
            if (isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                loadTestVideo()
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 更新播放器显示状态（全屏显示，不再隐藏）
                    playerView?.visibility = View.VISIBLE
                    
                    // 更新播放按钮显示状态（播放时隐藏，暂停时显示）
                    playButton?.visibility = if (state.isPlaying) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                    
                    // 处理错误
                    state.error?.let { error ->
                        // TODO: 显示错误提示
                        android.util.Log.e("RecommendFragment", "播放错误: $error")
                    }
                }
            }
        }
    }
    
    /**
     * 加载测试视频（临时方法，后续会从数据源获取）
     */
    private fun loadTestVideo() {
        // 检查 Fragment 是否还活着
        if (!isAdded || view == null || playerView == null) {
            Log.w(TAG, "Fragment not ready, skip loading video")
            return
        }
        
        try {
            // 使用测试视频 URL（Google 提供的公开测试视频）
            val testVideoId = "test_video_001"
            val testVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            
            Log.d(TAG, "Loading test video: $testVideoUrl")
            
            playerView?.let { pv ->
                Log.d(TAG, "PlayerView found, preparing video")
                viewModel.playVideo(testVideoId, testVideoUrl)
                viewModel.preparePlayer(testVideoId, testVideoUrl, pv)
                Log.d(TAG, "Video preparation started")
            } ?: run {
                Log.e(TAG, "PlayerView is null, cannot load video")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading test video", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }
    
    override fun onResume() {
        super.onResume()
        // 如果视频已加载，恢复播放
        try {
            if (isAdded && viewModel.uiState.value.currentVideoId != null) {
                viewModel.resume()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecommendFragment", "Error in onResume", e)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.releaseCurrentPlayer()
        playerView = null
        playButton = null
    }
}

