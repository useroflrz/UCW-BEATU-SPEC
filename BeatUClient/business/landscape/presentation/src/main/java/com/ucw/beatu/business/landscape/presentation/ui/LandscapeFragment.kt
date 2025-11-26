package com.ucw.beatu.business.landscape.presentation.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.ucw.beatu.business.landscape.presentation.R
import com.ucw.beatu.business.landscape.presentation.model.VideoItem
import com.ucw.beatu.business.landscape.presentation.ui.adapter.LandscapeVideoAdapter
import com.ucw.beatu.business.landscape.presentation.viewmodel.LandscapeViewModel
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        originalOrientation = requireActivity().requestedOrientation
        shouldForcePortraitOnExit = originalOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        handleExternalVideoArgs()
        setupViews(view)
        observeUiState()
        setupBackPressed()

        viewModel.loadVideoList()
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
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val total = adapter?.itemCount ?: 0
                    if (total > 0 && position >= total - 2) {
                        viewModel.loadMoreVideos()
                    }
                }
            })
        }

        // 使用 NavController 返回（而非 btn_exit_landscape）
        root.findViewById<View>(R.id.btn_exit_landscape)?.setOnClickListener {
            exitLandscape()
        }
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
        //使用 NavController 返回，NavHost 会自动处理栈管理
        findNavController().popBackStack()
    }

    private fun currentLandscapeItemFragment(): LandscapeVideoItemFragment? {
        val index = viewPager?.currentItem ?: return null
        val tag = "f$index"
        return childFragmentManager.findFragmentByTag(tag) as? LandscapeVideoItemFragment
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
}