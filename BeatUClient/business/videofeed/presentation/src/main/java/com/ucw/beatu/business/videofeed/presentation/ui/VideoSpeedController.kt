package com.ucw.beatu.business.videofeed.presentation.ui

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.media3.ui.PlayerView
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.designsystem.widget.VideoControlsView

/**
 * 封装 VideoItemFragment 的倍速长按交互逻辑，降低 Fragment 体积与职责复杂度。
 */
class VideoSpeedController(
    private val tag: String,
    private val playerViewProvider: () -> PlayerView?,
    private val speedControlOverlayProvider: () -> View?,
    private val controlsViewProvider: () -> VideoControlsView?,
    private val rootLayoutProvider: () -> ConstraintLayout?,
    private val rootViewProvider: () -> View?,
    private val parentFragmentProvider: () -> Fragment?,
    private val hostActivityProvider: () -> FragmentActivity?,
    private val isUserInfoVisibleProvider: () -> Boolean,
    private val isImagePostProvider: () -> Boolean,
    private val currentSpeedProvider: () -> Float,
    private val setSpeed: (Float) -> Unit
) {
    private var isSpeedAdjusting = false
    private var speedBeforeAdjust: Float = 1.0f
    private var gestureDetector: GestureDetector? = null
    private var touchStartX = 0f
    private var lastTouchY = 0f
    private val rootLocation = IntArray(2)
    private var wasControlsVisible = true
    private var wasTopNavigationVisible = true
    private var wasViewPagerEnabled = true
    private var isSpeedLocked = false
    private var isUnlocking = false
    private var speedBeforeLock: Float = 1.0f

    fun isAdjusting(): Boolean = isSpeedAdjusting

    fun setup() {
        val playerView = playerViewProvider() ?: return
        val context = playerView.context
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isUserInfoVisibleProvider() || isImagePostProvider()) {
                    return
                }
                startSpeedAdjusting()
            }
        })

        playerView.setOnTouchListener { _, event ->
            if (isUserInfoVisibleProvider()) {
                return@setOnTouchListener false
            }
            if (isSpeedAdjusting) {
                handleSpeedAdjustingTouch(event)
                return@setOnTouchListener true
            }
            gestureDetector?.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                touchStartX = event.x
                lastTouchY = event.y
            }
            false
        }
    }

    fun clear() {
        playerViewProvider()?.setOnTouchListener(null)
        speedControlOverlayProvider()?.setOnTouchListener(null)
        gestureDetector = null
    }

    private fun startSpeedAdjusting() {
        if (isSpeedAdjusting) return
        isSpeedAdjusting = true

        val currentSpeed = currentSpeedProvider()
        speedBeforeAdjust = currentSpeed

        val wasLocked = isSpeedLocked && currentSpeed == 2.0f
        isUnlocking = wasLocked
        if (wasLocked) {
            Log.d(tag, "开始倍速调节（解锁模式），当前倍速=$currentSpeed，将恢复为 ${speedBeforeLock}x")
        } else {
            isSpeedLocked = false
            Log.d(tag, "开始倍速调节，当前倍速=$currentSpeed，设置为 2.0x")
        }

        disableViewPagerScrolling()
        requestDisallowInterceptTouchEventForAllParents(true)
        hideBottomControlsAndTopNavigation()

        if (!isUnlocking) {
            setSpeed(2.0f)
        }

        showSpeedControlOverlay()
        highlightLockArea(false)
    }

    private fun handleSpeedAdjustingTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                lastTouchY = event.rawY
                requestDisallowInterceptTouchEventForAllParents(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.rawY - lastTouchY
                lastTouchY = event.rawY
                requestDisallowInterceptTouchEventForAllParents(true)

                rootLayoutProvider()?.getLocationOnScreen(rootLocation)
                val rootLayoutHeight = rootLayoutProvider()?.height ?: 1
                val touchYInRoot = event.rawY - rootLocation[1]
                val bottomThreshold = rootLayoutHeight - dpToPx(150)

                if (deltaY > 0 && touchYInRoot >= bottomThreshold) {
                    highlightLockArea(true)
                } else {
                    highlightLockArea(false)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                rootLayoutProvider()?.getLocationOnScreen(rootLocation)
                val rootLayoutHeight = rootLayoutProvider()?.height ?: 1
                val touchYInRoot = event.rawY - rootLocation[1]
                val bottomThreshold = rootLayoutHeight - dpToPx(150)

                if (touchYInRoot >= bottomThreshold && isSpeedAdjusting) {
                    if (isUnlocking) {
                        setSpeed(speedBeforeLock)
                        isSpeedLocked = false
                        Log.d(tag, "解锁倍速，恢复到 ${speedBeforeLock}x")
                    } else {
                        speedBeforeLock = speedBeforeAdjust
                        lockSpeed(2.0f)
                        isSpeedLocked = true
                        Log.d(tag, "锁定倍速为 2.0x，原始倍速=${speedBeforeLock}x")
                    }
                } else if (!isUnlocking && !isSpeedLocked) {
                    setSpeed(speedBeforeAdjust)
                    Log.d(tag, "恢复倍速到 ${speedBeforeAdjust}x")
                }

                endSpeedAdjusting()
            }
        }
        return true
    }

    private fun requestDisallowInterceptTouchEventForAllParents(disallow: Boolean) {
        fun walk(parent: ViewParent?) {
            var current = parent
            while (current != null) {
                current.requestDisallowInterceptTouchEvent(disallow)
                current = current.parent
            }
        }
        walk(playerViewProvider()?.parent)
        walk(speedControlOverlayProvider()?.parent)
        walk(rootLayoutProvider()?.parent)
    }

    private fun dpToPx(dp: Int): Int {
        val density = playerViewProvider()?.resources?.displayMetrics?.density ?: 1f
        return (dp * density).toInt()
    }

    private fun showSpeedControlOverlay() {
        val overlay = speedControlOverlayProvider() ?: run {
            Log.e(tag, "showSpeedControlOverlay: speedControlOverlay 为 null!")
            return
        }

        controlsViewProvider()?.elevation = 0f
        overlay.visibility = View.VISIBLE
        overlay.setOnTouchListener { _, event ->
            if (isSpeedAdjusting) {
                requestDisallowInterceptTouchEventForAllParents(true)
                handleSpeedAdjustingTouch(event)
                true
            } else {
                false
            }
        }

        rootViewProvider()?.post {
            val parent = overlay.parent as? ViewGroup
            parent?.bringChildToFront(overlay)
            overlay.bringToFront()
            overlay.elevation = 20f

            overlay.measure(
                View.MeasureSpec.makeMeasureSpec(overlay.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(overlay.height, View.MeasureSpec.EXACTLY)
            )
            overlay.layout(overlay.left, overlay.top, overlay.right, overlay.bottom)

            val indicator = overlay.findViewById<android.widget.TextView>(R.id.tv_speed_indicator)
            indicator?.apply {
                visibility = View.VISIBLE
                elevation = 22f
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                layout(left, top, left + measuredWidth, top + measuredHeight)
                bringToFront()
                (overlay as? ViewGroup)?.bringChildToFront(this)
            }

            overlay.findViewById<View>(R.id.ll_lock_area)?.visibility = View.VISIBLE
            updateSpeedIndicator(2.0f)
            highlightLockArea(false)

            rootViewProvider()?.postDelayed({
                val indicator2 = overlay.findViewById<android.widget.TextView>(R.id.tv_speed_indicator)
                indicator2?.apply {
                    if (width == 0 || height == 0) {
                        layoutParams.width = dpToPx(140)
                        layoutParams.height = dpToPx(44)
                        requestLayout()
                        invalidate()
                    }
                }
            }, 100)
        }
    }

    private fun updateSpeedIndicator(speed: Float) {
        speedControlOverlayProvider()?.findViewById<android.widget.TextView>(R.id.tv_speed_indicator)?.apply {
            text = "${speed}x 倍速播放中"
            visibility = View.VISIBLE
            bringToFront()
            (parent as? ViewGroup)?.bringChildToFront(this)
        }
    }

    private fun highlightLockArea(highlight: Boolean) {
        speedControlOverlayProvider()?.findViewById<android.widget.LinearLayout>(R.id.ll_lock_area)?.apply {
            alpha = if (highlight) 1.0f else 0.7f
        }
        speedControlOverlayProvider()?.findViewById<android.widget.TextView>(R.id.tv_lock_hint)?.apply {
            text = if (isUnlocking) "拖到此处恢复正常倍速" else "拖到此处锁定 2 倍速"
        }
    }

    private fun lockSpeed(speed: Float) {
        setSpeed(speed)
        Log.d(tag, "锁定倍速为 ${speed}x（需要接入 Settings 模块保存为默认倍速）")
    }

    private fun endSpeedAdjusting() {
        if (!isSpeedAdjusting) return
        isSpeedAdjusting = false
        isUnlocking = false

        controlsViewProvider()?.elevation = 0f
        speedControlOverlayProvider()?.visibility = View.GONE
        enableViewPagerScrolling()
        showBottomControlsAndTopNavigation()

        Log.d(tag, "结束倍速调节，倍速是否锁定：$isSpeedLocked")
    }

    private fun disableViewPagerScrolling() {
        val parent = parentFragmentProvider()
        if (parent is RecommendFragment) {
            parent.disableViewPagerScrolling()
            wasViewPagerEnabled = true
            Log.d(tag, "禁止ViewPager2滑动")
        }
    }

    private fun enableViewPagerScrolling() {
        val parent = parentFragmentProvider()
        if (parent is RecommendFragment && wasViewPagerEnabled) {
            parent.enableViewPagerScrolling()
            Log.d(tag, "恢复ViewPager2滑动")
        }
    }

    private fun hideBottomControlsAndTopNavigation() {
        wasControlsVisible = controlsViewProvider()?.visibility == View.VISIBLE
        controlsViewProvider()?.visibility = View.GONE

        hostActivityProvider()?.let { act ->
            val topNavId = act.resources.getIdentifier("top_navigation", "id", act.packageName)
            if (topNavId != 0) {
                act.findViewById<View>(topNavId)?.let { topNav ->
                    wasTopNavigationVisible = topNav.visibility == View.VISIBLE
                    topNav.visibility = View.GONE
                }
            }
        }
    }

    private fun showBottomControlsAndTopNavigation() {
        if (wasControlsVisible) {
            controlsViewProvider()?.visibility = View.VISIBLE
        }
        if (wasTopNavigationVisible) {
            hostActivityProvider()?.let { act ->
                val topNavId = act.resources.getIdentifier("top_navigation", "id", act.packageName)
                if (topNavId != 0) {
                    act.findViewById<View>(topNavId)?.visibility = View.VISIBLE
                }
            }
        }
    }
}
