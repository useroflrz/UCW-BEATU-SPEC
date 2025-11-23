package com.ucw.beatu

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.navigation.fragment.NavHostFragment
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragment
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragmentCallback
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragment.MainActivityBridge
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.designsystem.widget.TabIndicatorView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainActivityBridge {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // 导航栏相关
    private var topNavigation: View? = null
    private var btnFollow: TextView? = null
    private var btnRecommend: TextView? = null
    private var btnMe: TextView? = null
    private var ivSearch: ImageView? = null
    private var tabIndicator: TabIndicatorView? = null
    
    // FeedFragment 回调
    private var feedFragmentCallback: FeedFragmentCallback? = null
    
    // Tab位置信息（相对于指示器View的坐标）
    private var followTabCenterX: Float = 0f
    private var followTabCenterY: Float = 0f
    private var recommendTabCenterX: Float = 0f
    private var recommendTabCenterY: Float = 0f
    private var meTabCenterX: Float = 0f
    private var meTabCenterY: Float = 0f
    
    // 当前选中的Tab（0=关注，1=推荐）
    private var currentTabPosition = 1 // 默认显示推荐页面
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting MainActivity")
        
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)
            Log.d(TAG, "onCreate: Content view set")
            
            // 处理 WindowInsets：让视频内容延伸到状态栏下方，导航栏避开状态栏
            topNavigation = findViewById(R.id.top_navigation)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // 给导航栏添加状态栏高度的 padding top，确保内容不被状态栏遮挡
                // 导航栏总高度 = 状态栏高度 + 56dp（内容高度）
                val contentHeight = (56 * resources.displayMetrics.density).toInt()
                topNavigation?.setPadding(
                    topNavigation?.paddingLeft ?: 0,
                    systemBars.top,
                    topNavigation?.paddingRight ?: 0,
                    topNavigation?.paddingBottom ?: 0
                )
                // 动态设置导航栏高度，确保内容区域有足够的空间
                topNavigation?.layoutParams?.height = systemBars.top + contentHeight
                topNavigation?.requestLayout()
                insets
            }
            
            // 初始化导航栏组件
            initTopNavigation()
            
            // 等待导航栏布局完成后再计算Tab位置
            topNavigation?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    topNavigation?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    calculateTabPositions()
                    // 初始化指示器位置
                    updateTabSelection(currentTabPosition)
                }
            })
            
            // 显示 FeedFragment
            if (savedInstanceState == null) {
                Log.d(TAG, "onCreate: Adding FeedFragment")
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, FeedFragment())
                }
                Log.d(TAG, "onCreate: FeedFragment added")
            } else {
                Log.d(TAG, "onCreate: Restoring from saved state")
            }
            
            // 注册 Fragment 生命周期回调，用于获取 FeedFragment 实例
            supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(
                        fm: androidx.fragment.app.FragmentManager,
                        f: Fragment,
                        v: View,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState)
                        if (f is FeedFragment) {
                            feedFragmentCallback = f
                        }
                    }
                },
                true
            )
            
            Log.d(TAG, "onCreate: Completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error occurred", e)
            throw e
        }
    }
    
    /**
     * 初始化顶部导航栏
     */
    private fun initTopNavigation() {
        btnFollow = findViewById(R.id.btn_follow)
        btnRecommend = findViewById(R.id.btn_recommend)
        btnMe = findViewById(R.id.btn_me)
        ivSearch = findViewById(R.id.iv_search)
        tabIndicator = findViewById(R.id.tab_indicator)
        
        // 给导航栏文字添加阴影，提高在透明背景上的可见性
        val shadowRadius = 2f
        val shadowDx = 0f
        val shadowDy = 1f
        val shadowColor = 0x80000000.toInt() // 半透明黑色阴影
        
        btnFollow?.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        btnRecommend?.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        btnMe?.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        
        // 设置点击事件
        btnFollow?.setOnClickListener {
            feedFragmentCallback?.switchToTab(0)
        }
        
        btnRecommend?.setOnClickListener {
            feedFragmentCallback?.switchToTab(1)
        }
        
        btnMe?.setOnClickListener {
            // 使用 Navigation Graph 跳转到用户主页
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
            navHostFragment?.navController?.let { navController ->
                NavigationHelper.navigateByStringId(
                    navController,
                    NavigationIds.ACTION_FEED_TO_USER_PROFILE,
                    this
                )
            }
        }
        
        ivSearch?.setOnClickListener {
            // 使用 Navigation Graph 跳转到搜索页面
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
            navHostFragment?.navController?.let { navController ->
                NavigationHelper.navigateByStringId(
                    navController,
                    NavigationIds.ACTION_FEED_TO_SEARCH,
                    this
                )
            }
        }
    }
    
    /**
     * 计算各个Tab的位置（相对于指示器View）
     * X坐标：文字中心
     * Y坐标：文字底部下方（正下方）
     */
    private fun calculateTabPositions() {
        val tabIndicator = this.tabIndicator ?: return
        
        // 指示器距离文字底部的间距（dp转px）
        val spacingFromBottom = 4f.dpToPx()
        
        val indicatorLocation = IntArray(2)
        tabIndicator.getLocationInWindow(indicatorLocation)
        
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
        tabIndicator.setTabPositions(
            followTabCenterX, followTabCenterY,
            recommendTabCenterX, recommendTabCenterY,
            meTabCenterX, meTabCenterY
        )
    }
    
    /**
     * dp转px的扩展函数
     */
    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }
    
    /**
     * MainActivityBridge 接口实现
     */
    override fun updateTabSelection(position: Int) {
        updateTabSelectionInternal(position)
    }
    
    override fun onIndicatorScrollProgress(
        position: Int,
        positionOffset: Float,
        followTabX: Float,
        followTabY: Float,
        recommendTabX: Float,
        recommendTabY: Float
    ) {
        onIndicatorScrollProgressInternal(position, positionOffset)
    }
    
    /**
     * 更新顶部菜单栏的选中状态
     * @param position 当前选中的页面索引（0=关注，1=推荐）
     */
    private fun updateTabSelectionInternal(position: Int) {
        currentTabPosition = position
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
     * 更新指示器滚动进度（由 FeedFragment 调用）
     */
    private fun onIndicatorScrollProgressInternal(position: Int, positionOffset: Float) {
        // 位置进度：使用线性插值，让位置平滑移动
        val positionProgress = positionOffset
        
        // 宽度进度：使用sin函数，让长度在滑动中途达到最大
        // sin(offset * π)：在offset=0和1时widthProgress=0（圆点），在offset=0.5时widthProgress=1（最大拉长）
        val widthProgress = kotlin.math.sin(positionOffset * kotlin.math.PI).toFloat()
        
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
}