package com.ucw.beatu.business.videofeed.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.business.videofeed.presentation.ui.widget.TabIndicatorView
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds

class FeedFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private var currentPage = 1 // 默认显示推荐页面（索引1）
    
    // 指示器相关
    private var tabIndicator: TabIndicatorView? = null
    
    // Tab位置信息（相对于指示器View的坐标）
    private var followTabCenterX: Float = 0f
    private var followTabCenterY: Float = 0f
    private var recommendTabCenterX: Float = 0f
    private var recommendTabCenterY: Float = 0f
    private var meTabCenterX: Float = 0f
    private var meTabCenterY: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化指示器
        tabIndicator = view.findViewById(R.id.tab_indicator)
        
        // 初始化ViewPager2
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = TabPagerAdapter(requireActivity())
        viewPager.setCurrentItem(currentPage, false) // 默认显示推荐页面
        
        // 等待布局完成后再计算Tab位置
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                calculateTabPositions()
                setupViewPagerListener()
                // 初始化指示器位置
                updateTabSelection(currentPage)
            }
        })
        
        // 设置顶部导航栏点击事件
        view.findViewById<View>(R.id.btn_follow).setOnClickListener {
            viewPager.setCurrentItem(0, true)
        }
        
        view.findViewById<View>(R.id.btn_recommend).setOnClickListener {
            viewPager.setCurrentItem(1, true)
        }
        
        view.findViewById<View>(R.id.btn_me).setOnClickListener {
            // 使用 Navigation Graph 跳转到用户主页
            // 通过字符串 ID 进行导航，因为 Navigation Graph 在 app 模块，而 Fragment 在 business 模块
            NavigationHelper.navigateByStringId(
                findNavController(),
                NavigationIds.ACTION_FEED_TO_USER_PROFILE,
                requireContext()
            )
        }
        
        view.findViewById<View>(R.id.iv_search).setOnClickListener {
            // 使用 Navigation Graph 跳转到搜索页面
            NavigationHelper.navigateByStringId(
                findNavController(),
                NavigationIds.ACTION_FEED_TO_SEARCH,
                requireContext()
            )
        }
    }
    
    /**
     * 计算各个Tab的位置（相对于指示器View）
     * X坐标：文字中心
     * Y坐标：文字底部下方（正下方）
     */
    private fun calculateTabPositions() {
        val btnFollow = view?.findViewById<android.widget.TextView>(R.id.btn_follow)
        val btnRecommend = view?.findViewById<android.widget.TextView>(R.id.btn_recommend)
        val btnMe = view?.findViewById<android.widget.TextView>(R.id.btn_me)
        val tabIndicator = view?.findViewById<TabIndicatorView>(R.id.tab_indicator)
        
        // 指示器距离文字底部的间距（dp转px）
        val spacingFromBottom = 4f.dpToPx()
        
        tabIndicator?.let { indicator ->
            val indicatorLocation = IntArray(2)
            indicator.getLocationInWindow(indicatorLocation)
            
            // 计算TextView的位置（相对于指示器View）
            btnFollow?.let {
                val location = IntArray(2)
                it.getLocationInWindow(location)
                // X坐标：文字中心
                followTabCenterX = (location[0] - indicatorLocation[0]) + it.width / 2f
                // Y坐标：文字底部 + 间距
                followTabCenterY = (location[1] - indicatorLocation[1]) + it.height + spacingFromBottom
            }
            
            btnRecommend?.let {
                val location = IntArray(2)
                it.getLocationInWindow(location)
                // X坐标：文字中心
                recommendTabCenterX = (location[0] - indicatorLocation[0]) + it.width / 2f
                // Y坐标：文字底部 + 间距
                recommendTabCenterY = (location[1] - indicatorLocation[1]) + it.height + spacingFromBottom
            }
            
            btnMe?.let {
                val location = IntArray(2)
                it.getLocationInWindow(location)
                // X坐标：文字中心
                meTabCenterX = (location[0] - indicatorLocation[0]) + it.width / 2f
                // Y坐标：文字底部 + 间距
                meTabCenterY = (location[1] - indicatorLocation[1]) + it.height + spacingFromBottom
            }
            
            // 将坐标传递给指示器
            indicator.setTabPositions(
                followTabCenterX, followTabCenterY,
                recommendTabCenterX, recommendTabCenterY,
                meTabCenterX, meTabCenterY
            )
        }
    }
    
    /**
     * dp转px的扩展函数
     */
    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
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
                updateIndicatorOnScroll(position, positionOffset)
            }
            
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabSelection(position)
                currentPage = position
            }
        })
    }
    
    /**
     * 根据滑动进度更新指示器
     * ViewPager2的onPageScrolled中：
     * - position: 当前页面索引（起点）
     * - offset: 滑动到下一个页面的进度（0到1）
     * 
     * 逻辑：
     * - 起点和终点都是圆点（progress=0）
     * - 中点位置拉长到最大（progress=1）
     * - 使用sin函数实现平滑的抛物线效果
     */
    private fun updateIndicatorOnScroll(position: Int, offset: Float) {
        // 位置进度：使用线性插值，让位置平滑移动
        val positionProgress = offset
        
        // 宽度进度：使用sin函数，让长度在滑动中途达到最大
        // sin(offset * π)：在offset=0和1时widthProgress=0（圆点），在offset=0.5时widthProgress=1（最大拉长）
        val widthProgress = kotlin.math.sin(offset * kotlin.math.PI).toFloat()
        
        when (position) {
            0 -> {
                // 从关注（position=0，起点）滑动到推荐（position=1，终点）
                tabIndicator?.setScrollProgress(
                    positionProgress, widthProgress,
                    followTabCenterX, followTabCenterY,
                    recommendTabCenterX, recommendTabCenterY
                )
            }
            1 -> {
                // 从推荐（position=1，起点）滑动到关注（position=0，终点，反向）
                tabIndicator?.setScrollProgress(
                    positionProgress, widthProgress,
                    recommendTabCenterX, recommendTabCenterY,
                    followTabCenterX, followTabCenterY
                )
            }
        }
    }
    
    /**
     * 更新顶部菜单栏的选中状态
     * @param position 当前选中的页面索引（0=关注，1=推荐）
     */
    private fun updateTabSelection(position: Int) {
        val btnFollow = view?.findViewById<android.widget.TextView>(R.id.btn_follow)
        val btnRecommend = view?.findViewById<android.widget.TextView>(R.id.btn_recommend)
        
        when (position) {
            0 -> {
                // 选中关注
                btnFollow?.apply {
                    setTextColor(0xFFFFFFFF.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                btnRecommend?.apply {
                    setTextColor(0x99FFFFFF.toInt())
                    setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                // 指示器移动到关注位置并聚合成圆点
                tabIndicator?.moveToTab(0)
            }
            1 -> {
                // 选中推荐
                btnFollow?.apply {
                    setTextColor(0x99FFFFFF.toInt())
                    setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                btnRecommend?.apply {
                    setTextColor(0xFFFFFFFF.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                // 指示器移动到推荐位置并聚合成圆点
                tabIndicator?.moveToTab(1)
            }
        }
    }
    
    /**
     * dp转px的扩展函数
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
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
}