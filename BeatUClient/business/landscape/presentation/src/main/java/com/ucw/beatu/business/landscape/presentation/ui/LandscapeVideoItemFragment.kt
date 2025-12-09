package com.ucw.beatu.business.landscape.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
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
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import android.provider.Settings
import com.ucw.beatu.business.landscape.presentation.R
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeControlsState
import com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeVideoItemViewModel
import com.ucw.beatu.business.videofeed.presentation.share.ShareImageUtils
import com.ucw.beatu.business.videofeed.presentation.share.SharePosterGenerator
import com.ucw.beatu.business.videofeed.presentation.ui.VideoCommentsDialogFragment
import com.ucw.beatu.business.videofeed.presentation.ui.VideoShareDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import com.ucw.beatu.shared.designsystem.R as DesignSystemR
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        private const val BRIGHTNESS_SENSITIVITY = 1.2f // 亮度调节灵敏度（值越大越灵敏）
        private const val VOLUME_SENSITIVITY = 1.2f // 音量调节灵敏度（值越大越灵敏）
        private val LIKE_ACTIVE_COLOR = 0xFFFF4D4F.toInt()
        private val FAVORITE_ACTIVE_COLOR = 0xFFFFD700.toInt()
        private val ICON_INACTIVE_COLOR = 0xFFFFFFFF.toInt()
        private const val MIN_SCREEN_BRIGHTNESS = 0.08f
        private const val MAX_SCREEN_BRIGHTNESS = 1.0f

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
    private var isCurrentlyVisibleToUser = false
    private var latestControlsState = LandscapeControlsState()
    private var speedBeforeBoost: Float? = null

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
    private var brightnessIndicator: FrameLayout? = null
    private var volumeIndicator: FrameLayout? = null
    private var brightnessProgress: View? = null
    private var volumeProgress: View? = null
    private var brightnessPercentText: TextView? = null
    private var volumePercentText: TextView? = null
    private var audioManager: AudioManager? = null
    private var windowManager: WindowManager? = null
    private val brightnessLongPressHandler = Handler(Looper.getMainLooper())
    private val brightnessLongPressRunnable = Runnable { startBrightnessAdjustment() }
    // 亮度条隐藏延迟处理：手指松开后等待一段时间再恢复按钮
    private val brightnessHideHandler = Handler(Looper.getMainLooper())
    private val brightnessHideRunnable = Runnable {
        hideBrightnessIndicator()
        disallowParentIntercept(false)
        setParentPagingEnabled(true)
    }
    private var isBrightnessButtonPressed = false
    private var lastBrightnessDragY = 0f

    // 音量长按与隐藏控制
    private val volumeLongPressHandler = Handler(Looper.getMainLooper())
    private val volumeLongPressRunnable = Runnable { startVolumeAdjustment() }
    private val volumeHideHandler = Handler(Looper.getMainLooper())
    private val volumeHideRunnable = Runnable {
        hideVolumeIndicator()
        disallowParentIntercept(false)
        setParentPagingEnabled(true)
    }
    private var isVolumeButtonPressed = false
    private var lastVolumeDragY = 0f
    private var pendingVolumeDelta = 0f

    // 进度条相关
    private var seekIndicator: LinearLayout? = null
    private var seekTimeText: TextView? = null
    private var seekProgressBar: ProgressBar? = null

    // 交互按钮
    private var rotateButton: ImageButton? = null
    private var likeButton: ImageButton? = null
    private var favoriteButton: ImageButton? = null
    private var commentButton: ImageButton? = null
    private var shareButton: ImageButton? = null
    private var speedButton: TextView? = null
    private var qualityButton: TextView? = null
    private var lockButton: ImageButton? = null
    private var unlockButton: ImageButton? = null
    private var videoProgressBar: ProgressBar? = null

    // 状态

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoItem = arguments?.let {
            BundleCompat.getParcelable<VideoItem>(it, ARG_VIDEO_ITEM, VideoItem::class.java)
        }

        videoItem?.let { viewModel.bindVideoMeta(it) }

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
        observeControls()

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
        brightnessPercentText = view.findViewById(R.id.tv_brightness_percent)
        volumeProgress = view.findViewById(R.id.volume_progress)
        volumePercentText = view.findViewById(R.id.tv_volume_percent)

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
        videoProgressBar = view.findViewById(R.id.progress_video)

        // 更新视频信息
        videoItem?.let { item ->
            view.findViewById<TextView>(R.id.tv_like_count)?.text = item.likeCount.toString()
            view.findViewById<TextView>(R.id.tv_favorite_count)?.text = item.favoriteCount.toString()
        }
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // 亮度调节进行中时，屏蔽点击切换控制面板
                if (isBrightnessAdjusting || latestControlsState.isLocked) {
                    return true
                }
                toggleControlPanel()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                // 亮度调节进行中时，屏蔽双击播放/暂停
                if (isBrightnessAdjusting || latestControlsState.isLocked) {
                    return true
                }
                viewModel.togglePlayPause()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                // 亮度调节进行中或锁屏时，不触发中心长按倍速
                if (isBrightnessAdjusting || latestControlsState.isLocked) {
                    return
                }

                    // 屏幕中央长按进入2倍速
                    val viewWidth = rootView?.width ?: 0
                    val viewHeight = rootView?.height ?: 0
                    val centerX = viewWidth / 2f
                    val centerY = viewHeight / 2f
                    val tolerance = 100f

                    if (abs(e.x - centerX) < tolerance && abs(e.y - centerY) < tolerance) {
                        speedBeforeBoost = latestControlsState.currentSpeed
                        viewModel.setSpeed(2.0f)
                        speedButton?.text = formatSpeedLabel(2.0f)
                }
            }
        })
    }

    private var rootView: View? = null
    
    private fun setupTouchListener(view: View) {
        rootView = view
        view.setOnTouchListener { v, event ->
            if (latestControlsState.isLocked) {
                // 锁屏时只响应解锁按钮
                return@setOnTouchListener false
            }

            // 亮度 / 音量调节中：全局只处理对应手势，其它交互全部屏蔽
            if (isBrightnessAdjusting || isVolumeAdjusting) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        if (isBrightnessAdjusting) {
                            val delta = event.rawY - lastBrightnessDragY
                            lastBrightnessDragY = event.rawY
                            adjustBrightness(delta)
                        } else if (isVolumeAdjusting) {
                            val delta = event.rawY - lastVolumeDragY
                            lastVolumeDragY = event.rawY
                            adjustVolume(delta)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 手指离开屏幕后，结束当前调节，1秒后恢复按钮
                        if (isBrightnessAdjusting) {
                            cancelBrightnessTouch()
                        }
                        if (isVolumeAdjusting) {
                            cancelVolumeTouch()
                        }
                    }
                }
                return@setOnTouchListener true
            }

            gestureDetector?.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    isSeeking = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = abs(event.x - touchStartX)
                    val deltaY = abs(event.y - touchStartY)
                    val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

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

                    if (event.action == MotionEvent.ACTION_UP) {
                        speedBeforeBoost?.let { previous ->
                            viewModel.setSpeed(previous)
                            speedBeforeBoost = null
                        }
                    }

                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                }
            }

            true
        }
    }

    private fun setupButtonClickListeners(view: View) {
        // 旋转/退出全屏
        rotateButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            exitLandscape()
        }

        // 点赞
        likeButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            toggleLike()
        }

        // 收藏
        favoriteButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            toggleFavorite()
        }

        // 评论
        commentButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            Log.d(TAG, "评论按钮点击")
            val item = videoItem ?: return@setOnClickListener
            // 直接复用通用的 VideoCommentsDialogFragment：
            // 它在 onStart 中会自动根据横竖屏调整为右侧半屏/底部半屏
            VideoCommentsDialogFragment
                .newInstance(item.id, item.commentCount)
                .show(parentFragmentManager, "video_comments_dialog_landscape")
        }

        // 分享
        shareButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            val item = videoItem ?: return@setOnClickListener
            Log.d(TAG, "分享按钮点击, videoId=${item.id}")

            val dialog = VideoShareDialogFragment.newInstance(
                videoId = item.id,
                title = item.title,
                playUrl = item.videoUrl
            )
            dialog.shareActionListener = object : VideoShareDialogFragment.ShareActionListener {
                override fun onSharePoster() {
                    viewModel.reportShare()
                    Log.d(TAG, "横屏分享：生成分享图")

                    val pv = playerView
                    if (pv == null) {
                        Log.e(TAG, "PlayerView is null, cannot capture cover for landscape share poster")
                        return
                    }

                    val context = requireContext()
                    val shareUrl = item.videoUrl
                    val posterBitmap: Bitmap = SharePosterGenerator.generate(
                        context = context,
                        coverView = pv,
                        title = item.title,
                        author = item.authorName,
                        shareUrl = shareUrl
                    )

                    ShareImageUtils.shareBitmap(
                        context = context,
                        bitmap = posterBitmap,
                        fileName = "beatu_share_land_${item.id}.jpg",
                        chooserTitle = "分享图片"
                    )
                }

                override fun onShareLink() {
                    Log.d(TAG, "横屏分享：链接分享")
                    viewModel.reportShare()

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
            dialog.show(parentFragmentManager, "video_share_options_landscape")
        }

        // 倍速
        speedButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            showSpeedMenu()
        }

        // 清晰度
        qualityButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            showQualityMenu()
        }

        // 锁屏
        lockButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            lockScreen()
        }

        // 解锁
        unlockButton?.setOnClickListener {
            if (isBrightnessAdjusting) return@setOnClickListener
            unlockScreen()
            // 解锁后，短暂展示控制面板 1 秒，便于用户确认状态
            hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
            showControlPanel()
            hideControlPanelHandler.postDelayed(hideControlPanelRunnable, 1000L)
        }

        brightnessButton?.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.rawY
                    lastBrightnessDragY = event.rawY
                    isBrightnessButtonPressed = true
                    scheduleBrightnessLongPress()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isBrightnessButtonPressed) return@setOnTouchListener false
                    // 亮度调节开始后，允许手指离开按钮区域在附近滑动
                    if (!isBrightnessAdjusting && !isTouchInsideView(v, event.rawX, event.rawY)) {
                        // 在长按触发前移出按钮区域则取消本次亮度调节
                        cancelBrightnessTouch()
                        return@setOnTouchListener true
                    }
                    if (isBrightnessAdjusting) {
                        val delta = event.rawY - lastBrightnessDragY
                        lastBrightnessDragY = event.rawY
                        adjustBrightness(delta)
                        return@setOnTouchListener true
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 手指从亮度按钮抬起（仍在按钮区域内）
                    cancelBrightnessTouch()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    // 事件被系统取消：停止长按检测，但不立即隐藏亮度条
                    brightnessLongPressHandler.removeCallbacks(brightnessLongPressRunnable)
                    isBrightnessButtonPressed = false
                    true
                }
                else -> false
            }
        }

        volumeButton?.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.rawY
                    lastVolumeDragY = event.rawY
                    isVolumeButtonPressed = true
                    scheduleVolumeLongPress()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isVolumeButtonPressed) return@setOnTouchListener false
                    // 音量调节开始前，如果手指移出按钮区域则取消本次调节
                    if (!isVolumeAdjusting && !isTouchInsideView(v, event.rawX, event.rawY)) {
                        cancelVolumeTouch()
                        return@setOnTouchListener true
                    }
                    if (isVolumeAdjusting) {
                        val delta = event.rawY - lastVolumeDragY
                        lastVolumeDragY = event.rawY
                        adjustVolume(delta)
                        return@setOnTouchListener true
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 手指从音量按钮抬起（仍在按钮区域内）
                    cancelVolumeTouch()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    // 事件被系统取消：停止长按检测，但不立即隐藏音量条
                    volumeLongPressHandler.removeCallbacks(volumeLongPressRunnable)
                    isVolumeButtonPressed = false
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 退出横屏模式
     */
    private fun exitLandscape() {
        prepareForExit()
        requireActivity().finish()
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
        if (latestControlsState.isLocked) return
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
        // 亮度调节中不自动隐藏控制面板，否则会把亮度条一起隐藏掉
        if (isBrightnessAdjusting) return
        hideControlPanelHandler.postDelayed(hideControlPanelRunnable, CONTROL_PANEL_AUTO_HIDE_DELAY)
    }

    // ========== 亮度调节 ==========

    private fun showBrightnessIndicator() {
        // 隐藏按钮，显示亮度条
        brightnessButton?.visibility = View.GONE
        brightnessIndicator?.visibility = View.VISIBLE
        brightnessIndicator?.post { updateBrightnessIndicator() }
        // 进入亮度调节时，确保控制面板保持可见，取消自动隐藏
        hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
        controlPanel?.visibility = View.VISIBLE
        controlPanelVisible = true
    }

    private fun hideBrightnessIndicator() {
        // 显示按钮，隐藏亮度条
        brightnessButton?.visibility = View.VISIBLE
        brightnessIndicator?.visibility = View.GONE
        brightnessPercentText?.text = ""
        brightnessPercentText?.visibility = View.GONE
    }

    private fun adjustBrightness(deltaY: Float) {
        val window = requireActivity().window
        val layoutParams = window.attributes

        // 根据手指在屏幕上的相对滑动距离调节亮度
        // 公式：滑动距离 / 屏幕高度 * 灵敏度
        // BRIGHTNESS_SENSITIVITY 越大，调节越灵敏
        val viewHeight = (rootView?.height ?: 1).toFloat()
        val brightnessChange = (-deltaY / viewHeight) * BRIGHTNESS_SENSITIVITY
        val currentBrightness = getCurrentBrightness()
        val newBrightness = (currentBrightness + brightnessChange).coerceIn(MIN_SCREEN_BRIGHTNESS, MAX_SCREEN_BRIGHTNESS)
        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams

        updateBrightnessIndicator()
    }

    private fun updateBrightnessIndicator() {
        val brightness = getCurrentBrightness()
        val progress = (((brightness - MIN_SCREEN_BRIGHTNESS) / (MAX_SCREEN_BRIGHTNESS - MIN_SCREEN_BRIGHTNESS)) * 100).toInt().coerceIn(0, 100)

        val indicatorHeight = (brightnessIndicator?.height ?: 0) - (brightnessIndicator?.paddingTop ?: 0) - (brightnessIndicator?.paddingBottom ?: 0)
        if (indicatorHeight > 0) {
            val fillHeight = indicatorHeight * progress / 100
            val params = (brightnessProgress?.layoutParams as? FrameLayout.LayoutParams)
            params?.height = fillHeight
            params?.gravity = Gravity.BOTTOM
            brightnessProgress?.layoutParams = params
        }
        
        // 更新百分比文字（仅在调节时显示）
        brightnessPercentText?.text = "$progress%"
        brightnessPercentText?.visibility = View.VISIBLE
        
        brightnessProgress?.requestLayout()
    }

    private fun getCurrentBrightness(): Float {
        val windowBrightness = requireActivity().window.attributes.screenBrightness
        if (windowBrightness in 0f..1f) return windowBrightness
        return runCatching {
            Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrDefault(0.5f).coerceIn(MIN_SCREEN_BRIGHTNESS, MAX_SCREEN_BRIGHTNESS)
    }

    private fun scheduleBrightnessLongPress() {
        brightnessLongPressHandler.removeCallbacks(brightnessLongPressRunnable)
        brightnessLongPressHandler.postDelayed(
            brightnessLongPressRunnable,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun scheduleVolumeLongPress() {
        volumeLongPressHandler.removeCallbacks(volumeLongPressRunnable)
        volumeLongPressHandler.postDelayed(
            volumeLongPressRunnable,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun startBrightnessAdjustment() {
        if (!isBrightnessButtonPressed || isBrightnessAdjusting) return
        isBrightnessAdjusting = true
        lastBrightnessDragY = touchStartY
        disallowParentIntercept(true)
        setParentPagingEnabled(false)
        showBrightnessIndicator()
    }

    private fun cancelBrightnessTouch() {
        brightnessLongPressHandler.removeCallbacks(brightnessLongPressRunnable)
        isBrightnessButtonPressed = false
        if (isBrightnessAdjusting) {
            // 已经在亮度调节模式：手指松开后不立即隐藏亮度条，
            // 先结束“调节中”状态，恢复其它交互，再延迟一秒关闭亮度条并恢复按钮
            isBrightnessAdjusting = false
            brightnessHideHandler.removeCallbacks(brightnessHideRunnable)
            brightnessHideHandler.postDelayed(brightnessHideRunnable, 1000L)
        }
    }

    private fun startVolumeAdjustment() {
        if (!isVolumeButtonPressed || isVolumeAdjusting) return
        isVolumeAdjusting = true
        lastVolumeDragY = touchStartY
        pendingVolumeDelta = 0f
        disallowParentIntercept(true)
        setParentPagingEnabled(false)
        showVolumeIndicator()
    }

    private fun cancelVolumeTouch() {
        volumeLongPressHandler.removeCallbacks(volumeLongPressRunnable)
        isVolumeButtonPressed = false
        if (isVolumeAdjusting) {
            // 已经在音量调节模式：手指松开后不立即隐藏音量条，
            // 先结束“调节中”状态，恢复其它交互，再延迟一秒关闭音量条并恢复按钮
            isVolumeAdjusting = false
            pendingVolumeDelta = 0f
            volumeHideHandler.removeCallbacks(volumeHideRunnable)
            volumeHideHandler.postDelayed(volumeHideRunnable, 1000L)
        }
    }

    private fun isTouchInsideView(view: View, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        return rawX >= left && rawX <= right && rawY >= top && rawY <= bottom
    }

    private fun disallowParentIntercept(disallow: Boolean) {
        rootView?.parent?.requestDisallowInterceptTouchEvent(disallow)
    }

    private fun setParentPagingEnabled(enabled: Boolean) {
        (parentFragment as? LandscapeFragment)?.setPagingEnabled(enabled)
    }

    // ========== 音量调节 ==========

    private fun showVolumeIndicator() {
        // 隐藏按钮，显示音量条
        volumeButton?.visibility = View.GONE
        volumeIndicator?.visibility = View.VISIBLE
        updateVolumeIndicator()
        // 进入音量调节时，确保控制面板保持可见，取消自动隐藏
        hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
        controlPanel?.visibility = View.VISIBLE
        controlPanelVisible = true
    }

    private fun hideVolumeIndicator() {
        // 显示按钮，隐藏音量条
        volumeButton?.visibility = View.VISIBLE
        volumeIndicator?.visibility = View.GONE
        volumePercentText?.text = ""
        volumePercentText?.visibility = View.GONE
    }

    private fun adjustVolume(deltaY: Float) {
        if (!hasModifyAudioSettingsPermission()) {
            Log.w(TAG, "adjustVolume: missing MODIFY_AUDIO_SETTINGS permission, skip")
            return
        }

        audioManager?.let { am ->
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            // 将手指滑动距离转换为“音量步进”的累计值，避免因为取整导致的小步长失效
            val viewHeight = (rootView?.height ?: 1).toFloat()
            val volumeDeltaSteps = (-deltaY / viewHeight) * VOLUME_SENSITIVITY * maxVolume
            pendingVolumeDelta += volumeDeltaSteps

            val steps = pendingVolumeDelta.toInt()
            if (steps == 0) {
                return
            }
            pendingVolumeDelta -= steps

            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val newVolume = (currentVolume + steps).coerceIn(0, maxVolume)

            try {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                updateVolumeIndicator()
            } catch (se: SecurityException) {
                Log.e(TAG, "adjustVolume: failed to modify volume", se)
            }
        }
    }

    private fun updateVolumeIndicator() {
        if (!hasModifyAudioSettingsPermission()) {
            Log.w(TAG, "updateVolumeIndicator: missing MODIFY_AUDIO_SETTINGS permission, skip")
            return
        }

        audioManager?.let { am ->
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val progress = (currentVolume.toFloat() / maxVolume * 100).toInt()

            val indicatorHeight = (volumeIndicator?.height ?: 0) - (volumeIndicator?.paddingTop ?: 0) - (volumeIndicator?.paddingBottom ?: 0)
            if (indicatorHeight > 0) {
                val fillHeight = indicatorHeight * progress / 100
                val params = (volumeProgress?.layoutParams as? FrameLayout.LayoutParams)
                params?.height = fillHeight
                params?.gravity = Gravity.BOTTOM
                volumeProgress?.layoutParams = params
            }

            // 更新百分比文字（仅在调节时显示）
            volumePercentText?.text = "$progress%"
            volumePercentText?.visibility = View.VISIBLE

            volumeProgress?.requestLayout()
        }
    }

    private fun hasModifyAudioSettingsPermission(): Boolean {
        val context = context ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
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

    private fun formatSpeedLabel(speed: Float): String =
        if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

    // ========== 交互功能 ==========

    private fun toggleLike() {
        val nextState = !latestControlsState.isLiked
        viewModel.toggleLike()
        Log.d(TAG, "点赞状态: $nextState")
    }

    private fun toggleFavorite() {
        val nextState = !latestControlsState.isFavorited
        viewModel.toggleFavorite()
        Log.d(TAG, "收藏状态: $nextState")
    }

    private fun showSpeedMenu() {
        viewModel.cycleSpeed()
        Log.d(TAG, "切换倍速")
    }

    private fun showQualityMenu() {
        viewModel.cycleQuality()
        Log.d(TAG, "切换清晰度")
    }

    private fun lockScreen() {
        hideControlPanel()
        lockButton?.visibility = View.GONE
        unlockButton?.visibility = View.VISIBLE
        viewModel.lockControls()
        Log.d(TAG, "锁定屏幕")
    }

    private fun unlockScreen() {
        unlockButton?.visibility = View.GONE
        lockButton?.visibility = View.VISIBLE
        showControlPanel()
        viewModel.unlockControls()
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

                    // 更新底部进度条
                    if (state.durationMs > 0) {
                        val progress = ((state.currentPositionMs.toFloat() / state.durationMs * 100).toInt()).coerceIn(0, 100)
                        videoProgressBar?.progress = progress
                    }

                    // 更新进度显示（如果控制面板可见且正在Seek）
                    if (controlPanelVisible && isSeeking && state.durationMs > 0) {
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

    private fun observeControls() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.controlsState.collect { state ->
                    latestControlsState = state
                    renderControlState(state)
                }
            }
        }
    }

    private fun renderControlState(state: LandscapeControlsState) {
        likeButton?.apply {
            setImageResource(DesignSystemR.drawable.ic_like)
            imageTintList = ColorStateList.valueOf(
                if (state.isLiked) LIKE_ACTIVE_COLOR else ICON_INACTIVE_COLOR
            )
        }
        favoriteButton?.apply {
            setImageResource(DesignSystemR.drawable.ic_favorite)
            imageTintList = ColorStateList.valueOf(
                if (state.isFavorited) FAVORITE_ACTIVE_COLOR else ICON_INACTIVE_COLOR
            )
        }
        view?.findViewById<TextView>(R.id.tv_like_count)?.text = state.likeCount.toString()
        view?.findViewById<TextView>(R.id.tv_favorite_count)?.text = state.favoriteCount.toString()
        speedButton?.text = formatSpeedLabel(state.currentSpeed)
        qualityButton?.text = state.currentQualityLabel
        lockButton?.visibility = if (state.isLocked) View.GONE else View.VISIBLE
        unlockButton?.visibility = if (state.isLocked) View.VISIBLE else View.GONE
    }

    private fun loadVideo() {
        if (!isAdded || view == null || playerView == null || videoItem == null) {
            Log.w(TAG, "Fragment not ready, skip loading video")
            return
        }

        try {
            val item = videoItem!!
            
            // ✅ 修复：检查视频是否为 LANDSCAPE，如果不是则拒绝加载
            if (item.orientation != com.ucw.beatu.business.landscape.domain.model.VideoOrientation.LANDSCAPE) {
                Log.w(TAG, "loadVideo: 视频不是 LANDSCAPE 视频，拒绝加载，videoId=${item.id}, orientation=${item.orientation}")
                return
            }
            
            Log.d(TAG, "Loading landscape video: ${item.id} - ${item.videoUrl}")

            viewModel.bindVideoMeta(item)
            playerView?.let { pv ->
                viewModel.playVideo(item.id, item.videoUrl)
                viewModel.preparePlayer(item.id, item.videoUrl, pv)
                if (isCurrentlyVisibleToUser) {
                    viewModel.resume()
                } else {
                    viewModel.pause()
                }
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
        // ✅ 修复：onResume 时也要检查可见性，只有可见时才播放
        if (isAdded && viewModel.uiState.value.currentVideoId != null && isCurrentlyVisibleToUser) {
            Log.d(TAG, "onResume: Fragment 可见，恢复播放 videoId=${viewModel.uiState.value.currentVideoId}")
            viewModel.resume()
        } else {
            Log.d(TAG, "onResume: Fragment 不可见，不播放 videoId=${viewModel.uiState.value.currentVideoId}, isCurrentlyVisibleToUser=$isCurrentlyVisibleToUser")
        }
    }

    fun onParentVisibilityChanged(isVisible: Boolean) {
        isCurrentlyVisibleToUser = isVisible
        if (isVisible) {
            Log.d(TAG, "onParentVisibilityChanged: visible -> resume video ${videoItem?.id}")
            viewModel.resume()
        } else {
            Log.d(TAG, "onParentVisibilityChanged: hidden -> pause video ${videoItem?.id}")
            viewModel.pause()
        }
    }

    override fun onDestroyView() {
        hideControlPanelHandler.removeCallbacks(hideControlPanelRunnable)
        val handoff = viewModel.isHandoffFromPortrait()
        Log.d(TAG, "onDestroyView: handoff=$handoff, videoId=${viewModel.uiState.value.currentVideoId}")
        if (handoff) {
            viewModel.persistPlaybackSession()
            viewModel.mediaPlayer()?.let { player ->
                PlayerView.switchTargetView(player, playerView, null)
            }
            viewModel.releaseCurrentPlayer(force = false)
        } else {
            viewModel.releaseCurrentPlayer()
        }
        super.onDestroyView()
        playerView = null
        rootView = null
    }

    /**
     * 供 Activity 在退出前调用，先保存进度并解绑 Surface，返回竖屏时可无缝继续。
     */
    fun prepareForExit() {
        val videoId = viewModel.uiState.value.currentVideoId
        Log.d(TAG, "prepareForExit: videoId=$videoId")
        
        // ✅ 修复：确保在保存会话时获取最新的播放进度
        viewModel.mediaPlayer()?.let { player ->
            val currentPosition = player.currentPosition
            Log.d(TAG, "prepareForExit: 保存播放会话，当前播放位置=${currentPosition}ms")
            
            // 保存会话（会获取最新的播放进度）
            val session = viewModel.persistPlaybackSession()
            if (session != null) {
                Log.d(TAG, "prepareForExit: 播放会话已保存，视频ID=${session.videoId}，位置=${session.positionMs}ms，是否准备播放=${session.playWhenReady}")
            } else {
                Log.w(TAG, "prepareForExit: 播放会话保存失败，可能播放器未准备好")
            }
            
            // 解绑播放器，但不释放（保持播放器在池中）
            PlayerView.switchTargetView(player, playerView, null)
        } ?: run {
            Log.w(TAG, "prepareForExit: 播放器为null，无法保存会话")
        }
    }
}