package com.ucw.beatu.business.landscape.presentation.ui

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.landscape.presentation.R
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeVideoItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 单个横屏视频项 Fragment
 * 支持完整的手势控制：亮度、音量、进度、倍速、双击暂停等
 */
@AndroidEntryPoint
class LandscapeVideoItemFragment : Fragment() {

    companion object {
        private const val TAG = "LandscapeVideoItemFragment"
        private const val ARG_VIDEO_ITEM = "video_item"
        private const val CONTROL_PANEL_AUTO_HIDE_DELAY = 3000L // 3秒后自动隐藏
        private const val SEEK_SENSITIVITY = 0.01f // 进度调节灵敏度

        fun newInstance(videoItem: VideoItem): LandscapeVideoItemFragment {
            return LandscapeVideoItemFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VIDEO_ITEM, videoItem)
                }
            }
        }
    }

    private val viewModel: LandscapeVideoItemViewModel by viewModels()

    private var playerView: PlayerView? = null
    private var videoItem: VideoItem? = null

    // 控制面板相关
    private var controlPanel: View? = null
    private var controlPanelVisible = false
    private val hideControlPanelHandler = Handler(Looper.getMainLooper())
    private val hideControlPanelRunnable = Runnable { hideControlPanel() }

    // 手势相关
    private var gestureDetector: GestureDetector? = null
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isHorizontalSwipe = false
    private var isVerticalSwipe = false
    private var isBrightnessAdjusting = false
    private var isVolumeAdjusting = false
    private var isSeeking = false
    private var seekStartPosition = 0L

    // 亮度/音量相关
    private var brightnessButton: ImageButton? = null
    private var volumeButton: ImageButton? = null
    private var brightnessIndicator: LinearLayout? = null
    private var volumeIndicator: LinearLayout? = null
    private var brightnessProgress: View? = null
    private var volumeProgress: View? = null
    private var audioManager: AudioManager? = null
    private var windowManager: WindowManager? = null

    // 进度条相关
    private var seekIndicator: LinearLayout? = null
    private var seekTimeText: TextView? = null
    private var seekProgressBar: ProgressBar? = null

    // 交互按钮
    private var likeButton: ImageButton? = null
    private var favoriteButton: ImageButton? = null
    private var commentButton: ImageButton? = null
    private var shareButton: ImageButton? = null
    private var speedButton: TextView? = null
    private var qualityButton: TextView? = null
    private var lockButton: ImageButton? = null
    private var unlockButton: ImageButton? = null

    // 状态
    private var isLocked = false
    private var currentSpeed = 1.0f
    private var isLiked = false
    private var isFavorited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoItem = arguments?.let {
            BundleCompat.getParcelable(it, ARG_VIDEO_ITEM, VideoItem::class.java)
        }

        // 初始化系统服务
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_landscape_video_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        playerView = view.findViewById(R.id.player_view)
        controlPanel = view.findViewById(R.id.control_panel)

        // 初始化控制面板元素
        initControlPanel(view)

        // 初始化手势检测
        initGestureDetector()

        // 设置触摸监听
        setupTouchListener(view)

        // 设置按钮点击事件
        setupButtonClickListeners(view)

        // 观察 ViewModel 状态
        observeViewModel()

        // 延迟加载视频
        view.post {
            if (isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                loadVideo()
            }
        }
    }

    private fun initControlPanel(view: View) {
        brightnessButton = view.findViewById(R.id.btn_brightness)
        volumeButton = view.findViewById(R.id.btn_volume)
        brightnessIndicator = view.findViewById(R.id.brightness_indicator)
        volumeIndicator = view.findViewById(R.id.volume_indicator)
        brightnessProgress = view.findViewById(R.id.brightness_progress)
        volumeProgress = view.findViewById(R.id.volume_progress)

        seekIndicator = view.findViewById(R.id.seek_indicator)
        seekTimeText = view.findViewById(R.id.tv_seek_time)
        seekProgressBar = view.findViewById(R.id.progress_seek)

        likeButton = view.findViewById(R.id.btn_like)
        favoriteButton = view.findViewById(R.id.btn_favorite)
        commentButton = view.findViewById(R.id.btn_comment)
        shareButton = view.findViewById(R.id.btn_share)
        speedButton = view.findViewById(R.id.btn_speed)
        qualityButton = view.findViewById(R.id.btn_quality)
        lockButton = view.findViewById(R.id.btn_lock)
        unlockButton = view.findViewById(R.id.btn_unlock)

        // 更新视频信息
        videoItem?.let { item ->
            view.findViewById<TextView>(R.id.tv_like_count)?.text = item.likeCount.toString()
            view.findViewById<TextView>(R.id.tv_favorite_count)?.text = item.favoriteCount.toString()
            view.findViewById<TextView>(R.id.tv_comment_count)?.text = item.commentCount.toString()
        }
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isLocked) {
                    toggleControlPanel()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isLocked) {
                    viewModel.togglePlayPause()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!isLocked) {
                    // 屏幕中央长按进入2倍速
                    val viewWidth = rootView?.width ?: 0
                    val viewHeight = rootView?.height ?: 0
                    val centerX = viewWidth / 2f
                    val centerY = viewHeight / 2f
                    val tolerance = 100f

                    if (abs(e.x - centerX) < tolerance && abs(e.y - centerY) < tolerance) {
                        viewModel.setSpeed(2.0f)
                        currentSpeed = 2.0f
                        speedButton?.text = "2.0x"
                    }
                }
            }
        })
    }

    private var rootView: View? = null
    
    private fun setupTouchListener(view: View) {
        rootView = view
        view.setOnTouchListener { v, event ->
            if (isLocked) {
                // 锁屏时只响应解锁按钮
                return@setOnTouchListener false
            }

            gestureDetector?.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    isSeeking = false

                    // 检查是否按下亮度/音量按钮
                    brightnessButton?.let { btn ->
                        val location = IntArray(2)
                        btn.getLocationOnScreen(location)
                        val btnX = event.rawX
                        val btnY = event.rawY
                        if (btnX >= location[0] && btnX <= location[0] + btn.width &&
                            btnY >= location[1] && btnY <= location[1] + btn.height) {
                            isBrightnessAdjusting = true
                            showBrightnessIndicator()
                            return@setOnTouchListener true
                        }
                    }

                    volumeButton?.let { btn ->
                        val location = IntArray(2)
                        btn.getLocationOnScreen(location)
                        val btnX = event.rawX
                        val btnY = event.rawY
                        if (btnX >= location[0] && btnX <= location[0] + btn.width &&
                            btnY >= location[1] && btnY <= location[1] + btn.height) {
                            isVolumeAdjusting = true
                            showVolumeIndicator()
                            return@setOnTouchListener true
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = abs(event.x - touchStartX)
                    val deltaY = abs(event.y - touchStartY)
                    val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

                    // 亮度调节
                    if (isBrightnessAdjusting) {
                        adjustBrightness(event.y - touchStartY)
                        return@setOnTouchListener true
                    }

                    // 音量调节
                    if (isVolumeAdjusting) {
                        adjustVolume(event.y - touchStartY)
                        return@setOnTouchListener true
                    }

                    // 判断滑动方向
                    if (!isHorizontalSwipe && !isVerticalSwipe) {
                        if (deltaX > touchSlop && deltaX > deltaY * 2) {
                            isHorizontalSwipe = true
                            isSeeking = true
                            seekStartPosition = viewModel.getCurrentPosition()
                            showSeekIndicator()
                        } else if (deltaY > touchSlop && deltaY > deltaX * 2) {
                            isVerticalSwipe = true
                        }
                    }

                    // 进度调节
                    if (isSeeking && isHorizontalSwipe) {
                        adjustSeek(event.x - touchStartX)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isBrightnessAdjusting) {
                        isBrightnessAdjusting = false
                        hideBrightnessIndicator()
                    }

                    if (isVolumeAdjusting) {
                        isVolumeAdjusting = false
                        hideVolumeIndicator()
                    }

                    if (isSeeking) {
                        // 执行实际的 seek 操作
                        val deltaX = event.x - touchStartX
                        val viewWidth = rootView?.width ?: 1
                        val duration = viewModel.getDuration()
                        if (duration > 0) {
                            val seekChange = (deltaX / viewWidth * duration).toLong()
                            val newPosition = (seekStartPosition + seekChange).coerceIn(0L, duration)
                            viewModel.seekTo(newPosition)
                        }
                        isSeeking = false
                        hideSeekIndicator()
                    }

                    // 长按2倍速后恢复
                    if (currentSpeed == 2.0f && event.action == MotionEvent.ACTION_UP) {
                        viewModel.setSpeed(1.0f)
                        currentSpeed = 1.0f
                        speedButton?.text = "1.0x"
                    }

                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                }
            }

            true
        }
    }

    private fun setupButtonClickListeners(view: View) {
        // 退出全屏
        view.findViewById<ImageButton>(R.id.btn_exit_fullscreen)?.setOnClickListener {
            requireActivity().finish()
        }

        // 点赞
        likeButton?.setOnClickListener {
            toggleLike()
        }

        // 收藏
        favoriteButton?.setOnClickListener {
            toggleFavorite()
        }

        // 评论
        commentButton?.setOnClickListener {
            // TODO: 打开评论浮层
            Log.d(TAG, "评论按钮点击")
        }

        // 分享
        shareButton?.setOnClickListener {
            // TODO: 打开分享浮层
            Log.d(TAG, "分享按钮点击")
        }

        // 倍速
        speedButton?.setOnClickListener {
            showSpeedMenu()
        }

        // 清晰度
        qualityButton?.setOnClickListener {
            showQualityMenu()
        }

        // 锁屏
        lockButton?.setOnClickListener {
            lockScreen()
        }

        // 解锁
        unlockButton?.setOnClickListener {
            unlockScreen()
        }
    }

    // ========== 控制面板显示/隐藏 ==========

    private fun toggleControlPanel() {
        if (controlPanelVisible) {
            hideControlPanel()
        } else {
            showControlPanel()
        }
    }

    private fun showControlPanel() {
        controlPanel?.visibility = View.VISIBLE
        controlPanelVisible = true
        scheduleHideControlPanel()
    }

    private fun hideControlPanel() {
        controlPanel?.visibility = View.GONE
        controlPanelVisible = false
    }

    private fun scheduleHideControlPanel() {
        hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
        hideControlPanelHandler.postDelayed(hideControlPanelRunnable, CONTROL_PANEL_AUTO_HIDE_DELAY)
    }

    // ========== 亮度调节 ==========

    private fun showBrightnessIndicator() {
        brightnessIndicator?.visibility = View.VISIBLE
        updateBrightnessIndicator()
    }

    private fun hideBrightnessIndicator() {
        brightnessIndicator?.visibility = View.GONE
    }

    private fun adjustBrightness(deltaY: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes

        // 计算亮度变化（上滑增加，下滑减少）
        val brightnessChange = -deltaY / (rootView?.height ?: 1) * 0.5f
        val currentBrightness = layoutParams.screenBrightness
        val newBrightness = (currentBrightness + brightnessChange).coerceIn(0.1f, 1.0f)
        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams

        updateBrightnessIndicator()
    }

    private fun updateBrightnessIndicator() {
        val window = requireActivity().window
        val brightness = window.attributes.screenBrightness
        val progress = ((brightness - 0.1f) / 0.9f * 100).toInt().coerceIn(0, 100)

        brightnessProgress?.layoutParams?.height = (brightnessIndicator?.height ?: 0) * progress / 100
        brightnessProgress?.requestLayout()
    }

    // ========== 音量调节 ==========

    private fun showVolumeIndicator() {
        volumeIndicator?.visibility = View.VISIBLE
        updateVolumeIndicator()
    }

    private fun hideVolumeIndicator() {
        volumeIndicator?.visibility = View.GONE
    }

    private fun adjustVolume(deltaY: Float) {
        audioManager?.let { am ->
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)

            // 计算音量变化（上滑增加，下滑减少）
            val volumeChange = (-deltaY / (rootView?.height ?: 1) * maxVolume).toInt()
            val newVolume = (currentVolume + volumeChange).coerceIn(0, maxVolume)

            am.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            updateVolumeIndicator()
        }
    }

    private fun updateVolumeIndicator() {
        audioManager?.let { am ->
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val progress = (currentVolume.toFloat() / maxVolume * 100).toInt()

            volumeProgress?.layoutParams?.height = (volumeIndicator?.height ?: 0) * progress / 100
            volumeProgress?.requestLayout()
        }
    }

    // ========== 进度调节 ==========

    private fun showSeekIndicator() {
        seekIndicator?.visibility = View.VISIBLE
        scheduleHideControlPanel()
    }

    private fun hideSeekIndicator() {
        seekIndicator?.visibility = View.GONE
    }

    private fun adjustSeek(deltaX: Float) {
        val viewWidth = rootView?.width ?: 1
        val duration = viewModel.getDuration()
        
        if (duration > 0) {
            val seekChange = (deltaX / viewWidth * duration).toLong()
            val newPosition = (seekStartPosition + seekChange).coerceIn(0L, duration)
            
            // 更新显示
            seekTimeText?.text = formatTime(newPosition) + " / " + formatTime(duration)
            seekProgressBar?.progress = ((newPosition.toFloat() / duration * 100).toInt()).coerceIn(0, 100)
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // ========== 交互功能 ==========

    private fun toggleLike() {
        isLiked = !isLiked
        likeButton?.setImageResource(
            if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        )
        likeButton?.setColorFilter(
            if (isLiked) 0xFFFF0000.toInt() else 0xFFFFFFFF.toInt()
        )
        
        // 更新计数
        videoItem?.let { item ->
            val newCount = if (isLiked) item.likeCount + 1 else item.likeCount - 1
            view?.findViewById<TextView>(R.id.tv_like_count)?.text = newCount.coerceAtLeast(0).toString()
        }
        
        // TODO: 调用 API
        Log.d(TAG, "点赞状态: $isLiked")
    }

    private fun toggleFavorite() {
        isFavorited = !isFavorited
        favoriteButton?.setImageResource(
            if (isFavorited) android.R.drawable.star_big_on else android.R.drawable.star_big_off
        )
        favoriteButton?.setColorFilter(
            if (isFavorited) 0xFFFFD700.toInt() else 0xFFFFFFFF.toInt()
        )
        
        // 更新计数
        videoItem?.let { item ->
            val newCount = if (isFavorited) item.favoriteCount + 1 else item.favoriteCount - 1
            view?.findViewById<TextView>(R.id.tv_favorite_count)?.text = newCount.coerceAtLeast(0).toString()
        }
        
        // TODO: 调用 API
        Log.d(TAG, "收藏状态: $isFavorited")
    }

    private fun showSpeedMenu() {
        // 循环切换倍速：1.0x -> 1.25x -> 1.5x -> 2.0x -> 1.0x
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOf(currentSpeed)
        val nextIndex = (currentIndex + 1) % speeds.size
        val nextSpeed = speeds[nextIndex]

        viewModel.setSpeed(nextSpeed)
        currentSpeed = nextSpeed
        speedButton?.text = "${nextSpeed}x"
        
        Log.d(TAG, "切换倍速: ${nextSpeed}x")
    }

    private fun showQualityMenu() {
        // TODO: 显示清晰度选择菜单（自动 / 高清 / 标清）
        // 目前简单循环切换
        val qualities = listOf("自动", "高清", "标清")
        val currentQuality = qualityButton?.text?.toString() ?: "高清"
        val currentIndex = qualities.indexOf(currentQuality)
        val nextIndex = (currentIndex + 1) % qualities.size
        qualityButton?.text = qualities[nextIndex]
        
        Log.d(TAG, "切换清晰度: ${qualities[nextIndex]}")
    }

    private fun lockScreen() {
        isLocked = true
        hideControlPanel()
        lockButton?.visibility = View.GONE
        unlockButton?.visibility = View.VISIBLE
        Log.d(TAG, "锁定屏幕")
    }

    private fun unlockScreen() {
        isLocked = false
        unlockButton?.visibility = View.GONE
        lockButton?.visibility = View.VISIBLE
        showControlPanel()
        Log.d(TAG, "解锁屏幕")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    playerView?.visibility = View.VISIBLE

                    // 更新播放状态
                    if (state.isPlaying && !controlPanelVisible) {
                        // 播放时自动隐藏控制面板
                        hideControlPanel()
                    }

                    // 更新进度显示（如果控制面板可见）
                    if (controlPanelVisible && state.durationMs > 0) {
                        seekTimeText?.text = formatTime(state.currentPositionMs) + " / " + formatTime(state.durationMs)
                        seekProgressBar?.progress = ((state.currentPositionMs.toFloat() / state.durationMs * 100).toInt()).coerceIn(0, 100)
                    }

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
            Log.d(TAG, "Loading landscape video: ${item.id} - ${item.videoUrl}")

            playerView?.let { pv ->
                viewModel.playVideo(item.id, item.videoUrl)
                viewModel.preparePlayer(item.id, item.videoUrl, pv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading video", e)
        }
    }

    override fun onPause() {
        super.onPause()
        hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
        viewModel.pause()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && viewModel.uiState.value.currentVideoId != null) {
            viewModel.resume()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
        viewModel.releaseCurrentPlayer()
        playerView = null
        rootView = null
    }
}