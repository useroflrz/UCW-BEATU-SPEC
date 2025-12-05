package com.ucw.beatu.business.user.presentation.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EdgeEffect
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.ucw.beatu.business.user.presentation.R
import com.ucw.beatu.business.user.presentation.ui.adapter.UserWorksViewerAdapter
import com.ucw.beatu.business.user.presentation.viewmodel.UserWorksViewerViewModel

import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.router.UserWorksViewerRouter
import com.ucw.beatu.shared.router.RouterRegistry
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import kotlin.math.max
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserWorksViewerFragment : Fragment(R.layout.fragment_user_works_viewer), UserWorksViewerRouter {
    private val TAG = javaClass.simpleName
    private val viewModel: UserWorksViewerViewModel by viewModels()

    private var viewPager: ViewPager2? = null
    private var adapter: UserWorksViewerAdapter? = null
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            val total = adapter?.itemCount ?: 0
            if (total == 0) return
            val bounded = position.coerceIn(0, total - 1)
            if (position != bounded) {
                viewPager?.setCurrentItem(bounded, false)
                return
            }
            viewModel.updateCurrentIndex(bounded)
            handlePageSelected(bounded)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view)
        setupViewPager(view)
        parseArgumentsIfNeeded(requireArguments())
        collectUiState()
        // 注册 Router，供子 Fragment 使用
        RouterRegistry.registerUserWorksViewerRouter(this)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        viewPager = null
        adapter = null
        // 取消注册 Router
        RouterRegistry.registerUserWorksViewerRouter(null)
    }
    
    // UserWorksViewerRouter 接口实现
    override fun switchToVideo(index: Int): Boolean {
        val adapter = this.adapter
        val pager = this.viewPager
        if (adapter == null || pager == null) {
            Log.w(TAG, "Cannot switch video: adapter or viewPager is null")
            return false
        }
        
        val itemCount = adapter.itemCount
        if (itemCount == 0) {
            Log.w(TAG, "Cannot switch video: video list is empty")
            return false
        }
        
        val boundedIndex = index.coerceIn(0, itemCount - 1)
        val currentIndex = pager.currentItem
        
        // 如果目标索引与当前索引相同，不需要切换
        if (currentIndex == boundedIndex) {
            Log.d(TAG, "Already at video index $boundedIndex, no need to switch")
            return true
        }
        
        // 直接切换 ViewPager2，使用平滑滚动
        // 这会触发 pageChangeCallback，进而更新 ViewModel 的 currentIndex
        pager.setCurrentItem(boundedIndex, true)
        Log.d(TAG, "Switched to video at index $boundedIndex (from $currentIndex)")
        return true
    }
    
    override fun getCurrentUserId(): String? {
        return viewModel.uiState.value.userId.takeIf { it.isNotEmpty() }
    }

    private fun setupToolbar(root: View) {
        val toolbar: MaterialToolbar = root.findViewById(R.id.toolbar_user_works)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupViewPager(root: View) {
        val pager = root.findViewById<ViewPager2>(R.id.vp_user_works)
        adapter = UserWorksViewerAdapter(this)
        pager.adapter = adapter
        pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        pager.offscreenPageLimit = 1
        attachBounceEffect(pager)
        pager.registerOnPageChangeCallback(pageChangeCallback)
        viewPager = pager
    }

    private fun parseArgumentsIfNeeded(bundle: Bundle) {
        val userId = bundle.getString(ARG_USER_ID).orEmpty()
        val initialIndex = bundle.getInt(ARG_INITIAL_INDEX, 0)
        val videos = BundleCompat.getParcelableArrayList(bundle, ARG_VIDEO_LIST, VideoItem::class.java)
            ?: arrayListOf()
        val searchTitle = bundle.getString(ARG_SEARCH_TITLE).orEmpty()
        val sourceTab = bundle.getString(ARG_SOURCE_TAB).orEmpty()

        Log.d(TAG, "parseArgumentsIfNeeded: userId=$userId, initialIndex=$initialIndex, videoListSize=${videos.size}, searchTitle=$searchTitle, sourceTab=$sourceTab")

        videos.forEachIndexed { index, video ->
            Log.d(TAG, "Video[$index]: id=${video.id}, likeCount=${video.likeCount}")
        }

        // Toolbar 标题：优先显示搜索标题，否则使用默认
        val toolbar: MaterialToolbar? = view?.findViewById(R.id.toolbar_user_works)
        if (searchTitle.isNotBlank()) {
            toolbar?.title = searchTitle
        }

        // 允许 userId 为空（搜索模式），仅依赖传入的视频列表
        viewModel.setInitialData(userId, videos, initialIndex)
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "collectUiState: videoListSize=${state.videoList.size}, currentIndex=${state.currentIndex}")

                    if (state.videoList.isEmpty()) {
                        Log.w(TAG, "collectUiState: video list is empty, nothing to show")
                    }

                    state.videoList.forEachIndexed { index, video ->
                        Log.d(TAG, "UI State Video[$index]: id=${video.id}, likeCount=${video.likeCount}")
                    }

                    // 延迟提交列表，避免 RecyclerView 崩溃
                    viewPager?.post {
                        adapter?.submitList(state.videoList.toList()) // 提交新列表副本更安全

                        val desiredIndex = state.currentIndex.coerceIn(0, max(state.videoList.lastIndex, 0))
                        val current = viewPager?.currentItem ?: -1
                        if (current != desiredIndex) {
                            viewPager?.setCurrentItem(desiredIndex, false)
                        }
                        handlePageSelected(desiredIndex)
                    }
                }
            }
        }
    }

    private fun handlePageSelected(position: Int) {
        val fragmentTag = "f$position"
        val currentFragment = childFragmentManager.findFragmentByTag(fragmentTag)

        // 使用 Router 接口调用 VideoItemFragment 的方法，避免编译时直接依赖
        val router = RouterRegistry.getVideoItemRouter()
        if (router != null) {
            childFragmentManager.fragments.forEach { fragment ->
                if (fragment == currentFragment && fragment.isVisible) {
                    router.checkVisibilityAndPlay(fragment)
                } else {
                    router.onParentVisibilityChanged(fragment, false)
                }
            }
        } else {
            Log.e("UserWorksViewerFragment", "VideoItemRouter not registered")
        }
    }

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_VIDEO_LIST = "video_list"
        const val ARG_INITIAL_INDEX = "initial_index"
        const val ARG_SEARCH_TITLE = "search_title"
        const val ARG_SOURCE_TAB = "source_tab" // works/favorite/like/history/search
    }

    private fun attachBounceEffect(pager: ViewPager2) {
        val recyclerView = pager.getChildAt(0) as? RecyclerView ?: return
        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(rv: RecyclerView, direction: Int): EdgeEffect {
                if (direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT) {
                    return super.createEdgeEffect(rv, direction)
                }
                return BounceEdgeEffect(pager, direction)
            }
        }
    }

    private class BounceEdgeEffect(
        private val viewPager: ViewPager2,
        private val direction: Int
    ) : EdgeEffect(viewPager.context) {

        private var pulling = false
        private val maxTranslationPx =
            viewPager.context.resources.displayMetrics.density * MAX_TRANSLATION_DP

        override fun onPull(deltaDistance: Float) {
            super.onPull(deltaDistance)
            handlePull(deltaDistance)
        }

        override fun onPull(deltaDistance: Float, displacement: Float) {
            super.onPull(deltaDistance, displacement)
            handlePull(deltaDistance)
        }

        override fun onRelease() {
            super.onRelease()
            if (pulling) {
                animateBack()
                pulling = false
            }
        }

        override fun onAbsorb(velocity: Int) {
            super.onAbsorb(velocity)
            animateBack()
        }

        private fun handlePull(deltaDistance: Float) {
            val sign = if (direction == RecyclerView.EdgeEffectFactory.DIRECTION_TOP) 1 else -1
            val drag = sign * viewPager.height * deltaDistance * 0.6f
            val newTranslation = (viewPager.translationY + drag)
            val clamped = newTranslation.coerceIn(-maxTranslationPx, maxTranslationPx)
            viewPager.translationY = clamped
            pulling = true
        }

        private fun animateBack() {
            viewPager.animate()
                .translationY(0f)
                .setDuration(250L)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
        }

        companion object {
            private const val MAX_TRANSLATION_DP = 96f
        }
    }
}


