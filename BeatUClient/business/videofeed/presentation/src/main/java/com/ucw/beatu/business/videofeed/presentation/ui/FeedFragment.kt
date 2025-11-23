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
            }
        })
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
     * ViewPager2的适配器
     */
    private class TabPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2 // 关注和推荐两个页面
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FollowFragment()
                1 -> RecommendFragment()
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
