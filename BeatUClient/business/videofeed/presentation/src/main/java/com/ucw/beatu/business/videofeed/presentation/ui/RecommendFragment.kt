package com.ucw.beatu.business.videofeed.presentation.ui

import android.content.res.Configuration
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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.ui.adapter.VideoFeedAdapter
import com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragment.MainActivityBridge
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
    // ✅ 修复严重问题3：移除手动维护的 isLandscapeMode，改用系统配置实时获取
    // 不再手动维护状态，避免与系统不同步
    private var previousDestinationId: Int? = null // 记录之前的导航目标，用于检测返回
    private var navigationListener: androidx.navigation.NavController.OnDestinationChangedListener? = null // ✅ 修复中度问题7：保存 listener 引用以便清理

    // MainActivity 引用（用于控制加载动画）
    private var mainActivity: MainActivityBridge? = null

    // 下拉刷新状态管理
    private var pullToRefreshState = PullToRefreshState.IDLE
    private var pullStartY = 0f
    private var pullCurrentY = 0f
    private var pullThreshold = 0f // 下拉阈值（将在 onViewCreated 中初始化）

    // 下拉刷新状态枚举
    private enum class PullToRefreshState {
        IDLE,           // 空闲状态
        PULLING,        // 正在下拉
        REFRESHING,     // 正在刷新
        COMPLETED       // 刷新完成
    }

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

        // 获取 MainActivity 引用
        mainActivity = activity as? MainActivityBridge

        // 初始化下拉刷新阈值
        pullThreshold = 100f * resources.displayMetrics.density

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
                    val totalCount = adapter?.itemCount ?: 0
                    // 预加载机制：提前2页开始加载，确保用户滑动流畅
                    if (position >= totalCount - 2) {
                        viewModel.loadMoreVideos()
                    }
                    // 通知当前选中的 Fragment 播放，其他暂停
                    handlePageSelected(position)
                }
            })

            setupPullToRefresh(vp)
        }

        setupSwipeLeftGesture()
        setupNavigationListener()
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
        // ✅ 修复严重问题2：移除 onResume 中的横屏恢复逻辑，统一由 notifyExitLandscapeMode() 处理
        // 避免重复调用恢复播放器
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // ✅ 修复严重问题3：使用系统配置实时判断，不再依赖手动维护的状态
        // 由于MainActivity配置了configChanges，屏幕旋转时会调用此方法
        // 使用post延迟执行，避免阻塞主线程
        view?.post {
            val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
            
            // ✅ 修复严重问题2：统一恢复逻辑，只在一个地方处理
            if (isLandscape) {
                // 检测到横屏，切换到landscape模式
                checkOrientationAndSwitch()
            } else if (isPortrait) {
                // 从横屏返回竖屏，恢复播放器（统一入口）
                Log.d(TAG, "onConfigurationChanged: 检测到竖屏，准备恢复播放器")
                restorePlayerSafely()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAllVideoItems()
        pendingResumeRequest = true
    }

    /**
     * 设置下拉刷新功能
     * 使用"手势判断 + 状态管理 + 动画控制"的思路
     */
    private fun setupPullToRefresh(viewPager: ViewPager2) {
        // 获取 ViewPager2 内部的 RecyclerView
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return

        // 在 RecyclerView 上设置触摸监听，这样可以更可靠地拦截事件
        recyclerView.setOnTouchListener { v, event ->
            handlePullToRefreshTouch(viewPager, recyclerView, event)
        }
    }

    /**
     * 处理下拉刷新的触摸事件
     * @param viewPager ViewPager2 实例
     * @param recyclerView RecyclerView 实例
     * @param event 触摸事件
     * @return 是否消费了事件
     */
    private fun handlePullToRefreshTouch(
        viewPager: ViewPager2,
        recyclerView: RecyclerView,
        event: MotionEvent
    ): Boolean {
        // 只在第一个视频时处理下拉刷新
        if (viewPager.currentItem != 0) {
            resetPullToRefreshState()
            return false
        }

        // 检查 RecyclerView 是否在顶部（不能向上滚动）
        val canScrollUp = recyclerView.canScrollVertically(-1)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                // 手势开始：记录起始位置
                if (!canScrollUp && pullToRefreshState == PullToRefreshState.IDLE) {
                    pullStartY = event.y
                    pullCurrentY = event.y
                    pullToRefreshState = PullToRefreshState.PULLING
                    Log.d(TAG, "PullToRefresh: ACTION_DOWN, startY=$pullStartY")
                }
            }

                MotionEvent.ACTION_MOVE -> {
                if (pullToRefreshState == PullToRefreshState.PULLING) {
                    pullCurrentY = event.y
                    val deltaY = pullCurrentY - pullStartY // 向下滑动，deltaY 为正

                    Log.d(TAG, "PullToRefresh: ACTION_MOVE, deltaY=$deltaY, threshold=$pullThreshold, canScrollUp=$canScrollUp")

                    // 检查是否是向下滑动且超过阈值
                    if (deltaY > 0 && !canScrollUp) {
                        // 如果超过阈值且还未触发刷新，则触发刷新
                        if (deltaY > pullThreshold && !isRefreshing) {
                            triggerRefresh()
                            return true // 消费事件，防止 ViewPager2 滑动
                        }
                    } else {
                        // 如果向上滑动或可以向上滚动，重置状态
                        resetPullToRefreshState()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                Log.d(TAG, "PullToRefresh: ACTION_UP/CANCEL, state=$pullToRefreshState")
                // 如果正在下拉但未触发刷新，重置状态
                if (pullToRefreshState == PullToRefreshState.PULLING) {
                    resetPullToRefreshState()
                }
            }
        }

        // 如果正在刷新，消费事件防止 ViewPager2 滑动
        return pullToRefreshState == PullToRefreshState.REFRESHING
    }

    /**
     * 触发刷新
     */
    private fun triggerRefresh() {
        if (isRefreshing || pullToRefreshState == PullToRefreshState.REFRESHING) {
            return
        }

        pullToRefreshState = PullToRefreshState.REFRESHING
        isRefreshing = true

        Log.d(TAG, "PullToRefresh: Trigger refresh, mainActivity=${mainActivity != null}")

        // 隐藏"推荐"文字
        mainActivity?.hideRecommendText()

        // 暂停当前视频
        pauseAllVideoItems()

        // 调用 ViewModel 刷新
        viewModel.refreshVideoList()
    }

    /**
     * 重置下拉刷新状态
     */
    private fun resetPullToRefreshState() {
        if (pullToRefreshState == PullToRefreshState.PULLING) {
            pullToRefreshState = PullToRefreshState.IDLE
            pullStartY = 0f
            pullCurrentY = 0f
            Log.d(TAG, "PullToRefresh: Reset state to IDLE")
        }
    }

    /**
     * 完成刷新（由 observeViewModel 调用）
     */
    private fun completeRefresh() {
        if (isRefreshing) {
            pullToRefreshState = PullToRefreshState.COMPLETED
            isRefreshing = false

            Log.d(TAG, "PullToRefresh: Refresh completed")

            // 显示"推荐"文字
            mainActivity?.showRecommendText()

            // 延迟重置状态，避免立即触发新的下拉
            viewPager?.postDelayed({
                pullToRefreshState = PullToRefreshState.IDLE
            }, 300)
        }
    }

    /**
     * 刷新视频列表（供外部调用）
     */
    fun refreshVideoList() {
        if (!isRefreshing) {
            Log.d(TAG, "refreshVideoList called, mainActivity: ${mainActivity != null}")
            isRefreshing = true
            mainActivity?.hideRecommendText()

            // 刷新时暂停当前视频
            pauseAllVideoItems()
            Log.d(TAG, "Paused all videos before refresh")

            viewModel.refreshVideoList()
        } else {
            Log.d(TAG, "refreshVideoList called but already refreshing")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "observeViewModel: isRefreshing=${state.isRefreshing}, local isRefreshing=$isRefreshing, videoList.size=${state.videoList.size}")

                    // 监听刷新状态，控制加载动画（必须在列表处理之前，确保状态及时更新）
                    var refreshJustCompleted = false
                    if (state.isRefreshing) {
                        if (!isRefreshing) {
                            // 开始刷新（可能是双击触发或其他方式触发）
                            isRefreshing = true
                            pullToRefreshState = PullToRefreshState.REFRESHING
                            Log.d(TAG, "State is refreshing")
                            mainActivity?.hideRecommendText()
                        }
                    } else {
                        if (isRefreshing) {
                            // 刷新完成
                            Log.d(TAG, "Refresh completed, videoList.size=${state.videoList.size}")
                            refreshJustCompleted = true
                            completeRefresh() // 使用统一的状态管理方法
                        }
                    }

                    if (state.videoList.isNotEmpty()) {
                        val hasInitialData = (adapter?.itemCount ?: 0) == 0
                        val previousListSize = adapter?.itemCount ?: 0
                        val currentViewPagerItem = viewPager?.currentItem ?: 0

                        // 检查列表的第一个视频是否变化（用于检测列表是否被替换）
                        val previousFirstVideo = adapter?.getVideoAt(0)?.id
                        val newFirstVideo = state.videoList.firstOrNull()?.id
                        val isListReplaced = !hasInitialData && previousFirstVideo != null && previousFirstVideo != newFirstVideo

                        // 打印更新前的状态
                        Log.d(TAG, "observeViewModel: Updating video list - previousSize=$previousListSize, currentSize=${state.videoList.size}, currentViewPagerItem=$currentViewPagerItem")
                        Log.d(TAG, "observeViewModel: previousFirstVideo=$previousFirstVideo, newFirstVideo=$newFirstVideo, isListReplaced=$isListReplaced")
                        if (state.videoList.isNotEmpty()) {
                            Log.d(TAG, "observeViewModel: First video in new list: ${state.videoList[0].id}")
                        }

                        // ViewModel 已经将新视频插入到完整列表顶部，直接更新整个列表
                        adapter?.updateVideoList(state.videoList, state.hasLoadedAllFromBackend)

                        // 刷新完成后，立即跳转到第一个视频并播放（优先级最高）
                        if (refreshJustCompleted) {
                            Log.d(TAG, "Refresh completed: updated list with ${state.videoList.size} videos, jumping to first video immediately")
                            val firstVideoId = state.videoList.firstOrNull()?.id
                            Log.d(TAG, "Refresh completed: First video in new list: $firstVideoId")

                            // 先暂停所有视频，确保旧 Fragment 都暂停
                            pauseAllVideoItems()

                            // 等待 adapter 更新完成，然后跳转到第一个视频
                            // 使用 post 确保在下一个 UI 周期执行，让 ViewPager2 有时间更新 Fragment
                            viewPager?.post {
                                val currentItem = viewPager?.currentItem ?: 0
                                Log.d(TAG, "Refresh completed: currentViewPagerItem=$currentItem, jumping to position 0")
                                // 刷新后总是跳转到第一个视频（新列表的第一个）
                                viewPager?.setCurrentItem(0, false)

                                // 等待 ViewPager2 更新 Fragment，然后验证并触发播放
                                // 由于我们重写了 getItemId，ViewPager2 会知道需要重新创建 Fragment
                                viewPager?.postDelayed({
                                    // 验证第一个 Fragment 是否匹配预期的 videoId
                                    val itemIdAt0 = adapter?.getItemId(0) ?: 0L
                                    val fragmentAt0 = childFragmentManager.findFragmentByTag("f$itemIdAt0")
                                    val fragmentVideoId = when (fragmentAt0) {
                                        is VideoItemFragment -> {
                                            fragmentAt0.arguments?.let {
                                                androidx.core.os.BundleCompat.getParcelable(it, "video_item", com.ucw.beatu.shared.common.model.VideoItem::class.java)
                                            }?.id
                                        }
                                        is ImagePostFragment -> {
                                            fragmentAt0.arguments?.let {
                                                androidx.core.os.BundleCompat.getParcelable(it, "image_post_item", com.ucw.beatu.shared.common.model.VideoItem::class.java)
                                            }?.id
                                        }
                                        else -> null
                                    }

                                    if (fragmentVideoId == firstVideoId) {
                                        Log.d(TAG, "Refresh completed: Fragment at position 0 matches expected videoId=$firstVideoId, triggering playback")
                                        // Fragment 已更新，触发播放
                                        handlePageSelected(0)
                                    } else {
                                        Log.w(TAG, "Refresh completed: Fragment at position 0 doesn't match (expected=$firstVideoId, actual=$fragmentVideoId), waiting longer")
                                        // Fragment 还没更新，再等待一下
                                        viewPager?.postDelayed({
                                            handlePageSelected(0)
                                            Log.d(TAG, "Refresh completed: Retrying playback after additional delay")
                                        }, 200)
                                    }
                                }, 300)
                            }
                        } else {
                            // 如果列表被替换（远程数据替换本地缓存），但不是刷新完成，也需要确保 ViewPager2 显示正确的第一个视频
                            if (isListReplaced) {
                                Log.d(TAG, "observeViewModel: List replaced (remote data replaced local cache), resetting ViewPager2 to first item")
                                viewPager?.post {
                                    viewPager?.setCurrentItem(0, false)
                                    // 确保第一个视频正确播放
                                    viewPager?.postDelayed({
                                        handlePageSelected(0)
                                        Log.d(TAG, "observeViewModel: Reset ViewPager2 to first video after list replacement")
                                    }, 100)
                                }
                            }

                        // ✅ 修复：先处理状态恢复，再处理刷新
                        pendingRestoreIndex?.let { target ->
                            val safeIndex = target.coerceIn(0, state.videoList.lastIndex)
                            viewPager?.post { viewPager?.setCurrentItem(safeIndex, false) }
                            pendingRestoreIndex = null
                        }
                        }

                        // 首次加载完成时，手动触发当前页的播放/轮播（包括图文）
                        if (hasInitialData) {
                            val currentIndex = viewPager?.currentItem ?: 0
                            viewPager?.post { handlePageSelected(currentIndex) }
                        }
                    } else {
                        Log.d(TAG, "Video list is empty")
                    }

                    state.error?.let { error ->
                        Log.e(TAG, "Error: $error")
                        // 如果有错误，也要重置刷新状态并显示推荐文字
                        if (isRefreshing) {
                            completeRefresh() // 使用统一的状态管理方法
                        }
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

                        // 只处理明显的水平滑动（水平距离远大于垂直距离）
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
                // 只处理水平滑动，垂直滑动（下拉刷新）让 ViewPager2 处理
                val handled = gestureDetector?.onTouchEvent(event) ?: false
                // 如果是垂直滑动，返回 false 让事件继续传递
                if (!handled) {
                    false
                } else {
                handled
                }
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
        // ✅ 修复中度问题7：清理 navigation listener，避免 fragment 销毁后仍被回调
        navigationListener?.let {
            try {
                findNavController().removeOnDestinationChangedListener(it)
            } catch (e: Exception) {
                // NavController 可能已经销毁，忽略异常
                Log.w(TAG, "Failed to remove navigation listener", e)
            }
            navigationListener = null
        }
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
        // ViewPager2 的 FragmentStateAdapter 使用 "f" + getItemId(position) 作为 Fragment tag
        // 由于我们重写了 getItemId，需要使用 adapter 的 getItemId 来获取正确的 tag
        val itemId = adapter?.getItemId(position) ?: position.toLong()
        val currentFragmentTag = "f$itemId"
        val currentFragment = childFragmentManager.findFragmentByTag(currentFragmentTag)

        // 从 adapter 获取当前 position 应该显示的 videoId（考虑无限循环模式）
        val expectedVideo = adapter?.getVideoAt(position)
        val expectedVideoId = expectedVideo?.id

        // 验证当前 Fragment 是否匹配预期的 videoId
        // 注意：videoItem 和 imagePost 是 private，通过 arguments 获取
        val currentFragmentVideoId = when (currentFragment) {
            is VideoItemFragment -> {
                val args = currentFragment.arguments
                val item = args?.let {
                    androidx.core.os.BundleCompat.getParcelable(it, "video_item", com.ucw.beatu.shared.common.model.VideoItem::class.java)
                }
                item?.id
            }
            is ImagePostFragment -> {
                val args = currentFragment.arguments
                val item = args?.let {
                    androidx.core.os.BundleCompat.getParcelable(it, "image_post_item", com.ucw.beatu.shared.common.model.VideoItem::class.java)
                }
                item?.id
            }
            else -> null
        }

        // 如果 Fragment 不匹配预期的 videoId，说明 ViewPager2 还没有更新 Fragment
        // 这种情况在刷新后很常见，需要等待 ViewPager2 更新
        if (expectedVideoId != null && currentFragmentVideoId != null && currentFragmentVideoId != expectedVideoId) {
            Log.w(TAG, "handlePageSelected: Fragment mismatch at position $position - expected=$expectedVideoId, actual=$currentFragmentVideoId, waiting for ViewPager2 to update")
            // 不处理，等待 ViewPager2 更新 Fragment
            return
        }

        // 将 adapter 赋值给局部变量，避免智能转换问题
        val currentAdapter = adapter

        childFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is VideoItemFragment -> {
                    // 从 arguments 获取 videoId
                    val fragmentArgs = fragment.arguments
                    val fragmentVideoItem = fragmentArgs?.let {
                        androidx.core.os.BundleCompat.getParcelable(it, "video_item", com.ucw.beatu.shared.common.model.VideoItem::class.java)
                    }
                    val fragmentVideoId = fragmentVideoItem?.id

                    // 验证 Fragment 是否匹配当前列表中的视频
                    // 注意：由于我们重写了 getItemId，tag 格式是 "f" + getItemId(position)，而不是 "f" + position
                    // 我们通过检查 Fragment 的 videoId 是否在当前列表中来验证有效性
                    // 如果 Fragment 的 videoId 在当前列表中，且 tag 匹配，则认为有效
                    val isFragmentValid = if (fragmentVideoId != null && currentAdapter != null) {
                        // 检查 videoId 是否在当前列表中（只检查前几个 position，避免无限循环模式下的性能问题）
                        // 使用 adapter 的实际列表大小（通过 getVideoAt 获取，最多检查 100 个 position）
                        val maxCheckPositions = minOf(100, currentAdapter.itemCount)
                        var found = false
                        for (pos in 0 until maxCheckPositions) {
                            val videoAtPos = currentAdapter.getVideoAt(pos)
                            if (videoAtPos?.id == fragmentVideoId) {
                                val itemIdAtPos = currentAdapter.getItemId(pos)
                                val expectedTag = "f$itemIdAtPos"
                                if (fragment.tag == expectedTag) {
                                    found = true
                                    break
                                }
                            }
                        }
                        found
                    } else {
                        true // 如果无法验证，假设有效
                    }

                    if (fragment == currentFragment && fragment.isVisible && isFragmentValid) {
                        Log.d(TAG, "handlePageSelected: play VideoItemFragment tag=${fragment.tag}, videoId=$fragmentVideoId")
                        fragment.checkVisibilityAndPlay()
                    } else {
                        if (!isFragmentValid) {
                            Log.w(TAG, "handlePageSelected: Fragment at tag=${fragment.tag} is invalid (expected videoId in list, actual=$fragmentVideoId), skipping")
                        } else {
                            Log.d(TAG, "handlePageSelected: pause VideoItemFragment tag=${fragment.tag}, videoId=$fragmentVideoId")
                        }
                        fragment.onParentVisibilityChanged(false)
                    }
                }
                is ImagePostFragment -> {
                    // 从 arguments 获取 postId
                    val fragmentArgs = fragment.arguments
                    val fragmentPostItem = fragmentArgs?.let {
                        androidx.core.os.BundleCompat.getParcelable(it, "image_post_item", com.ucw.beatu.shared.common.model.VideoItem::class.java)
                    }
                    val fragmentPostId = fragmentPostItem?.id

                    // 验证 Fragment 是否匹配当前列表中的视频
                    // 注意：由于我们重写了 getItemId，tag 格式是 "f" + getItemId(position)，而不是 "f" + position
                    val isFragmentValid = if (fragmentPostId != null && currentAdapter != null) {
                        // 检查 postId 是否在当前列表中（只检查前几个 position，避免无限循环模式下的性能问题）
                        val maxCheckPositions = minOf(100, currentAdapter.itemCount)
                        var found = false
                        for (pos in 0 until maxCheckPositions) {
                            val videoAtPos = currentAdapter.getVideoAt(pos)
                            if (videoAtPos?.id == fragmentPostId) {
                                val itemIdAtPos = currentAdapter.getItemId(pos)
                                val expectedTag = "f$itemIdAtPos"
                                if (fragment.tag == expectedTag) {
                                    found = true
                                    break
                                }
                            }
                        }
                        found
                    } else {
                        true // 如果无法验证，假设有效
                    }

                    if (fragment == currentFragment && fragment.isVisible && isFragmentValid) {
                        Log.d(TAG, "handlePageSelected: play ImagePostFragment tag=${fragment.tag}, postId=$fragmentPostId")
                        fragment.checkVisibilityAndPlay()
                    } else {
                        if (!isFragmentValid) {
                            Log.w(TAG, "handlePageSelected: Fragment at tag=${fragment.tag} is invalid (expected postId in list, actual=$fragmentPostId), skipping")
                        } else {
                            Log.d(TAG, "handlePageSelected: pause ImagePostFragment tag=${fragment.tag}, postId=$fragmentPostId")
                        }
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
    
    /**
     * 检查屏幕方向并切换到landscape模式
     * 只有当前视频的 orientation 为 LANDSCAPE 时才自动切换
     * ✅ 修复严重问题3：不再维护 isLandscapeMode，直接检查系统配置
     */
    private fun checkOrientationAndSwitch() {
        if (!isAdded) return
        
        val currentPosition = viewPager?.currentItem ?: -1
        if (currentPosition >= 0) {
            // 检查当前视频的 orientation，只有 LANDSCAPE 视频才自动切换
            val currentVideo = adapter?.getVideoAt(currentPosition)
            if (currentVideo?.orientation != com.ucw.beatu.shared.common.model.VideoOrientation.LANDSCAPE) {
                Log.d(TAG, "检测到横屏，但当前视频不是 LANDSCAPE，不自动切换")
                return
            }
            
            // ✅ 修复严重问题1和4：使用 getItemId 获取正确的 tag，并检查 Fragment 是否准备好
            val itemId = adapter?.getItemId(currentPosition) ?: currentPosition.toLong()
            val currentFragmentTag = "f$itemId"
            val currentFragment = childFragmentManager.findFragmentByTag(currentFragmentTag)
            
            if (currentFragment is VideoItemFragment && isFragmentReady(currentFragment)) {
                Log.d(TAG, "检测到横屏，当前视频为 LANDSCAPE，自动切换到landscape模式")
                // 使用post延迟执行，避免阻塞主线程
                view?.post {
                    currentFragment.openLandscapeMode()
                }
            } else {
                Log.w(TAG, "checkOrientationAndSwitch: Fragment 未准备好，tag=$currentFragmentTag, fragment=$currentFragment")
            }
        }
    }
    
    /**
     * ✅ 修复严重问题1和4：统一的、可靠的恢复播放器方法
     * 使用 getItemId 获取正确的 tag，并确保 Fragment 准备好后再恢复
     * 支持延迟重试机制，解决横屏返回时 Fragment 还未 attach 的问题
     */
    private fun restorePlayerSafely(
        sourceVideoId: Long? = null,
        maxRetries: Int = 3,
        retryDelay: Long = 200L,
        currentRetry: Int = 0
    ) {
        if (!isAdded || viewPager == null || adapter == null) {
            Log.w(TAG, "restorePlayerSafely: Fragment 未准备好，跳过恢复")
            return
        }
        
        val currentPosition = viewPager?.currentItem ?: -1
        if (currentPosition < 0) {
            Log.w(TAG, "restorePlayerSafely: currentPosition 无效")
            return
        }
        
        // ✅ 修复：如果提供了 sourceVideoId，验证当前位置的视频是否匹配
        if (sourceVideoId != null) {
            val currentVideo = adapter?.getVideoAt(currentPosition)
            if (currentVideo?.id != sourceVideoId) {
                Log.w(TAG, "restorePlayerSafely: 当前位置的视频ID (${currentVideo?.id}) 与 sourceVideoId ($sourceVideoId) 不匹配，可能需要等待滚动完成")
                // 如果位置不匹配，延迟重试，等待滚动完成
                if (currentRetry < maxRetries) {
                    view?.postDelayed({
                        restorePlayerSafely(sourceVideoId, maxRetries, retryDelay, currentRetry + 1)
                    }, retryDelay)
                    return
                }
            }
        }
        
        // ✅ 修复严重问题1：使用 getItemId 获取正确的 tag，支持无限循环模式
        val itemId = adapter?.getItemId(currentPosition) ?: currentPosition.toLong()
        val currentFragmentTag = "f$itemId"
            val currentFragment = childFragmentManager.findFragmentByTag(currentFragmentTag)
            
        // ✅ 修复严重问题4：检查 Fragment 是否准备好（view 已创建且已 attach）
        if (currentFragment is VideoItemFragment && isFragmentReady(currentFragment)) {
            Log.d(TAG, "restorePlayerSafely: 恢复播放器，position=$currentPosition, tag=$currentFragmentTag, videoId=${sourceVideoId ?: "unknown"}, retry=$currentRetry")
                currentFragment.restorePlayerFromLandscape()
        } else {
            // ✅ 修复严重问题4：Fragment 还未准备好，延迟重试
            if (currentRetry < maxRetries) {
                Log.d(TAG, "restorePlayerSafely: Fragment 未准备好，延迟重试 (${currentRetry + 1}/$maxRetries), tag=$currentFragmentTag")
                view?.postDelayed({
                    restorePlayerSafely(sourceVideoId, maxRetries, retryDelay, currentRetry + 1)
                }, retryDelay)
            } else {
                Log.w(TAG, "restorePlayerSafely: 重试次数用尽，无法恢复播放器，tag=$currentFragmentTag, fragment=$currentFragment")
            }
        }
    }
    
    /**
     * ✅ 修复严重问题4：检查 Fragment 是否准备好（view 已创建且已 attach）
     */
    private fun isFragmentReady(fragment: Fragment): Boolean {
        return fragment.isAdded && fragment.view != null && fragment.isVisible
    }
    
    /**
     * 从用户弹窗返回后恢复播放器
     * ✅ 修复严重问题2：统一使用 restorePlayerSafely()，避免重复逻辑
     */
    private fun restorePlayerFromUserWorksViewer() {
        Log.d(TAG, "restorePlayerFromUserWorksViewer: 从用户弹窗返回，恢复播放器")
        restorePlayerSafely()
    }

    /**
     * 通知进入横屏模式（供 VideoItemFragment 调用）
     * ✅ 修复严重问题3：不再维护 isLandscapeMode 状态，此方法保留用于兼容性
     */
    fun notifyEnterLandscapeMode() {
        Log.d(TAG, "notifyEnterLandscapeMode: 进入横屏模式")
        // 不再维护状态，直接由系统配置判断
    }
    
    /**
     * 通知退出横屏模式（供 VideoItemFragment 或导航监听器调用）
     * ✅ 修复严重问题2：统一使用 restorePlayerSafely() 恢复播放器
     * ✅ 修复：支持根据 sourceVideoId 定位到正确的视频项
     */
    fun notifyExitLandscapeMode(sourceVideoId: Long? = null) {
        Log.d(TAG, "notifyExitLandscapeMode: 退出横屏模式，恢复播放器，sourceVideoId=$sourceVideoId")
        // ✅ 修复严重问题2和4：统一恢复逻辑，使用延迟确保 Fragment 准备好
        view?.postDelayed({
            if (sourceVideoId != null) {
                // ✅ 修复：如果提供了 sourceVideoId，先滚动到对应的视频项
                scrollToVideoById(sourceVideoId)
            }
            restorePlayerSafely(sourceVideoId)
        }, 300) // ✅ 修复方案2：延迟恢复，确保 ViewPager2 已更新
    }
    
    /**
     * ✅ 修复：根据 videoId 滚动到对应的视频项
     */
    private fun scrollToVideoById(videoId: Long) {
        if (viewPager == null || adapter == null) {
            Log.w(TAG, "scrollToVideoById: ViewPager 或 Adapter 为空，跳过滚动")
            return
        }
        
        val currentPosition = viewPager?.currentItem ?: -1
        if (currentPosition < 0) {
            Log.w(TAG, "scrollToVideoById: currentPosition 无效，跳过滚动")
            return
        }
        
        // 查找包含该 videoId 的位置
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) {
            Log.w(TAG, "scrollToVideoById: itemCount 为 0，跳过滚动")
            return
        }
        
        // 在无限循环模式下，同一个视频可能出现在多个位置
        // 我们需要找到最接近当前位置的位置
        var targetPosition = -1
        var minDistance = Int.MAX_VALUE
        
        // 检查当前位置附近的视频（最多检查前后各 50 个位置）
        val searchRange = 50
        for (offset in -searchRange..searchRange) {
            val checkPosition = currentPosition + offset
            if (checkPosition >= 0 && checkPosition < itemCount) {
                val video = adapter?.getVideoAt(checkPosition)
                if (video?.id == videoId) {
                    val distance = kotlin.math.abs(offset)
                    if (distance < minDistance) {
                        minDistance = distance
                        targetPosition = checkPosition
                    }
                }
            }
        }
        
        if (targetPosition >= 0 && targetPosition != currentPosition) {
            Log.d(TAG, "scrollToVideoById: 滚动到位置 $targetPosition (当前=$currentPosition, videoId=$videoId)")
            viewPager?.setCurrentItem(targetPosition, false)
        } else if (targetPosition >= 0) {
            Log.d(TAG, "scrollToVideoById: 已经在正确的位置 $targetPosition (videoId=$videoId)")
        } else {
            Log.w(TAG, "scrollToVideoById: 未找到 videoId=$videoId 的视频项，保持当前位置")
        }
    }
    
    /**
     * 设置导航监听，监听从landscape返回和从用户弹窗返回
     * ✅ 修复严重问题2、3、5、7：统一恢复逻辑，移除 isLandscapeMode 依赖，延迟恢复，保存 listener 引用
     */
    private fun setupNavigationListener() {
        val navController = findNavController()

        // 初始化 previousDestinationId 为当前的目标
        previousDestinationId = navController.currentDestination?.id

        // ✅ 修复中度问题7：保存 listener 引用以便清理
        navigationListener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            val feedDestinationId = NavigationHelper.getResourceId(
                requireContext(),
                NavigationIds.FEED
            )
            val userWorksViewerDestinationId = NavigationHelper.getResourceId(
                requireContext(),
                NavigationIds.USER_WORKS_VIEWER
            )

            // ✅ 修复严重问题3：使用系统配置判断是否从横屏返回，不再依赖 isLandscapeMode
            val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            
            val landscapeDestinationId = NavigationHelper.getResourceId(
                requireContext(),
                NavigationIds.LANDSCAPE
            )
            
            // ✅ 修复：添加详细日志，调试导航状态
            Log.d(TAG, "导航监听器: destination.id=${destination.id}, feedDestinationId=$feedDestinationId, isPortrait=$isPortrait, previousDestinationId=$previousDestinationId, landscapeDestinationId=$landscapeDestinationId")
            
            // 当从landscape返回到feed时，恢复播放器
            if (destination.id == feedDestinationId && isPortrait) {
                // ✅ 修复：如果是从横屏返回（previousDestinationId == landscapeDestinationId），从 SharedPreferences 读取 sourceVideoId
                // 注意：previousDestinationId 可能为 null，需要检查
                if (previousDestinationId == landscapeDestinationId) {
                    Log.d(TAG, "从landscape返回到feed，恢复播放器，previousDestinationId=$previousDestinationId")
                    // ✅ 修复：从 SharedPreferences 读取 sourceVideoId
                    val prefs = requireContext().getSharedPreferences("landscape_exit", android.content.Context.MODE_PRIVATE)
                    val sourceVideoId = prefs.getLong("source_video_id", -1L)
                    if (sourceVideoId != -1L) {
                        Log.d(TAG, "从 SharedPreferences 读取 sourceVideoId=$sourceVideoId")
                        // 清除 SharedPreferences，避免下次误用
                        prefs.edit().remove("source_video_id").apply()
                        notifyExitLandscapeMode(sourceVideoId)
                    } else {
                        Log.w(TAG, "未找到 sourceVideoId，使用当前位置恢复")
                        notifyExitLandscapeMode()
                    }
                } else {
                    // ✅ 修复：即使 previousDestinationId 不匹配，也尝试读取 sourceVideoId（可能是导航栈的问题）
                    // 因为从横屏返回时，previousDestinationId 可能还没有更新
                    Log.d(TAG, "previousDestinationId ($previousDestinationId) 与 landscapeDestinationId ($landscapeDestinationId) 不匹配，但仍尝试读取 sourceVideoId")
                    val prefs = requireContext().getSharedPreferences("landscape_exit", android.content.Context.MODE_PRIVATE)
                    val sourceVideoId = prefs.getLong("source_video_id", -1L)
                    if (sourceVideoId != -1L) {
                        Log.d(TAG, "从 SharedPreferences 读取 sourceVideoId=$sourceVideoId（previousDestinationId 不匹配但找到了 sourceVideoId）")
                        // 清除 SharedPreferences，避免下次误用
                        prefs.edit().remove("source_video_id").apply()
                        notifyExitLandscapeMode(sourceVideoId)
                    } else {
                        Log.d(TAG, "未找到 sourceVideoId，使用当前位置恢复")
                        notifyExitLandscapeMode()
                    }
                }
            }

            // ✅ 修复严重问题2和5：当从用户弹窗（USER_WORKS_VIEWER）返回到feed时，恢复播放器
            // 使用延迟恢复，确保 Fragment 已经 attach
            if (destination.id == feedDestinationId && previousDestinationId == userWorksViewerDestinationId) {
                Log.d(TAG, "从用户弹窗返回到feed，恢复播放器，previousDestinationId=$previousDestinationId")
                // ✅ 修复中度问题5：延迟恢复，确保 ViewPager2 已更新
                view?.postDelayed({
                    restorePlayerFromUserWorksViewer()
                }, 200)
            }

            // 记录当前的导航目标，作为下次的前一个目标
            previousDestinationId = destination.id
        }
        
        navController.addOnDestinationChangedListener(navigationListener!!)
    }
}