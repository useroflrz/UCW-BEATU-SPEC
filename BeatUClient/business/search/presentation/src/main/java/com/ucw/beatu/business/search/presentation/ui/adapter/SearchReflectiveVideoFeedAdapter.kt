package com.ucw.beatu.business.search.presentation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ucw.beatu.shared.common.logger.AppLogger
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.reflect.ReflectionBridge

/**
 * Search 模块专用的视频流 Adapter。
 * 通过反射创建 videofeed 的具体 Fragment，避免对 videofeed:presentation 的编译期依赖。
 */
class SearchReflectiveVideoFeedAdapter(
    parentFragment: Fragment,
    private var videoList: MutableList<VideoItem> = mutableListOf(),
    private var hasLoadedAllFromBackend: Boolean = false
) : FragmentStateAdapter(parentFragment) {

    override fun getItemCount(): Int {
        if (videoList.isEmpty()) return 0
        return if (hasLoadedAllFromBackend) Int.MAX_VALUE else videoList.size
    }

    override fun getItemId(position: Int): Long {
        if (videoList.isEmpty()) return position.toLong()
        val safeIndex = position % videoList.size
        return videoList[safeIndex].id
    }

    override fun containsItem(itemId: Long): Boolean {
        if (videoList.isEmpty()) return false
        return videoList.any { it.id == itemId }
    }

    override fun createFragment(position: Int): Fragment {
        val safeIndex = if (videoList.isEmpty()) 0 else position % videoList.size
        val videoItem = videoList[safeIndex]
        val className = if (videoItem.type == FeedContentType.IMAGE_POST) {
            IMAGE_POST_FRAGMENT_CLASS
        } else {
            VIDEO_ITEM_FRAGMENT_CLASS
        }
        return newInstanceByReflection(className, videoItem)
    }

    fun updateVideoList(newList: List<VideoItem>, hasLoadedAll: Boolean) {
        videoList.clear()
        videoList.addAll(newList)
        hasLoadedAllFromBackend = hasLoadedAll
        notifyDataSetChanged()
    }

    private fun newInstanceByReflection(className: String, videoItem: VideoItem): Fragment {
        return ReflectionBridge.createByCompanion(
            className = className,
            methodName = "newInstance",
            returnType = Fragment::class.java,
            caller = TAG,
            videoItem
        ) ?: run {
            AppLogger.e(TAG, "createFragment fallback: class=$className, videoId=${videoItem.id}")
            Fragment()
        }
    }

    companion object {
        private const val TAG = "SearchReflectiveAdapter"
        private const val VIDEO_ITEM_FRAGMENT_CLASS =
            "com.ucw.beatu.business.videofeed.presentation.ui.VideoItemFragment"
        private const val IMAGE_POST_FRAGMENT_CLASS =
            "com.ucw.beatu.business.videofeed.presentation.ui.ImagePostFragment"
    }
}
