package com.ucw.beatu.business.videofeed.presentation.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragmentCallback

/**
 * FeedFragment - 只负责视频流的 ViewPager2
 * 顶部导航栏已提升到 MainActivity，由应用层统一管理
 */
class FeedFragment : Fragment(), FeedFragmentCallback {

    companion object {
        private const val TAG = "FeedFragment"
    }

    private lateinit var viewPager: ViewPager2
    private var currentPage = 1 // 默认显示推荐页面（索引1）
    private var recommendFragment: RecommendFragment? = null
    private var fragmentLifecycleCallback: FragmentManager.FragmentLifecycleCallbacks? = null
    
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
        // ✅ 修复：使用 this@FeedFragment 作为 adapter 的宿主，而不是 requireActivity()
        // 这样 Fragment 生命周期会和 FeedFragment 绑定，而不是 Activity
        viewPager.adapter = TabPagerAdapter(this@FeedFragment)
        viewPager.setCurrentItem(currentPage, false) // 默认显示推荐页面
        
        // 设置ViewPager2的监听器
        setupViewPagerListener()
        
        // ✅ 使用 FragmentLifecycleCallbacks 监听 Fragment attach 事件（最可靠的方案）
        setupFragmentLifecycleCallback()
        
        // ✅ 修复：初始化时主动修复找不到 fragment 的问题
        // 防止在 callback 注册之前 Fragment 已经 attach 的情况
        fixRecommendFragmentIfNeeded()
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 取消注册 Fragment 生命周期回调
        fragmentLifecycleCallback?.let {
            childFragmentManager.unregisterFragmentLifecycleCallbacks(it)
            fragmentLifecycleCallback = null
        }
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
        // ✅ 现在可以安全地使用缓存的 recommendFragment，因为它是在 Fragment attach 时设置的
        recommendFragment?.onParentTabVisibilityChanged(isVisible)
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
     * 设置 Fragment 生命周期回调，在 Fragment attach 时捕获 RecommendFragment
     * 这是最可靠的方案，确保 100% 捕获 Fragment 的真实实例
     */
    private fun setupFragmentLifecycleCallback() {
        fragmentLifecycleCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentAttached(
                fm: FragmentManager,
                f: Fragment,
                context: Context
            ) {
                super.onFragmentAttached(fm, f, context)
                // ✅ 修复：验证 Fragment 属于当前 FeedFragment，避免捕获到旧的 Fragment
                // 在屏幕旋转时，可能先 attach 新 Fragment，再逐步 detach 旧 Fragment
                // 必须验证 parentFragment 确保是当前 FeedFragment 的子 Fragment
                if (f is RecommendFragment && f.parentFragment == this@FeedFragment) {
                    Log.d(TAG, "onFragmentAttached: RecommendFragment attached: $f")
                    recommendFragment = f
                }
            }
        }
        // 注册 Fragment 生命周期回调，递归监听子 Fragment
        childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallback!!, true)
    }
    
    /**
     * 修复找不到 RecommendFragment 的问题
     * 在初始化时主动查找一次，防止在 callback 注册之前 Fragment 已经 attach 的情况
     */
    private fun fixRecommendFragmentIfNeeded() {
        if (recommendFragment == null) {
            val fragment = childFragmentManager.fragments
                .filterIsInstance<RecommendFragment>()
                .firstOrNull { it.isAdded && it.parentFragment == this@FeedFragment }
            if (fragment != null) {
                recommendFragment = fragment
                Log.d(TAG, "fixRecommendFragmentIfNeeded: restored recommendFragment")
            }
        }
    }
    
    /**
     * 刷新推荐页面
     * 现在可以安全地使用缓存的 recommendFragment，因为它是在 Fragment attach 时设置的
     */
    fun refreshRecommendFragment() {
        if (recommendFragment != null) {
            Log.d(TAG, "refreshRecommendFragment: calling refreshVideoList")
            recommendFragment?.refreshVideoList()
        } else {
            Log.w(TAG, "refreshRecommendFragment: RecommendFragment not found")
        }
    }
    
    /**
     * ViewPager2的适配器
     * ✅ 修复：使用 FeedFragment 作为宿主，而不是 FragmentActivity
     * 这样 Fragment 生命周期会和 FeedFragment 绑定，确保在 onDestroyView 时 adapter 也会销毁
     */
    private inner class TabPagerAdapter(parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {
        override fun getItemCount(): Int = 2 // 关注和推荐两个页面
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FollowFragment()
                1 -> RecommendFragment()
                // ✅ 注意：不再在这里设置 recommendFragment，而是通过 FragmentLifecycleCallbacks 在 attach 时设置
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
        fun hideRecommendText()
        fun showRecommendText()
    }
}
