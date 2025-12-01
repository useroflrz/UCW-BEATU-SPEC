package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.ui.adapter.VideoFeedAdapter
import com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 推荐页面Fragment
 * 显示推荐视频内容，支持无限上下滑动
 */
@AndroidEntryPoint
class RecommendFragment : Fragment() {

    companion object {
        private const val TAG = "RecommendFragment"
        private const val STATE_VIDEOS = "recommend_videos"
        private const val STATE_VIEWPAGER_INDEX = "recommend_viewpager_index"
        private const val STATE_CURRENT_PAGE = "recommend_current_page"
    }

    private val viewModel: RecommendViewModel by viewModels()

    private var viewPager: ViewPager2? = null
    private var adapter: VideoFeedAdapter? = null
    private var isRefreshing = false
    private var pendingRestoreIndex: Int? = null
    private var pendingResumeRequest = false

    private var gestureDetector: GestureDetector? = null
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_recommend, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewpager_video_feed)
        viewPager?.let { vp ->
            vp.orientation = ViewPager2.ORIENTATION_VERTICAL
            vp.offscreenPageLimit = 1

            // 使用当前 Fragment 作为 FragmentStateAdapter 的宿主，
            // 以便子页面 Fragment 能够挂在 childFragmentManager 下，被 handlePageSelected 正确管理
            adapter = VideoFeedAdapter(this@RecommendFragment)
            vp.adapter = adapter

            vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position >= (adapter?.itemCount ?: 0) - 2) {
                        viewModel.loadMoreVideos()
                    }
                    // 通知当前选中的 Fragment 播放，其他暂停
                    handlePageSelected(position)
                }
            })

            setupPullToRefresh(vp)
        }

        setupSwipeLeftGesture()
        observeViewModel()
        savedInstanceState?.let { restoreState(it) }
    }

    fun onParentTabVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                resumeVisibleVideoItem()
            } else {
                pendingResumeRequest = true
            }
        } else {
            pauseAllVideoItems()
            pendingResumeRequest = false
        }
    }

    override fun onResume() {
        super.onResume()
        // ✅ 修复：确保 View 已创建再执行
        if (pendingResumeRequest && isAdded) {
            resumeVisibleVideoItem()
            pendingResumeRequest = false
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAllVideoItems()
        pendingResumeRequest = true
    }

    private fun setupPullToRefresh(viewPager: ViewPager2) {
        var touchStartY = 0f
        var hasTriggeredRefresh = false

        viewPager.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.y
                    hasTriggeredRefresh = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (viewPager.currentItem == 0 && !isRefreshing && !hasTriggeredRefresh) {
                        val deltaY = event.y - touchStartY
                        if (deltaY > 100 * resources.displayMetrics.density) {
                            hasTriggeredRefresh = true
                            isRefreshing = true
                            viewModel.refreshVideoList()
                            Log.d(TAG, "Trigger pull to refresh (deltaY: $deltaY)")
                        }
                    }
                }
            }
            false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.videoList.isNotEmpty()) {
                        val hasInitialData = (adapter?.itemCount ?: 0) == 0
                        adapter?.updateVideoList(state.videoList)

                        // ✅ 修复：先处理状态恢复，再处理刷新
                        pendingRestoreIndex?.let { target ->
                            val safeIndex = target.coerceIn(0, state.videoList.lastIndex)
                            viewPager?.post { viewPager?.setCurrentItem(safeIndex, false) }
                            pendingRestoreIndex = null
                        }
                        // 刷新完成后回到首页
                        if (isRefreshing && !state.isRefreshing) {
                            isRefreshing = false
                            viewPager?.setCurrentItem(0, false)
                        }

                        // 首次加载完成时，手动触发当前页的播放/轮播（包括图文）
                        if (hasInitialData) {
                            val currentIndex = viewPager?.currentItem ?: 0
                            viewPager?.post { handlePageSelected(currentIndex) }
                        }
                    }

                    state.error?.let { error ->
                        Log.e(TAG, "Error: $error")
                    }
                }
            }
        }
    }

    private fun setupSwipeLeftGesture() {
        rootView?.let { view ->
            gestureDetector = GestureDetector(
                requireContext(),
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (e1 == null) return false
                        val deltaX = e2.x - e1.x
                        val deltaY = e2.y - e1.y
                        val absDeltaX = abs(deltaX)
                        val absDeltaY = abs(deltaY)
                        val minHorizontalDistance = 100 * resources.displayMetrics.density

                        if (absDeltaX > absDeltaY * 2 &&
                            deltaX < 0 &&
                            absDeltaX > minHorizontalDistance &&
                            velocityX < -1000
                        ) {
                            navigateToUserProfile()
                            return true
                        }
                        return false
                    }
                }
            )

            view.setOnTouchListener { _, event ->
                val handled = gestureDetector?.onTouchEvent(event) ?: false
                handled
            }
        }
    }

    private fun navigateToUserProfile() {
        runCatching {
            val navController = findNavController()
            NavigationHelper.navigateByStringId(
                navController,
                NavigationIds.ACTION_FEED_TO_USER_PROFILE,
                requireContext()
            )
        }.onFailure { Log.e(TAG, "Failed to navigate to user profile", it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
        adapter = null
        gestureDetector = null
        rootView = null
        pendingResumeRequest = false
    }

    /**
     * 暂停所有视频（供外部调用，如 FeedFragment）
     */
    fun pauseAllVideoItems() {
        childFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is VideoItemFragment -> fragment.onParentVisibilityChanged(false)
                is ImagePostFragment -> fragment.onParentVisibilityChanged(false)
            }
        }
    }

    /**
     * 恢复当前可见的视频（供外部调用，如 FeedFragment）
     */
    fun resumeVisibleVideoItem() {
        val currentPosition = viewPager?.currentItem ?: -1
        if (currentPosition >= 0) {
            handlePageSelected(currentPosition)
        }
    }
    
    /**
     * 处理页面选中事件：暂停所有视频，只播放当前可见的视频
     */
    private fun handlePageSelected(position: Int) {
        // ViewPager2 的 FragmentStateAdapter 使用 "f" + position 作为 Fragment tag
        val currentFragmentTag = "f$position"
        val currentFragment = childFragmentManager.findFragmentByTag(currentFragmentTag)

        childFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is VideoItemFragment -> {
                    if (fragment == currentFragment && fragment.isVisible) {
                        fragment.checkVisibilityAndPlay()
                    } else {
                        Log.d(TAG, "handlePageSelected: pause VideoItemFragment tag=${fragment.tag}")
                        fragment.onParentVisibilityChanged(false)
                    }
                }
                is ImagePostFragment -> {
                    if (fragment == currentFragment && fragment.isVisible) {
                        fragment.checkVisibilityAndPlay()
                    } else {
                        Log.d(TAG, "handlePageSelected: pause ImagePostFragment tag=${fragment.tag}")
                        fragment.onParentVisibilityChanged(false)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val videos = ArrayList(viewModel.uiState.value.videoList)
        if (videos.isNotEmpty()) {
            outState.putParcelableArrayList(STATE_VIDEOS, videos)
            outState.putInt(STATE_VIEWPAGER_INDEX, viewPager?.currentItem ?: 0)
            outState.putInt(STATE_CURRENT_PAGE, viewModel.getCurrentPage())
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val videos = BundleCompat.getParcelableArrayList(savedInstanceState, STATE_VIDEOS, VideoItem::class.java)
        val restoredIndex = savedInstanceState.getInt(STATE_VIEWPAGER_INDEX, 0)
        val restoredPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, 1)
        if (!videos.isNullOrEmpty()) {
            pendingRestoreIndex = restoredIndex
            viewModel.restoreState(videos, restoredPage)
        }
    }
}