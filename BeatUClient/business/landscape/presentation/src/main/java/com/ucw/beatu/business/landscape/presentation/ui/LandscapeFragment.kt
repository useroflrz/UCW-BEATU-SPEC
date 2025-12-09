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
            // ✅ 修复：过滤掉 PORTRAIT 视频，只保留 LANDSCAPE 视频
            val landscapeOnlyList = videoList.filter { it.orientation == com.ucw.beatu.shared.common.model.VideoOrientation.LANDSCAPE }
            
            if (landscapeOnlyList.isEmpty()) {
                Log.w(TAG, "LandscapeFragment: 传入的视频列表中没有任何 LANDSCAPE 视频，加载所有横屏视频")
                handleExternalVideoArgs()
                viewModel.loadVideoList()
                return
            }
            
            // ✅ 修复：转换为横屏页面的 VideoItem 模型，确保 ID 类型正确
            val landscapeVideoList = landscapeOnlyList.map { commonItem ->
                VideoItem(
                    id = commonItem.id,  // ✅ 修复：commonItem.id 已经是 Long 类型
                    videoUrl = commonItem.videoUrl,
                    title = commonItem.title,
                    authorName = commonItem.authorName,
                    likeCount = commonItem.likeCount.toInt(),
                    commentCount = commonItem.commentCount.toInt(),
                    favoriteCount = commonItem.favoriteCount.toInt(),
                    shareCount = commonItem.shareCount.toInt()
                )
            }
            // ✅ 修复：获取当前视频ID，确保匹配正确的视频
            val targetVideoId = args.getLong(LandscapeLaunchContract.EXTRA_VIDEO_ID, -1L)
            val currentIndex = if (targetVideoId != -1L) {
                // 优先使用传入的索引，如果没有则根据视频ID查找
                val providedIndex = args.getInt(LandscapeLaunchContract.EXTRA_CURRENT_INDEX, -1)
                if (providedIndex >= 0 && providedIndex < landscapeVideoList.size) {
                    providedIndex
                } else {
                    // 根据视频ID查找索引（在过滤后的列表中）
                    landscapeVideoList.indexOfFirst { it.id == targetVideoId }.let {
                        if (it == -1) {
                            Log.w(TAG, "LandscapeFragment: 目标视频ID=$targetVideoId 不在过滤后的 LANDSCAPE 视频列表中")
                            0
                        } else {
                            it
                        }
                    }
                }
            } else {
                args.getInt(LandscapeLaunchContract.EXTRA_CURRENT_INDEX, 0)
            }
            Log.d(TAG, "LandscapeFragment: Using fixed LANDSCAPE video list, 过滤前数量=${videoList.size}，过滤后数量=${landscapeVideoList.size}, targetVideoId=$targetVideoId, currentIndex=$currentIndex")
            viewModel.setVideoList(landscapeVideoList, currentIndex)
            // ✅ 修复：设置视频列表后，跳转到当前索引，并延迟触发 handlePageSelected 确保 Fragment 已加载视频
            viewPager?.postDelayed({
                val boundedIndex = currentIndex.coerceIn(0, landscapeVideoList.lastIndex)
                Log.d(TAG, "LandscapeFragment: Setting ViewPager to index $boundedIndex, videoId=${landscapeVideoList.getOrNull(boundedIndex)?.id}")
                viewPager?.setCurrentItem(boundedIndex, false)
                // ✅ 修复：延迟调用 handlePageSelected，确保 Fragment 已加载视频并准备好播放器
                viewPager?.postDelayed({
                    handlePageSelected(boundedIndex)
                }, 150) // 延迟150ms，确保 Fragment 的 loadVideo() 已执行
            }, 50) // 先延迟50ms，确保 ViewPager 已设置好适配器
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
        // ✅ 修复：首次创建时手动触发一次，确保第一个页面播放/其他暂停
        // 延迟执行，确保 ViewPager2 和 Fragment 都已完全初始化
        viewPager?.postDelayed({
            handlePageSelected(viewPager?.currentItem ?: 0)
        }, 100) // 延迟100ms，确保 Fragment 已加载视频

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
                    // ✅ 修复：在更新 Adapter 之前，再次过滤确保只有 LANDSCAPE 视频
                    // 虽然 ViewModel 已经过滤，但这里再加一层防护
                    val landscapeOnlyList = state.videoList.filter { 
                        it.orientation == com.ucw.beatu.business.landscape.domain.model.VideoOrientation.LANDSCAPE 
                    }
                    if (landscapeOnlyList.size != state.videoList.size) {
                        Log.w(TAG, "observeUiState: 检测到非 LANDSCAPE 视频，已过滤，原始数量=${state.videoList.size}，过滤后数量=${landscapeOnlyList.size}")
                    }
                    adapter?.updateVideoList(landscapeOnlyList)
                }
            }
        }
    }

    private fun handleExternalVideoArgs() {
        if (externalVideoHandled) return
        val args = arguments ?: return
        // ✅ 修复：videoId 现在是 Long 类型，使用 getLong 而不是 getString
        val videoId = args.getLong(LandscapeLaunchContract.EXTRA_VIDEO_ID, -1L)
        if (videoId == -1L) {
            Log.w(TAG, "handleExternalVideoArgs: videoId not found in arguments")
            return
        }
        
        // ✅ 新增：检查视频列表中是否包含该视频，如果包含则检查其 orientation
        // 如果从视频列表传入，会包含 EXTRA_VIDEO_LIST，此时应该已经在上层过滤过了
        // 但为了安全，这里也做一次检查
        val videoList = args.getParcelableArrayList<CommonVideoItem>(LandscapeLaunchContract.EXTRA_VIDEO_LIST)
        if (videoList != null && videoList.isNotEmpty()) {
            val targetVideo = videoList.find { it.id == videoId }
            if (targetVideo != null && targetVideo.orientation != com.ucw.beatu.shared.common.model.VideoOrientation.LANDSCAPE) {
                Log.w(TAG, "handleExternalVideoArgs: 外部视频不是 LANDSCAPE 视频，无法进入横屏模式，videoId=$videoId")
                return
            }
        }

        val videoUrl = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_URL) ?: return

        val videoItem = VideoItem(
            id = videoId,  // ✅ 修复：直接使用 Long 类型，不需要转换
            videoUrl = videoUrl,
            title = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_TITLE).orEmpty(),
            authorName = args.getString(LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR).orEmpty(),
            likeCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_LIKE),
            commentCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_COMMENT),
            favoriteCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE),
            shareCount = args.getInt(LandscapeLaunchContract.EXTRA_VIDEO_SHARE)
        )
        Log.d(TAG, "handleExternalVideoArgs: 处理外部 LANDSCAPE 视频，videoId=$videoId, videoUrl=$videoUrl")
        viewModel.showExternalVideo(videoItem)
        externalVideoHandled = true
    }

    /**
     * 退出横屏模式，返回到来源页面
     * ✅ 修复：根据来源页面决定返回到哪里，而不是简单地 popBackStack
     * ✅ 修复：确保返回到对应的竖屏视频页面，并正确恢复播放进度
     */
    private fun exitLandscape() {
        // ✅ 修复：先保存播放器状态并解绑 Surface，确保播放进度被正确保存
        val currentFragment = currentLandscapeItemFragment()
        // ✅ 修复：prepareForExit() 方法内部已经处理了保存播放会话的逻辑，不需要在这里访问 viewModel
        currentFragment?.prepareForExit()
        
        // 恢复屏幕方向
        restoreOrientation()

        // ✅ 修复：获取来源页面 ID 和视频 ID，确保返回到正确的页面
        val args = arguments
        val sourceDestinationId = args?.getInt(LandscapeLaunchContract.EXTRA_SOURCE_DESTINATION, 0) ?: 0
        val sourceVideoId = args?.getLong(LandscapeLaunchContract.EXTRA_VIDEO_ID, -1L) ?: -1L
        
        Log.d(TAG, "exitLandscape: 来源页面 ID=$sourceDestinationId, 来源视频ID=$sourceVideoId")
        val navController = findNavController()
        val currentDestinationId = navController.currentDestination?.id
        
        Log.d(TAG, "exitLandscape: 来源页面 ID=$sourceDestinationId, 来源视频ID=$sourceVideoId, 当前页面 ID=$currentDestinationId")
        
        // ✅ 修复：保存 sourceVideoId 到 SharedPreferences，以便 RecommendFragment 读取
        if (sourceVideoId != -1L) {
            val prefs = requireContext().getSharedPreferences("landscape_exit", android.content.Context.MODE_PRIVATE)
            prefs.edit().putLong("source_video_id", sourceVideoId).apply()
            Log.d(TAG, "exitLandscape: 保存 sourceVideoId=$sourceVideoId 到 SharedPreferences")
        }
        
        // ✅ 修复：直接使用 popBackStack() 返回到上一个页面
        // Navigation Component 会自动处理返回到正确的页面（FEED 或 USER_WORKS_VIEWER）
        // 因为导航栈的结构是：FEED/USER_WORKS_VIEWER -> LANDSCAPE
        // 所以 popBackStack() 会自动返回到正确的来源页面
        Log.d(TAG, "exitLandscape: 使用 popBackStack() 返回到上一个页面")
        val popped = runCatching { 
            navController.popBackStack()
        }.onFailure { e ->
            Log.w(TAG, "popBackStack failed: ${e.message}, fallback to finish()", e)
            requireActivity().finish()
        }.getOrDefault(false)

        if (!popped) {
            Log.w(TAG, "exitLandscape: popBackStack() 返回 false，使用 finish()")
            requireActivity().finish()
        } else {
            Log.d(TAG, "exitLandscape: 成功返回到上一个页面")
            // ✅ 修复：延迟一小段时间，确保 Fragment 已经恢复，然后触发恢复播放
            // 使用 post 确保在 UI 线程中执行，并且 Fragment 已经恢复
            view?.postDelayed({
                Log.d(TAG, "exitLandscape: Fragment 应该已经恢复，播放会话应该会被自动恢复")
            }, 100)
        }
    }

    private fun currentLandscapeItemFragment(): LandscapeVideoItemFragment? {
        val index = viewPager?.currentItem ?: return null
        val tag = "f$index"
        return childFragmentManager.findFragmentByTag(tag) as? LandscapeVideoItemFragment
    }

    private fun handlePageSelected(position: Int) {
        val adapter = this.adapter ?: return
        val total = adapter.itemCount
        if (position < 0 || position >= total) {
            Log.w(TAG, "handlePageSelected: invalid position=$position, total=$total")
            return
        }
        
        Log.d(TAG, "handlePageSelected: position=$position, total=$total")
        
        // ✅ 修复：遍历所有 Fragment，根据位置决定可见性
        // ViewPager2 的 FragmentStateAdapter 使用的 tag 格式是 "f$position"
        childFragmentManager.fragments
            .filterIsInstance<LandscapeVideoItemFragment>()
            .forEach { fragment ->
                // 从 tag 中提取位置（格式为 "f$position"）
                val fragmentTag = fragment.tag ?: ""
                val fragmentPosition = fragmentTag.removePrefix("f").toIntOrNull()
                
                if (fragmentPosition == position) {
                    // 当前选中的 Fragment，设置为可见
                    Log.d(TAG, "handlePageSelected: Fragment at position=$fragmentPosition is visible")
                    fragment.onParentVisibilityChanged(true)
                } else {
                    // 其他 Fragment，设置为不可见并暂停
                    Log.d(TAG, "handlePageSelected: Fragment at position=$fragmentPosition is hidden (current=$position)")
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