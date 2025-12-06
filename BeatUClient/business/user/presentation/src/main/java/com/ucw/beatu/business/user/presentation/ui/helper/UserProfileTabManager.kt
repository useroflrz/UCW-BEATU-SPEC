package com.ucw.beatu.business.user.presentation.ui.helper

import android.view.View
import android.widget.TextView
import com.ucw.beatu.business.user.presentation.R
import com.ucw.beatu.business.user.presentation.viewmodel.UserProfileViewModel
import com.ucw.beatu.shared.designsystem.util.IOSButtonEffect
import com.ucw.beatu.shared.designsystem.widget.TabIndicatorView

/**
 * 管理用户主页的 Tab 切换逻辑
 */
class UserProfileTabManager(
    private val view: View,
    private val viewModel: UserProfileViewModel,
    private val isReadOnly: Boolean,
    private val onTabSwitched: (UserProfileViewModel.TabType, String, String) -> Unit
) {
    private lateinit var tabWorks: TextView
    private lateinit var tabCollections: TextView
    private lateinit var tabLikes: TextView
    private lateinit var tabHistory: TextView
    private var tabIndicator: TabIndicatorView? = null
    
    // 标签容器
    private var tabsContainer: android.view.View? = null

    // Tab位置坐标
    private var worksTabX: Float = 0f
    private var worksTabY: Float = 0f
    private var collectionsTabX: Float = 0f
    private var collectionsTabY: Float = 0f
    private var likesTabX: Float = 0f
    private var likesTabY: Float = 0f
    private var historyTabX: Float = 0f
    private var historyTabY: Float = 0f

    // 当前选中的标签
    private var selectedTab: TextView? = null
    private var currentTabIndex = 0 // 0=作品, 1=收藏, 2=点赞, 3=历史

    /**
     * 初始化标签
     */
    fun initTabs() {
        tabWorks = view.findViewById(R.id.tab_works)
        tabCollections = view.findViewById(R.id.tab_collections)
        tabLikes = view.findViewById(R.id.tab_likes)
        tabHistory = view.findViewById(R.id.tab_history)
        tabsContainer = view.findViewById(com.ucw.beatu.business.user.presentation.R.id.tabs_container)
//        tabIndicator = view.findViewById(R.id.tab_indicator)

        // 给标签文字添加阴影，提高在透明背景上的可见性
        val shadowRadius = 2f
        val shadowDx = 0f
        val shadowDy = 1f
        val shadowColor = 0x80000000.toInt() // 半透明黑色阴影
        tabWorks.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        tabCollections.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        tabLikes.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        tabHistory.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)

        // 只读模式（弹窗）下，隐藏整个标签容器，不占用任何空间
        if (isReadOnly) {
            tabsContainer?.visibility = View.GONE
            tabWorks.visibility = View.GONE
            tabCollections.visibility = View.GONE
            tabLikes.visibility = View.GONE
            tabHistory.visibility = View.GONE
            tabIndicator?.visibility = View.GONE
        } else {
            // 非只读模式下，显式设置其他标签为未选中状态
            updateTabState(tabCollections, false)
            updateTabState(tabLikes, false)
            updateTabState(tabHistory, false)

            // 计算Tab位置（在布局完成后）
            tabIndicator?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    tabIndicator?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    calculateTabPositions()
                }
            })
        }

        // 默认选中"作品"
        selectedTab = tabWorks
        currentTabIndex = 0
        updateTabState(tabWorks, true)

        // 在布局完成后移动指示器到初始位置
        if (!isReadOnly) {
            tabIndicator?.post {
                tabIndicator?.moveToTab(0)
            }
        }

        // 设置点击监听（只读模式下，其它标签不可见，不会被点击）
        IOSButtonEffect.applyIOSEffect(tabWorks) { switchTab(tabWorks, UserProfileViewModel.TabType.WORKS) }
        IOSButtonEffect.applyIOSEffect(tabCollections) { switchTab(tabCollections, UserProfileViewModel.TabType.COLLECTIONS) }
        IOSButtonEffect.applyIOSEffect(tabLikes) { switchTab(tabLikes, UserProfileViewModel.TabType.LIKES) }
        IOSButtonEffect.applyIOSEffect(tabHistory) { switchTab(tabHistory, UserProfileViewModel.TabType.HISTORY) }
    }

    /**
     * 计算各个Tab的位置（相对于指示器View）
     */
    private fun calculateTabPositions() {
        val indicator = tabIndicator ?: return
        val indicatorLocation = IntArray(2)
        indicator.getLocationInWindow(indicatorLocation)

        val spacingFromBottom = 8f.dpToPx()

        val worksLocation = IntArray(2)
        tabWorks.getLocationInWindow(worksLocation)
        worksTabX = (worksLocation[0] - indicatorLocation[0]) + tabWorks.width / 2f
        worksTabY = (worksLocation[1] - indicatorLocation[1]) + tabWorks.height + spacingFromBottom

        val collectionsLocation = IntArray(2)
        tabCollections.getLocationInWindow(collectionsLocation)
        collectionsTabX = (collectionsLocation[0] - indicatorLocation[0]) + tabCollections.width / 2f
        collectionsTabY = (collectionsLocation[1] - indicatorLocation[1]) + tabCollections.height + spacingFromBottom

        val likesLocation = IntArray(2)
        tabLikes.getLocationInWindow(likesLocation)
        likesTabX = (likesLocation[0] - indicatorLocation[0]) + tabLikes.width / 2f
        likesTabY = (likesLocation[1] - indicatorLocation[1]) + tabLikes.height + spacingFromBottom

        val historyLocation = IntArray(2)
        tabHistory.getLocationInWindow(historyLocation)
        historyTabX = (historyLocation[0] - indicatorLocation[0]) + tabHistory.width / 2f
        historyTabY = (historyLocation[1] - indicatorLocation[1]) + tabHistory.height + spacingFromBottom

        // 将坐标传递给指示器（使用多Tab版本）
        indicator.setTabPositions(
            worksTabX to worksTabY,
            collectionsTabX to collectionsTabY,
            likesTabX to likesTabY,
            historyTabX to historyTabY
        )

        // 移动到当前选中的Tab
        indicator.moveToTab(currentTabIndex)
    }

    private fun Float.dpToPx(): Float {
        return this * view.context.resources.displayMetrics.density
    }

    /**
     * 切换标签
     */
    private fun switchTab(tab: TextView, tabType: UserProfileViewModel.TabType) {
        if (selectedTab == tab) return

        // 更新之前选中的标签
        selectedTab?.let { updateTabState(it, false) }

        // 更新新选中的标签
        selectedTab = tab
        currentTabIndex = when (tab) {
            tabWorks -> 0
            tabCollections -> 1
            tabLikes -> 2
            tabHistory -> 3
            else -> 0
        }
        updateTabState(tab, true)

        // 移动指示器到新Tab位置
        tabIndicator?.moveToTab(currentTabIndex)

        // 通知外部处理Tab切换
        onTabSwitched(tabType, "", "")
    }

    /**
     * 更新标签状态（使用主页导航栏样式）
     */
    private fun updateTabState(tab: TextView, isSelected: Boolean) {
        if (isSelected) {
            tab.setTextColor(0xFFFFFFFF.toInt())       // 白色
            tab.typeface = android.graphics.Typeface.DEFAULT_BOLD // 粗体
        } else {
            tab.setTextColor(0x99FFFFFF.toInt())       // 白色 60% 透明（#99FFFFFF）
            tab.typeface = android.graphics.Typeface.DEFAULT // 正常字体
        }
        // 移除背景色，使用透明背景
        tab.setBackgroundColor(0x00000000.toInt())
    }

    /**
     * 应用只读模式下的标签文本颜色
     */
    fun applyReadOnlyTabColors() {
        tabWorks.setTextColor(android.graphics.Color.WHITE)
        tabWorks.typeface = android.graphics.Typeface.DEFAULT_BOLD
        tabCollections.setTextColor(android.graphics.Color.parseColor("#99FFFFFF"))
        tabCollections.typeface = android.graphics.Typeface.DEFAULT
        tabLikes.setTextColor(android.graphics.Color.parseColor("#99FFFFFF"))
        tabLikes.typeface = android.graphics.Typeface.DEFAULT
        tabHistory.setTextColor(android.graphics.Color.parseColor("#99FFFFFF"))
        tabHistory.typeface = android.graphics.Typeface.DEFAULT
    }
}

