package com.ucw.beatu.business.landscape.presentation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ucw.beatu.business.landscape.presentation.model.VideoItem

import com.ucw.beatu.business.landscape.presentation.ui.LandscapeVideoItemFragment

/**
 * 横屏视频流 Adapter
 * 用于 ViewPager2 显示横屏视频列表
 */
class LandscapeVideoAdapter(
    fragmentActivity: FragmentActivity,
    private var videoList: MutableList<VideoItem> = mutableListOf()
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = videoList.size

    override fun createFragment(position: Int): Fragment {
        val videoItem = videoList[position]
        return LandscapeVideoItemFragment.newInstance(videoItem)
    }

    fun updateVideoList(newList: List<VideoItem>) {
        videoList.clear()
        videoList.addAll(newList)
        notifyDataSetChanged()
    }

    fun appendVideos(newVideos: List<VideoItem>) {
        videoList.addAll(newVideos)
        notifyDataSetChanged()
    }

    fun getVideoAt(position: Int): VideoItem? {
        return if (position in 0 until videoList.size) {
            videoList[position]
        } else {
            null
        }
    }
}

