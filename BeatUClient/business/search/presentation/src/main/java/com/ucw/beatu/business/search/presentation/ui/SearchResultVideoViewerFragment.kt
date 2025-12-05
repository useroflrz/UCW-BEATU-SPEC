package com.ucw.beatu.business.search.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.search.presentation.R
import com.ucw.beatu.business.search.presentation.viewmodel.SearchResultVideoViewModel
import com.ucw.beatu.business.videofeed.presentation.ui.ImagePostFragment
import com.ucw.beatu.business.videofeed.presentation.ui.VideoItemFragment
import com.ucw.beatu.business.videofeed.presentation.ui.adapter.VideoFeedAdapter
import com.ucw.beatu.shared.common.model.VideoItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 搜索结果视频播放页面
 * - 根据搜索词与点击的标题关键词，匹配视频列表
 * - 垂直 ViewPager2，抖音风格单列播放
 */
@AndroidEntryPoint
class SearchResultVideoViewerFragment : Fragment() {

    companion object {
        private const val TAG = "SearchResultVideoViewer"
        private const val ARG_SEARCH_QUERY = "search_query"
        private const val ARG_RESULT_TITLE = "result_title"
        private const val STATE_VIDEOS = "search_result_videos"
        private const val STATE_VIEWPAGER_INDEX = "search_result_viewpager_index"
        private const val STATE_CURRENT_PAGE = "search_result_current_page"
    }

    private val viewModel: SearchResultVideoViewModel by viewModels()

    private var viewPager: ViewPager2? = null
    private var adapter: VideoFeedAdapter? = null
    private var isRefreshing = false
    private var pendingRestoreIndex: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_result_video_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchQuery = arguments?.getString(ARG_SEARCH_QUERY).orEmpty()
        val resultTitle = arguments?.getString(ARG_RESULT_TITLE).orEmpty()

        setupViewPager(view)
        observeViewModel()

        if (savedInstanceState == null) {
            viewModel.initSearch(searchQuery, resultTitle)
        } else {
            restoreState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        resumeVisibleVideoItem()
    }

    override fun onPause() {
        super.onPause()
        pauseAllVideoItems()
    }

    private fun setupViewPager(view: View) {
        viewPager = view.findViewById(R.id.viewpager_video_feed)
        viewPager?.let { vp ->
            vp.orientation = ViewPager2.ORIENTATION_VERTICAL
            vp.offscreenPageLimit = 1

            adapter = VideoFeedAdapter(this@SearchResultVideoViewerFragment)
            vp.adapter = adapter

            vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val totalCount = adapter?.itemCount ?: 0
                    if (position >= totalCount - 2) {
                        viewModel.loadMoreVideos()
                    }
                    handlePageSelected(position)
                }
            })

            setupPullToRefresh(vp)
        }
    }

    private fun setupPullToRefresh(viewPager: ViewPager2) {
        var touchStartY = 0f
        var hasTriggeredRefresh = false

        viewPager.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.y
                    hasTriggeredRefresh = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (viewPager.currentItem == 0 && !isRefreshing && !hasTriggeredRefresh) {
                        val deltaY = event.y - touchStartY
                        if (deltaY > 100 * resources.displayMetrics.density) {
                            hasTriggeredRefresh = true
                            isRefreshing = true
                            viewModel.refreshVideoList()
                            Log.d(TAG, "Trigger pull to refresh (deltaY: $deltaY)")
                        }
                    }
                }
            }
            false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.videoList.isNotEmpty()) {
                        val hasInitialData = (adapter?.itemCount ?: 0) == 0
                        adapter?.updateVideoList(state.videoList, state.hasLoadedAllFromBackend)

                        pendingRestoreIndex?.let { target ->
                            val safeIndex = target.coerceIn(0, state.videoList.lastIndex)
                            viewPager?.post { viewPager?.setCurrentItem(safeIndex, false) }
                            pendingRestoreIndex = null
                        }
                        if (isRefreshing && !state.isRefreshing) {
                            isRefreshing = false
                            viewPager?.setCurrentItem(0, false)
                        }
                        if (hasInitialData) {
                            val currentIndex = viewPager?.currentItem ?: 0
                            viewPager?.post { handlePageSelected(currentIndex) }
                        }
                    }

                    state.error?.let { error ->
                        Log.e(TAG, "Error: $error")
                    }
                }
            }
        }
    }

    fun pauseAllVideoItems() {
        childFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is VideoItemFragment -> fragment.onParentVisibilityChanged(false)
                is ImagePostFragment -> fragment.onParentVisibilityChanged(false)
            }
        }
    }

    fun resumeVisibleVideoItem() {
        val currentPosition = viewPager?.currentItem ?: -1
        if (currentPosition >= 0) {
            handlePageSelected(currentPosition)
        }
    }

    private fun handlePageSelected(position: Int) {
        val currentFragmentTag = "f$position"
        val currentFragment = childFragmentManager.findFragmentByTag(currentFragmentTag)

        childFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is VideoItemFragment -> {
                    if (fragment == currentFragment && fragment.isVisible) {
                        fragment.checkVisibilityAndPlay()
                    } else {
                        fragment.onParentVisibilityChanged(false)
                    }
                }
                is ImagePostFragment -> {
                    if (fragment == currentFragment && fragment.isVisible) {
                        fragment.checkVisibilityAndPlay()
                    } else {
                        fragment.onParentVisibilityChanged(false)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val videos = ArrayList(viewModel.uiState.value.videoList)
        if (videos.isNotEmpty()) {
            outState.putParcelableArrayList(STATE_VIDEOS, videos)
            outState.putInt(STATE_VIEWPAGER_INDEX, viewPager?.currentItem ?: 0)
            outState.putInt(STATE_CURRENT_PAGE, viewModel.getCurrentPage())
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val videos = BundleCompat.getParcelableArrayList(savedInstanceState, STATE_VIDEOS, VideoItem::class.java)
        val restoredIndex = savedInstanceState.getInt(STATE_VIEWPAGER_INDEX, 0)
        val restoredPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, 1)
        if (!videos.isNullOrEmpty()) {
            pendingRestoreIndex = restoredIndex
            viewModel.restoreState(videos, restoredPage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
        adapter = null
    }
}

