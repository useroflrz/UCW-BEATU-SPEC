package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.ui.adapter.VideoFeedAdapter
import com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 推荐页面Fragment
 * 显示推荐视频内容，支持无限上下滑动
 */
@AndroidEntryPoint
class RecommendFragment : Fragment() {

    companion object {
        private const val TAG = "RecommendFragment"
    }

    private val viewModel: RecommendViewModel by viewModels()
    
    private var viewPager: ViewPager2? = null
    private var adapter: VideoFeedAdapter? = null
    private var isRefreshing = false
    
    // 左滑手势检测
    private var gestureDetector: GestureDetectorCompat? = null
    private var rootView: View? = null

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
        rootView = inflater.inflate(R.layout.fragment_recommend, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: View created")
        
        // 初始化 ViewPager2
        viewPager = view.findViewById(R.id.viewpager_video_feed)
        viewPager?.let { vp ->
            // 设置垂直方向
            vp.orientation = ViewPager2.ORIENTATION_VERTICAL
            // 设置预加载页面数量（当前页面的前后各1页）
            vp.offscreenPageLimit = 1
            
            // 创建 Adapter
            adapter = VideoFeedAdapter(requireActivity())
            vp.adapter = adapter
            
            // 设置页面切换监听
            vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    Log.d(TAG, "Page selected: $position")
                    
                    // 如果滑动到最后一个视频，加载更多
                    adapter?.let { adapter ->
                        if (position >= adapter.itemCount - 2) {
                            viewModel.loadMoreVideos()
                        }
                    }
                }
            })
            
            // 设置触摸监听，检测下拉刷新
            setupPullToRefresh(vp)
        }
        
        // 设置左滑手势检测
        setupSwipeLeftGesture()
        
        // 观察 ViewModel 状态
        observeViewModel()
    }
    
    /**
     * 设置下拉刷新功能
     * 当在第一个视频时，向下滑动触发刷新
     * 注意：ViewPager2 的垂直滑动和下拉刷新可能有冲突，这里使用简单的触摸检测
     */
    private fun setupPullToRefresh(viewPager: ViewPager2) {
        var touchStartY = 0f
        var hasTriggeredRefresh = false
        
        viewPager.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.y
                    hasTriggeredRefresh = false
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    // 只在第一个视频时检测下拉
                    if (viewPager.currentItem == 0 && !isRefreshing && !hasTriggeredRefresh) {
                        val deltaY = event.y - touchStartY
                        // 向下滑动超过阈值（100dp）
                        if (deltaY > 100 * resources.displayMetrics.density) {
                            hasTriggeredRefresh = true
                            isRefreshing = true
                            viewModel.refreshVideoList()
                            Log.d(TAG, "Trigger pull to refresh (deltaY: $deltaY)")
                        }
                    }
                }
            }
            false // 不拦截事件，让 ViewPager2 正常处理滑动
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 更新视频列表
                    if (state.videoList.isNotEmpty()) {
                        adapter?.updateVideoList(state.videoList)
                        
                        // 如果是刷新完成，重置刷新状态
                        if (isRefreshing && !state.isRefreshing) {
                            isRefreshing = false
                            // 刷新后回到第一个视频
                            viewPager?.setCurrentItem(0, false)
                        }
                    }
                    
                    // 处理错误
                    state.error?.let { error ->
                        Log.e(TAG, "Error: $error")
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // ViewPager2 中的 Fragment 会自己处理生命周期
    }
    
    override fun onResume() {
        super.onResume()
        // ViewPager2 中的 Fragment 会自己处理生命周期
    }
    
    /**
     * 设置左滑手势检测
     * 当用户在推荐页左滑时，跳转到个人主页
     * 注意：需要与 ViewPager2 的垂直滑动区分开
     */
    private fun setupSwipeLeftGesture() {
        rootView?.let { view ->
            gestureDetector = GestureDetectorCompat(
                requireContext(),
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (e1 == null || e2 == null) return false
                        
                        val deltaX = e2.x - e1.x
                        val deltaY = e2.y - e1.y
                        val absDeltaX = kotlin.math.abs(deltaX)
                        val absDeltaY = kotlin.math.abs(deltaY)
                        
                        // 判断是否为左滑：
                        // 1. 水平位移明显大于垂直位移（至少2倍）
                        // 2. 向左滑动（deltaX < 0）
                        // 3. 水平位移超过阈值（100dp）
                        // 4. 水平速度足够快（velocityX < -1000）
                        val minHorizontalDistance = 100 * resources.displayMetrics.density
                        if (absDeltaX > absDeltaY * 2 && 
                            deltaX < 0 && 
                            absDeltaX > minHorizontalDistance &&
                            velocityX < -1000) {
                            Log.d(TAG, "Left swipe detected, navigating to user profile")
                            navigateToUserProfile()
                            return true
                        }
                        
                        return false
                    }
                }
            )
            
            // 在根视图上设置触摸监听
            // 注意：返回 false 表示不拦截事件，让 ViewPager2 正常处理垂直滑动
            view.setOnTouchListener { _, event ->
                val handled = gestureDetector?.onTouchEvent(event) ?: false
                // 如果手势检测器处理了事件（左滑），返回 true 拦截事件
                // 否则返回 false，让 ViewPager2 处理垂直滑动
                handled
            }
        }
    }
    
    /**
     * 导航到用户主页
     */
    private fun navigateToUserProfile() {
        try {
            val navController = findNavController()
            NavigationHelper.navigateByStringId(
                navController,
                NavigationIds.ACTION_FEED_TO_USER_PROFILE,
                requireContext()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to user profile", e)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
        adapter = null
        gestureDetector = null
        rootView = null
    }
}






