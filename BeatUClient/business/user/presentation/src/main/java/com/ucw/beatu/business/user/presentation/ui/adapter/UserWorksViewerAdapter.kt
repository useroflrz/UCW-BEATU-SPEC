package com.ucw.beatu.business.user.presentation.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.ui.VideoItemFragment

/**
 * 个人主页作品观看页的 ViewPager2 适配器，复用 VideoItemFragment。
 */
class UserWorksViewerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    private val videoItems = mutableListOf<VideoItem>()

    override fun getItemCount(): Int = videoItems.size

    override fun createFragment(position: Int): Fragment {
        val item = videoItems[position]
        return VideoItemFragment.newInstance(item)
    }

    fun submitList(newList: List<VideoItem>) {
        videoItems.clear()
        videoItems.addAll(newList)
        notifyDataSetChanged()
    }

    fun getVideoAt(position: Int): VideoItem? = videoItems.getOrNull(position)
}


