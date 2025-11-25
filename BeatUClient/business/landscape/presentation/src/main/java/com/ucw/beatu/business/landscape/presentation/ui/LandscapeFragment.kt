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

        setupViews(view)
        handleExternalVideoArgs()
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

        root.findViewById<View>(R.id.btn_exit_landscape)?.setOnClickListener {
            exitLandscape()
        }
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
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

    private fun exitLandscape() {
        currentLandscapeItemFragment()?.prepareForExit()
        restoreOrientation()
        if (!findNavController().popBackStack()) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
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

