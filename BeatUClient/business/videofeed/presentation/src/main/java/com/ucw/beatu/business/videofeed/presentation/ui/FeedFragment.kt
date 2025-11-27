package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragmentCallback

/**
 * FeedFragment - 只负责视频流的 ViewPager2
 * 顶部导航栏已提升到 MainActivity，由应用层统一管理
 */
class FeedFragment : Fragment(), FeedFragmentCallback {

    private lateinit var viewPager: ViewPager2
    private var currentPage = 1 // 默认显示推荐页面（索引1）
    private var recommendFragment: RecommendFragment? = null
    
    // MainActivity 引用（用于更新指示器）
    private var mainActivity: MainActivityBridge? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取 MainActivity 引用
        mainActivity = activity as? MainActivityBridge
        
        // 初始化ViewPager2
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = TabPagerAdapter(requireActivity())
        viewPager.setCurrentItem(currentPage, false) // 默认显示推荐页面
        
        // 设置ViewPager2的监听器
        setupViewPagerListener()
    }
    
    override fun onResume() {
        super.onResume()
        // 当 FeedFragment 恢复时，恢复当前可见的视频
        resumeVisibleVideo()
    }
    
    override fun onPause() {
        super.onPause()
        // 当 FeedFragment 暂停时（比如切换到其他页面），暂停所有视频
        pauseAllVideos()
    }
    
    /**
     * 设置ViewPager2的监听器
     */
    private fun setupViewPagerListener() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                // 通知 MainActivity 更新指示器
                mainActivity?.onIndicatorScrollProgress(
                    position,
                    positionOffset,
                    // 这里需要从 MainActivity 获取 Tab 位置，暂时传 0
                    // 实际实现中，MainActivity 会自己计算位置
                    0f, 0f, 0f, 0f
                )
            }
            
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                // 通知 MainActivity 更新 Tab 选中状态
                mainActivity?.updateTabSelection(position)
                notifyRecommendVisibility(position == 1)
            }
        })
    }

    private fun notifyRecommendVisibility(isVisible: Boolean) {
        val target = recommendFragment ?: childFragmentManager.fragments
            .filterIsInstance<RecommendFragment>()
            .firstOrNull()
            ?.also { recommendFragment = it }
        target?.onParentTabVisibilityChanged(isVisible)
    }
    
    /**
     * FeedFragmentCallback 接口实现
     */
    override fun switchToTab(position: Int) {
        if (::viewPager.isInitialized) {
            viewPager.setCurrentItem(position, true)
        }
    }
    
    override fun getCurrentTabPosition(): Int {
        return currentPage
    }
    
    override fun onIndicatorScrollProgress(
        position: Int,
        positionOffset: Float,
        followTabX: Float,
        followTabY: Float,
        recommendTabX: Float,
        recommendTabY: Float
    ) {
        // 这个方法由 MainActivity 调用，但实际更新逻辑在 MainActivity 中
        // 这里不需要实现，因为指示器在 MainActivity 中
    }
    
    override fun onIndicatorTabSelected(position: Int) {
        // 这个方法由 MainActivity 调用，但实际更新逻辑在 MainActivity 中
        // 这里不需要实现，因为指示器在 MainActivity 中
    }
    
    /**
     * 暂停所有视频（当导航到其他页面时调用）
     */
    fun pauseAllVideos() {
        // 暂停推荐页面的所有视频
        recommendFragment?.pauseAllVideoItems()
        // TODO: 当 FollowFragment 实现视频播放功能后，也需要暂停关注页面的视频
    }
    
    /**
     * 恢复当前可见的视频（当返回 Feed 页面时调用）
     */
    fun resumeVisibleVideo() {
        // 恢复当前 Tab 的视频
        if (currentPage == 1) {
            // 推荐页面
            recommendFragment?.resumeVisibleVideoItem()
        }
        // TODO: 当 FollowFragment 实现视频播放功能后，也需要恢复关注页面的视频
    }
    
    /**
     * ViewPager2的适配器
     */
    private inner class TabPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2 // 关注和推荐两个页面
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FollowFragment()
                1 -> RecommendFragment().also { recommendFragment = it }
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
    
    /**
     * MainActivity 桥接接口
     * 用于 FeedFragment 与 MainActivity 之间的通信
     */
    interface MainActivityBridge {
        fun updateTabSelection(position: Int)
        fun onIndicatorScrollProgress(
            position: Int,
            positionOffset: Float,
            followTabX: Float,
            followTabY: Float,
            recommendTabX: Float,
            recommendTabY: Float
        )
    }
}
