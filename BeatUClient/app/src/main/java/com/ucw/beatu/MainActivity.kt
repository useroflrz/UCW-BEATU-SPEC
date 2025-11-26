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
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragment
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragment.MainActivityBridge
import com.ucw.beatu.business.videofeed.presentation.ui.FeedFragmentCallback
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.designsystem.widget.TabIndicatorView
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity 作为应用的入口 Activity
 * 使用 NavHostFragment 作为导航容器，由 Navigation Graph 自动管理 Fragment 栈
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainActivityBridge {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var topNavigation: View? = null
    private var btnFollow: TextView? = null
    private var btnRecommend: TextView? = null
    private var ivMe: ImageView? = null
    private var ivSearch: ImageView? = null
    private var tabIndicator: TabIndicatorView? = null

    private var feedFragmentCallback: FeedFragmentCallback? = null
    private var navController: NavController? = null

    private var followTabCenterX: Float = 0f
    private var followTabCenterY: Float = 0f
    private var recommendTabCenterX: Float = 0f
    private var recommendTabCenterY: Float = 0f

    private var currentTabPosition = 1 // 默认显示推荐页面

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        topNavigation = findViewById(R.id.top_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                view.paddingBottom
            )
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val contentHeight = (56 * resources.displayMetrics.density).toInt()
            topNavigation?.setPadding(
                topNavigation?.paddingLeft ?: 0,
                systemBars.top,
                topNavigation?.paddingRight ?: 0,
                topNavigation?.paddingBottom ?: 0
            )
            topNavigation?.layoutParams?.height = systemBars.top + contentHeight
            topNavigation?.requestLayout()
            insets
        }

        initTopNavigation()
        topNavigation?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                topNavigation?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                calculateTabPositions()
                updateTabSelection(currentTabPosition)
            }
        })

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navController = navHostFragment?.navController
        setupNavigationListener()

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: androidx.fragment.app.FragmentManager,
                    f: Fragment,
                    view: View,
                    savedInstanceState: Bundle?
                ) {
                    super.onFragmentViewCreated(fm, f, view, savedInstanceState)
                    if (f is FeedFragment) {
                        feedFragmentCallback = f
                    }
                }
            },
            true
        )
    }

    /**
     * 初始化顶部导航栏
     */
    private fun initTopNavigation() {
        btnFollow = findViewById(R.id.btn_follow)
        btnRecommend = findViewById(R.id.btn_recommend)
        ivMe = findViewById(R.id.iv_me)
        ivSearch = findViewById(R.id.iv_search)
        tabIndicator = findViewById(R.id.tab_indicator)

        // 给导航栏文字添加阴影，提高在透明背景上的可见性
        val shadowRadius = 2f
        val shadowDx = 0f
        val shadowDy = 1f
        val shadowColor = 0x80000000.toInt() // 半透明黑色阴影

        btnFollow?.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        btnRecommend?.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)

        // 设置点击事件
        btnFollow?.setOnClickListener {
            feedFragmentCallback?.switchToTab(0)
        }

        btnRecommend?.setOnClickListener {
            feedFragmentCallback?.switchToTab(1)
        }

        // 设置"我"图标点击事件
        ivMe?.setOnClickListener {
            navigateToUserProfile()
        }

        ivSearch?.setOnClickListener {
            // 使用 Navigation Graph 跳转到搜索页面
            navigateToSearch()
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

        // 将坐标传递给指示器（只传递关注和推荐两个Tab的位置）
        tabIndicator.setTabPositions(
            followTabCenterX, followTabCenterY,
            recommendTabCenterX, recommendTabCenterY
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

    /**
     * 导航到搜索页面
     */
    private fun navigateToSearch() {
        navController?.let { controller ->
            try {
                NavigationHelper.navigateByStringId(
                    controller,
                    NavigationIds.ACTION_FEED_TO_SEARCH,
                    this
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to navigate to search", e)
            }
        }
    }

    /**
     * 设置导航监听器，用于控制顶部导航栏的显示/隐藏
     */
    private fun setupNavigationListener() {
        navController?.addOnDestinationChangedListener { _, destination, _ ->
            // 根据当前目标页面控制顶部导航栏的显示/隐藏
            when (destination.id) {
                R.id.userProfile,
                R.id.search -> {
                    // 进入个人主页或搜索页时隐藏顶部导航栏
                    hideTopNavigation()
                }
                R.id.feed -> {
                    // 返回 Feed 页面时显示顶部导航栏
                    showTopNavigation()
                }
                else -> {
                    // 其他页面保持当前状态或根据需求调整
                }
            }
        }
    }

    /**
     * 隐藏顶部导航栏（带动画）
     */
    private fun hideTopNavigation() {
        topNavigation?.let { nav ->
            if (nav.visibility == View.VISIBLE) {
                val height = if (nav.height > 0) nav.height else {
                    // 如果高度为0，使用估算值（状态栏高度 + 56dp）
                    val statusBarHeight = resources.getIdentifier(
                        "status_bar_height", "dimen", "android"
                    ).let { if (it > 0) resources.getDimensionPixelSize(it) else 0 }
                    val contentHeight = (56 * resources.displayMetrics.density).toInt()
                    statusBarHeight + contentHeight
                }
                nav.animate()
                    .alpha(0f)
                    .translationY(-height.toFloat())
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        nav.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    /**
     * 显示顶部导航栏（带动画）
     */
    private fun showTopNavigation() {
        topNavigation?.let { nav ->
            if (nav.visibility != View.VISIBLE) {
                val height = if (nav.height > 0) nav.height else {
                    // 如果高度为0，使用估算值（状态栏高度 + 56dp）
                    val statusBarHeight = resources.getIdentifier(
                        "status_bar_height", "dimen", "android"
                    ).let { if (it > 0) resources.getDimensionPixelSize(it) else 0 }
                    val contentHeight = (56 * resources.displayMetrics.density).toInt()
                    statusBarHeight + contentHeight
                }
                nav.visibility = View.VISIBLE
                nav.alpha = 0f
                nav.translationY = -height.toFloat()
                nav.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }
    }

    /**
     * 导航到用户主页
     */
    private fun navigateToUserProfile() {
        navController?.let { controller ->
            try {
                NavigationHelper.navigateByStringId(
                    controller,
                    NavigationIds.ACTION_FEED_TO_USER_PROFILE,
                    this
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to navigate to user profile", e)
            }
        }
    }

    /**
     * 导航到设置页面
     */
    fun navigateToSettings() {
        navController?.let { controller ->
            try {
                NavigationHelper.navigateByStringId(
                    controller,
                    NavigationIds.ACTION_FEED_TO_SETTINGS,
                    this
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to navigate to settings", e)
            }
        }
    }

    /**
     * 导航到横屏页面
     */
    fun navigateToLandscape() {
        navController?.let { controller ->
            try {
                NavigationHelper.navigateByStringId(
                    controller,
                    NavigationIds.ACTION_FEED_TO_LANDSCAPE,
                    this
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to navigate to landscape", e)
            }
        }
    }
}