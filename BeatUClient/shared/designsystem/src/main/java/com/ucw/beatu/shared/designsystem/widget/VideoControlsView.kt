package com.ucw.beatu.shared.designsystem.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.ucw.beatu.shared.designsystem.R

/**
 * 通用视频控制视图（竖屏主场景）
 *
 * - 展示播放/暂停按钮覆盖层
 * - 展示基础互动信息：点赞/收藏数量
 * - 展示进度条与当前进度/总时长
 *
 * 通过 [VideoControlsState] + [VideoControlsListener] 与业务解耦：
 * - 状态只读，由 ViewModel 暴露
 * - 交互通过回调上抛，由 Fragment/Activity 转给 ViewModel
 */
class VideoControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private val LIKE_ACTIVE_COLOR = 0xFFFF4D4F.toInt()
        private val FAVORITE_ACTIVE_COLOR = 0xFFFFD700.toInt()
        private val ICON_INACTIVE_COLOR = 0xFFFFFFFF.toInt()
    }

    data class VideoControlsState(
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val isLiked: Boolean = false,
        val isFavorited: Boolean = false,
        val likeCount: Long = 0L,
        val favoriteCount: Long = 0L
    )

    interface VideoControlsListener {
        fun onPlayPauseClicked()
        fun onLikeClicked()
        fun onFavoriteClicked()
        fun onCommentClicked()
        fun onShareClicked()
        fun onSeekRequested(positionMs: Long) {}
    }

    private var playButton: ImageView
    private var likeIcon: ImageView
    private var favoriteIcon: ImageView
    private var likeCountText: TextView
    private var favoriteCountText: TextView
    private var progressBar: SeekBar
    private var progressTimeText: TextView
    private var topInfoContainer: View          // 绿色层：集数/合集/标题
    private var bottomChannelContainer: View    // 蓝色层：作者头像 + 互动区
    private var seekContainer: View             // 红色层：进度条所在行

    // 进度条显示/手势相关状态
    private var isSeekUiVisible: Boolean = false          // 进度条和时间是否可见（但行本身始终占位）
    private var isDraggingInHiddenZone: Boolean = false   // 播放中在“隐藏区”里横向拖动中
    private var isUserSeekingOnBar: Boolean = false       // 手指直接在 SeekBar 上拖动中
    private var dragStartX: Float = 0f
    private var basePositionMs: Long = 0L

    var listener: VideoControlsListener? = null

    var state: VideoControlsState = VideoControlsState()
        set(value) {
            field = value
            renderState(value)
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_video_controls, this, true)

        playButton = findViewById(R.id.iv_play_button_overlay)
        likeIcon = findViewById(R.id.iv_like)
        favoriteIcon = findViewById(R.id.iv_favorite)
        likeCountText = findViewById(R.id.tv_like_count)
        favoriteCountText = findViewById(R.id.tv_favorite_count)
        progressBar = findViewById(R.id.progress_playback)
        progressTimeText = findViewById(R.id.tv_progress_time)
        topInfoContainer = findViewById(R.id.layout_top_info)
        bottomChannelContainer = findViewById(R.id.layout_bottom_channel)
        seekContainer = findViewById(R.id.layout_seek_container)

        // 默认：进度条所在整行始终占位，但处于“隐藏 UI”状态（看不见、仍可点）
        setSeekUiVisibleInternal(false)

        // 先隐藏进度条文字信息，只保留条本身
        progressTimeText.visibility = View.GONE

        playButton.setOnClickListener { listener?.onPlayPauseClicked() }
        likeIcon.setOnClickListener { listener?.onLikeClicked() }
        favoriteIcon.setOnClickListener { listener?.onFavoriteClicked() }
        findViewById<View>(R.id.iv_comment).setOnClickListener { listener?.onCommentClicked() }
        findViewById<View>(R.id.iv_share).setOnClickListener { listener?.onShareClicked() }

        // 普通 SeekBar 拖动（进度条已经可见的情况，例如暂停时）
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val duration = state.durationMs.coerceAtLeast(0L)
                if (duration <= 0L) return
                val position = (duration * progress / seekBar!!.max).coerceIn(0L, duration)
                progressTimeText.text = formatTime(position) + " / " + formatTime(duration)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeekingOnBar = true
                enterSeekingMode()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val duration = state.durationMs.coerceAtLeast(0L)
                if (duration <= 0L || seekBar == null) return
                val position = (duration * seekBar.progress / seekBar.max).coerceIn(0L, duration)
                listener?.onSeekRequested(position)
                isUserSeekingOnBar = false
                // 先恢复上下两层
                exitSeekingMode()
                // 再根据当前播放状态立即控制进度条显示，避免依赖后续 state 刷新产生可见延迟
                if (state.isPlaying) {
                    setSeekUiVisibleInternal(false)
                } else {
                    setSeekUiVisibleInternal(true)
                }
            }
        })

        // 播放中时，在“进度条所在行（隐藏区）”按住并左右滑动：呼出进度条并做相对 Seek
        seekContainer.setOnTouchListener { _, event ->
            handleHiddenSeekGesture(event)
        }
    }

    private fun renderState(state: VideoControlsState) {
        // 播放按钮覆盖层：播放中隐藏，暂停时显示
        playButton.visibility = if (state.isPlaying) View.GONE else View.VISIBLE

        // 点赞/收藏数量 + 点亮状态
        likeCountText.text = state.likeCount.toString()
        favoriteCountText.text = state.favoriteCount.toString()
        likeIcon.setImageResource(R.drawable.ic_like)
        likeIcon.imageTintList = ColorStateList.valueOf(
            if (state.isLiked) LIKE_ACTIVE_COLOR else ICON_INACTIVE_COLOR
        )
        favoriteIcon.setImageResource(R.drawable.ic_favorite)
        favoriteIcon.imageTintList = ColorStateList.valueOf(
            if (state.isFavorited) FAVORITE_ACTIVE_COLOR else ICON_INACTIVE_COLOR
        )

        // 进度条数值更新
        val duration = state.durationMs.coerceAtLeast(0L)
        val position = state.currentPositionMs.coerceIn(0L, duration.takeIf { it > 0 } ?: Long.MAX_VALUE)

        if (duration > 0) {
            val safeMax = progressBar.max.takeIf { it > 0 } ?: 1000
            val progress = ((position.toDouble() / duration.toDouble()) * safeMax)
                .toInt()
                .coerceIn(0, safeMax)
            progressBar.progress = progress
        }

        progressTimeText.text = formatTime(position) + " / " + formatTime(duration)

        // A：点视频 = 播放/暂停切换，暂停时才显示进度条
        // - 暂停：始终显示进度条 UI
        // - 播放中：默认隐藏 UI，但如果正在隐藏区拖动或直接拖动进度条，则保持显示
        val shouldShowSeekUi = !state.isPlaying || isDraggingInHiddenZone || isUserSeekingOnBar
        setSeekUiVisibleInternal(shouldShowSeekUi)
    }

    /**
     * 供外部（如 VideoItemFragment）调用：
     * - 正常播放时：隐藏进度条，仅显示底部信息
     * - 单击视频区域时：显示进度条（与底部信息同时存在）
     */
    fun toggleSeekControls() {
        // 保持 API 向后兼容，但现在只切换“UI 是否可见”，不再影响占位和点击区域
        setSeekUiVisibleInternal(!isSeekUiVisible)
    }

    fun showSeekControls() {
        setSeekUiVisibleInternal(true)
    }

    fun hideSeekControls() {
        setSeekUiVisibleInternal(false)
    }

    /**
     * 拖动进度条时进入“专注模式”：只保留中间红色层（进度条），
     * 上下两层（绿色+蓝色）只是不显示，但仍然占位（通过 alpha 控制）
     */
    private fun enterSeekingMode() {
        setTopBottomVisibleForSeek(false)
        setSeekUiVisibleInternal(true)
    }

    /**
     * 拖动结束后恢复上下两层（绿色+蓝色）的可见性
     */
    private fun exitSeekingMode() {
        setTopBottomVisibleForSeek(true)
    }

    /**
     * 在 Seek 模式下只通过透明度隐藏/显示上下两层：
     * - 保持布局位置与高度不变（仍然占位）
     * - 视觉上只看到中间红色进度条
     */
    private fun setTopBottomVisibleForSeek(visible: Boolean) {
        val alpha = if (visible) 1f else 0f
        topInfoContainer.alpha = alpha
        bottomChannelContainer.alpha = alpha
    }

    /**
     * 内部切换进度条 UI 是否可见：
     * - 整行（layout_seek_container）始终可点、占位不变
     * - “隐藏”仅通过 alpha 控制，让用户看不见但仍能点击触发手势
     */
    private fun setSeekUiVisibleInternal(visible: Boolean) {
        isSeekUiVisible = visible
        val alpha = if (visible) 1f else 0f
        progressBar.alpha = alpha
        progressTimeText.alpha = alpha
    }

    /**
     * B：播放中时，在进度条所在那一行的“隐藏区”按住并左右滑动：
     * - 呼出进度条
     * - 视频进度根据“当前时间基础上的相对位移”做 Seek
     */
    private fun handleHiddenSeekGesture(event: MotionEvent): Boolean {
        // 若没有有效时长，直接不处理，交给子 View
        if (state.durationMs <= 0L) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 若当前已经在可见的 SeekBar 上（例如暂停状态），交给子 View 处理
                if (isSeekUiVisible && !state.isPlaying) {
                    return false
                }
                isDraggingInHiddenZone = true
                dragStartX = event.x
                basePositionMs = state.currentPositionMs
                // 播放中从隐藏区唤出进度条
                setSeekUiVisibleInternal(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDraggingInHiddenZone) return false
                val width = seekContainer.width.takeIf { it > 0 } ?: return true
                val dx = event.x - dragStartX
                // 按整行宽度映射到整段时长的相对位移
                val deltaFraction = dx / width.toFloat()
                val duration = state.durationMs.coerceAtLeast(0L)
                val deltaMs = (duration * deltaFraction).toLong()
                val newPosition = (basePositionMs + deltaMs).coerceIn(0L, duration)

                // 更新本地 UI
                val safeMax = progressBar.max.takeIf { it > 0 } ?: 1000
                val progress = ((newPosition.toDouble() / duration.toDouble()) * safeMax)
                    .toInt()
                    .coerceIn(0, safeMax)
                progressBar.progress = progress
                progressTimeText.text = formatTime(newPosition) + " / " + formatTime(duration)

                // 通知外部立刻 Seek，形成“滑动即跟随”的体验
                listener?.onSeekRequested(newPosition)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!isDraggingInHiddenZone) return false
                isDraggingInHiddenZone = false
                // 播放中：手势结束后可以把进度条再次隐藏，交给状态渲染逻辑统一处理
                if (state.isPlaying) {
                    setSeekUiVisibleInternal(false)
                }
                return true
            }
        }

        return false
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}


