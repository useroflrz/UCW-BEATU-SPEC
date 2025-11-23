package com.ucw.beatu.business.videofeed.presentation.ui

/**
 * FeedFragment 回调接口
 * 用于 MainActivity 与 FeedFragment 之间的通信
 */
interface FeedFragmentCallback {
    /**
     * 切换到指定的 Tab（0=关注，1=推荐）
     */
    fun switchToTab(position: Int)
    
    /**
     * 获取当前选中的 Tab 位置
     */
    fun getCurrentTabPosition(): Int
    
    /**
     * 设置指示器滚动进度回调
     * 当 ViewPager2 滑动时，FeedFragment 会调用此方法通知 MainActivity 更新指示器
     */
    fun onIndicatorScrollProgress(
        position: Int,
        positionOffset: Float,
        followTabX: Float,
        followTabY: Float,
        recommendTabX: Float,
        recommendTabY: Float
    )
    
    /**
     * 设置指示器移动到指定 Tab 的回调
     * 当 ViewPager2 切换完成时，FeedFragment 会调用此方法通知 MainActivity 更新指示器
     */
    fun onIndicatorTabSelected(position: Int)
}

