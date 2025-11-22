package com.ucw.beatu.business.videofeed.presentation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.ui.VideoItemFragment

/**
 * 视频流 Adapter
 * 用于 ViewPager2 显示视频列表
 */
class VideoFeedAdapter(
    fragmentActivity: FragmentActivity,
    private var videoList: MutableList<VideoItem> = mutableListOf()
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = videoList.size

    override fun createFragment(position: Int): Fragment {
        val videoItem = videoList[position]
        return VideoItemFragment.newInstance(videoItem)
    }

    /**
     * 更新视频列表
     */
    fun updateVideoList(newList: List<VideoItem>) {
        videoList.clear()
        videoList.addAll(newList)
        notifyDataSetChanged()
    }

    /**
     * 在列表开头添加视频（用于下拉刷新）
     */
    fun prependVideos(newVideos: List<VideoItem>) {
        videoList.addAll(0, newVideos)
        notifyDataSetChanged()
    }

    /**
     * 在列表末尾添加视频（用于上拉加载更多）
     */
    fun appendVideos(newVideos: List<VideoItem>) {
        videoList.addAll(newVideos)
        notifyDataSetChanged()
    }

    /**
     * 获取指定位置的视频
     */
    fun getVideoAt(position: Int): VideoItem? {
        return if (position in 0 until videoList.size) {
            videoList[position]
        } else {
            null
        }
    }
}


