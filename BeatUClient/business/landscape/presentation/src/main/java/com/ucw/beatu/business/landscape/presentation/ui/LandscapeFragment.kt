package com.ucw.beatu.business.landscape.presentation.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.core.os.BundleCompat
import com.ucw.beatu.business.landscape.presentation.R
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.ui.adapter.LandscapeVideoAdapter
import com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeViewModel
import com.ucw.beatu.shared.common.model.VideoItem as CommonVideoItem
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
import com.ucw.beatu.shared.designsystem.widget.NoMoreVideosToast
import android.widget.EdgeEffect
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 横屏页面 Fragment，使用 Navigation 承载 ViewPager2。
 */
@AndroidEntryPoint
class LandscapeFragment : Fragment(R.layout.fragment_landscape) {

    private val viewModel: LandscapeViewModel by viewModels()

    private var viewPager: ViewPager2? = null
    private var adapter: LandscapeVideoAdapter? = null
    private var externalVideoHandled = false
    private var originalOrientation: Int? = null
    private var shouldForcePortraitOnExit = false
    private var noMoreVideosToast: NoMoreVideosToast? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        originalOrientation = requireActivity().requestedOrientation
        shouldForcePortraitOnExit = originalOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        setupViews(view)
        observeUiState()
        setupBackPressed()

        // 如果有传入的视频列表，使用视频列表；否则加载所有横屏视频
        val args = arguments
        val videoList = args?.let {
            BundleCompat.getParcelableArrayList(it, LandscapeLaunchContract.EXTRA_VIDEO_LIST, CommonVideoItem::class.java)
        }
        if (videoList != null && videoList.isNotEmpty()) {
            // 转换为横屏页面的 VideoItem 模型
            val landscapeVideoList = videoList.map { commonItem ->
                VideoItem(
                    id = commonItem.id,
                    videoUrl = commonItem.videoUrl,
                    title = commonItem.title,
                    authorName = commonItem.authorName,
                    likeCount = commonItem.likeCount.toInt(),
                    commentCount = commonItem.commentCount.toInt(),
                    favoriteCount = commonItem.favoriteCount.toInt(),
                    shareCount = commonItem.shareCount.toInt()
                )
            }
            val currentIndex = args.getInt(LandscapeLaunchContract.EXTRA_CURRENT_INDEX, 0)
            Log.d(TAG, "LandscapeFragment: Using fixed video list, size=${landscapeVideoList.size}, currentIndex=$currentIndex")
            viewModel.setVideoList(landscapeVideoList, currentIndex)
            // 设置视频列表后，跳转到当前索引
            viewPager?.post {
                val boundedIndex = currentIndex.coerceIn(0, landscapeVideoList.lastIndex)
                Log.d(TAG, "LandscapeFragment: Setting ViewPager to index $boundedIndex")
                viewPager?.setCurrentItem(boundedIndex, false)
            }
        } else {
            // 没有视频列表时，处理外部视频参数并加载所有横屏视频
            Log.d(TAG, "LandscapeFragment: No fixed video list, loading all landscape videos")
            handleExternalVideoArgs()
            viewModel.loadVideoList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        restoreOrientation()
        viewPager = null
        adapter = null
    }

