package com.ucw.beatu.business.videofeed.presentation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.ui.ImagePostFragment
import com.ucw.beatu.business.videofeed.presentation.ui.VideoItemFragment

/**
 * 视频流 Adapter
 * 用于 ViewPager2 显示视频列表
 *
 * 注意：这里必须使用承载 ViewPager2 的 Fragment 作为 FragmentStateAdapter 的宿主，
 * 这样子 Fragment 会挂在该 Fragment 的 childFragmentManager 下，
 * 才能被 RecommendFragment 中通过 childFragmentManager 正确找到并控制可见性/播放状态。
 */
class VideoFeedAdapter(
    parentFragment: Fragment,
    private var videoList: MutableList<VideoItem> = mutableListOf(),
    /**
     * 是否已经把后端所有分页数据加载完：
     * - true：Adapter 以“无限循环”模式工作，getItemCount 返回一个很大的值
     * - false：Adapter 只展示当前已加载的真实数量
     */
    private var hasLoadedAllFromBackend: Boolean = false
) : FragmentStateAdapter(parentFragment) {

    override fun getItemCount(): Int {
        if (videoList.isEmpty()) return 0
        // 当后端所有页都加载完后，通过一个非常大的 itemCount + 取模来实现“无限刷”
        return if (hasLoadedAllFromBackend) {
            Int.MAX_VALUE
        } else {
            videoList.size
        }
    }

    override fun getItemId(position: Int): Long {
        if (videoList.isEmpty()) {
            return position.toLong()
        }
        // ✅ 修复：使用 video.id（稳定的 Numeric ID）作为 itemId
        // 这样即使横屏重建、列表刷新、position 变化，Fragment 也能正确匹配到对应的视频
        // 在无限循环模式下，同一个视频可能出现在多个 position，但 itemId 始终是 video.id
        val safeIndex = position % videoList.size
        val videoItem = videoList[safeIndex]
        return videoItem.id  // 返回稳定的 Numeric ID
    }

    override fun containsItem(itemId: Long): Boolean {
        if (videoList.isEmpty()) {
            return false
        }
        // ✅ 修复：itemId 现在是 video.id，需要检查这个 ID 是否在当前列表中
        return videoList.any { it.id == itemId }
    }

    override fun createFragment(position: Int): Fragment {
        val safeIndex = if (videoList.isEmpty()) 0 else position % videoList.size
        val videoItem = videoList[safeIndex]
        return if (videoItem.type == FeedContentType.IMAGE_POST) {
            ImagePostFragment.newInstance(videoItem)
        } else {
            VideoItemFragment.newInstance(videoItem)
        }
    }

    /**
     * 更新视频列表
     */
    fun updateVideoList(newList: List<VideoItem>, hasLoadedAll: Boolean) {
        videoList.clear()
        videoList.addAll(newList)
        hasLoadedAllFromBackend = hasLoadedAll
        notifyDataSetChanged()
    }

    /**
     * 在列表开头添加视频（用于下拉刷新）
     */
    fun prependVideos(newVideos: List<VideoItem>, hasLoadedAll: Boolean) {
        videoList.addAll(0, newVideos)
        hasLoadedAllFromBackend = hasLoadedAll
        notifyDataSetChanged()
    }

    /**
     * 在列表末尾添加视频（用于上拉加载更多）
     */
    fun appendVideos(newVideos: List<VideoItem>, hasLoadedAll: Boolean) {
        videoList.addAll(newVideos)
        hasLoadedAllFromBackend = hasLoadedAll
        notifyDataSetChanged()
    }

    /**
     * 获取指定位置的视频
     * ✅ 修复：支持无限循环模式，使用取模获取正确的视频
     */
    fun getVideoAt(position: Int): VideoItem? {
        if (videoList.isEmpty()) {
            return null
        }
        // 在无限循环模式下，使用取模获取正确的视频
        val safeIndex = position % videoList.size
        return videoList[safeIndex]
    }
    
    /**
     * 根据 itemId（video.id）查找对应的视频
     * ✅ 新增：用于根据稳定的 Numeric ID 查找视频
     */
    fun getVideoById(itemId: Long): VideoItem? {
        return videoList.firstOrNull { it.id == itemId }
    }
}


