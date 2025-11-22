package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.ui.adapter.VideoFeedAdapter
import com.ucw.beatu.business.videofeed.presentation.viewmodel.RecommendViewModel
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
        return inflater.inflate(R.layout.fragment_recommend, container, false)
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
        adapter = null
    }
}