    private fun setupViews(root: View) {
        viewPager = root.findViewById(R.id.viewpager_landscape)
        adapter = LandscapeVideoAdapter(this)
        viewPager?.apply {
            adapter = this@LandscapeFragment.adapter
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
            attachBounceEffect(this, root)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val total = adapter?.itemCount ?: 0
                    // 只有在非固定视频列表模式下才加载更多
                    if (total > 0 && position >= total - 2 && !viewModel.isUsingFixedVideoList) {
                        viewModel.loadMoreVideos()
                    }
                    handlePageSelected(position)
                }
            })
        }
        // 首次创建时手动触发一次，确保第一个页面播放/其他暂停
        viewPager?.post {
            handlePageSelected(viewPager?.currentItem ?: 0)
        }

        // 顶部返回按钮（需要确保在 ViewPager 之上且可点击）
        root.findViewById<View>(R.id.btn_exit_landscape)?.apply {
            bringToFront()
            setOnClickListener {
                Log.d(TAG, "Exit button clicked")
            exitLandscape()
            }
        }
        
        // 设置提示视图
        setupNoMoreVideosToast(root)
    }
    
    private fun setupNoMoreVideosToast(root: View) {
        val container = root as? ViewGroup ?: run {
            Log.e(TAG, "setupNoMoreVideosToast: root is not a ViewGroup")
            return
        }
        noMoreVideosToast = NoMoreVideosToast(requireContext())
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.addView(noMoreVideosToast, layoutParams)
        Log.d(TAG, "setupNoMoreVideosToast: toast added to container, container=${container.javaClass.simpleName}")
    }
    
    private fun attachBounceEffect(pager: ViewPager2, root: View) {
        val recyclerView = pager.getChildAt(0) as? RecyclerView ?: return
        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(rv: RecyclerView, direction: Int): EdgeEffect {
                if (direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT) {
                    return super.createEdgeEffect(rv, direction)
                }
                return BounceEdgeEffect(pager, direction, this@LandscapeFragment)
            }
        }
    }
    
    fun showNoMoreVideosToast() {
        Log.d(TAG, "showNoMoreVideosToast called, toast=${noMoreVideosToast != null}")
        noMoreVideosToast?.show()
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 使用 NavController 返回，与导航栈同步
                    exitLandscape()
                }
            }
        )
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter?.updateVideoList(state.videoList)
                }
            }
        }
    }

    private fun handleExternalVideoArgs() {
        if (externalVideoHandled) return
        val args = arguments ?: return
        val videoId = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_ID) ?: return
        val videoUrl = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_URL) ?: return

        val videoItem = VideoItem(
            id = videoId,
            videoUrl = videoUrl,
            title = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_TITLE).orEmpty(),
            authorName = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR).orEmpty(),
            likeCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_LIKE),
            commentCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_COMMENT),
            favoriteCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE),
            shareCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_SHARE)
        )
        viewModel.showExternalVideo(videoItem)
        externalVideoHandled = true
    }

    /**
     * 退出横屏模式，返回到 Feed 页面
     * 使用 NavController 的 popBackStack() 确保与导航栈同步
     */
    private fun exitLandscape() {
        // 先保存播放器状态并解绑 Surface
        currentLandscapeItemFragment()?.prepareForExit()
        // 恢复屏幕方向
        restoreOrientation()

        val popped = runCatching { findNavController().popBackStack() }
            .onFailure { Log.w(TAG, "popBackStack failed, fallback to finish()", it) }
            .getOrDefault(false)

        if (!popped) {
            requireActivity().finish()
        }
    }

    private fun currentLandscapeItemFragment(): LandscapeVideoItemFragment? {
        val index = viewPager?.currentItem ?: return null
        val tag = "f$index"
        return childFragmentManager.findFragmentByTag(tag) as? LandscapeVideoItemFragment
    }

    private fun handlePageSelected(position: Int) {
        val currentTag = "f$position"
        childFragmentManager.fragments
            .filterIsInstance<LandscapeVideoItemFragment>()
            .forEach { fragment ->
                if (fragment.tag == currentTag && fragment.isVisible) {
                    fragment.onParentVisibilityChanged(true)
                } else {
                    fragment.onParentVisibilityChanged(false)
                }
            }
    }

    private fun restoreOrientation() {
        val target = originalOrientation
        if (target != null) {
            requireActivity().requestedOrientation = target
        } else if (shouldForcePortraitOnExit) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        shouldForcePortraitOnExit = false
    }

    fun setPagingEnabled(enabled: Boolean) {
        viewPager?.isUserInputEnabled = enabled
    }
    
    private class BounceEdgeEffect(
        private val viewPager: ViewPager2,
        private val direction: Int,
        private val fragment: LandscapeFragment
    ) : EdgeEffect(viewPager.context) {

        private var pulling = false
        private var hasShownToast = false
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
                hasShownToast = false
            }
        }

        override fun onAbsorb(velocity: Int) {
            super.onAbsorb(velocity)
            animateBack()
            hasShownToast = false
        }

        private fun handlePull(deltaDistance: Float) {
            val adapter = viewPager.adapter ?: return
            val itemCount = adapter.itemCount
            if (itemCount == 0) return

            val currentItem = viewPager.currentItem
            val isAtTop = currentItem == 0 && direction == RecyclerView.EdgeEffectFactory.DIRECTION_TOP
            val isAtBottom = currentItem == itemCount - 1 && direction == RecyclerView.EdgeEffectFactory.DIRECTION_BOTTOM

            // 如果到达边界，显示提示（固定视频列表模式始终显示，非固定模式只在底部显示）
            val shouldShowToast = if (fragment.viewModel.isUsingFixedVideoList) {
                isAtTop || isAtBottom
            } else {
                isAtBottom // 非固定模式，顶部可能还有更多视频
            }
            
            // 降低阈值，确保更容易触发
            if (shouldShowToast && !hasShownToast && Math.abs(deltaDistance) > 0.01f) {
                Log.d("BounceEdgeEffect", "Landscape: Showing no more videos toast: isAtTop=$isAtTop, isAtBottom=$isAtBottom, deltaDistance=$deltaDistance")
                fragment.showNoMoreVideosToast()
                hasShownToast = true
            }

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

private const val TAG = "LandscapeFragment"