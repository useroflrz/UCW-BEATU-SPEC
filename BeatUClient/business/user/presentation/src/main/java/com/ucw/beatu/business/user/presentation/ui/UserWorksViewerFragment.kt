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
import com.ucw.beatu.business.videofeed.presentation.model.VideoItem
import com.ucw.beatu.business.videofeed.presentation.ui.VideoItemFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserWorksViewerFragment : Fragment(R.layout.fragment_user_works_viewer) {

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        viewPager = null
        adapter = null
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
        val userId = bundle.getString(ARG_USER_ID) ?: return
        val initialIndex = bundle.getInt(ARG_INITIAL_INDEX, 0)
        val videos = BundleCompat.getParcelableArrayList(bundle, ARG_VIDEO_LIST, VideoItem::class.java)
            ?: arrayListOf()
        viewModel.setInitialData(userId, videos, initialIndex)
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter?.submitList(state.videoList)
                    if (state.videoList.isEmpty()) {
                        return@collect
                    }
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

    private fun handlePageSelected(position: Int) {
        val fragmentTag = "f$position"
        val currentFragment =
            childFragmentManager.findFragmentByTag(fragmentTag) as? VideoItemFragment

        childFragmentManager.fragments
            .filterIsInstance<VideoItemFragment>()
            .forEach { fragment ->
                if (fragment == currentFragment && fragment.isVisible) {
                    fragment.checkVisibilityAndPlay()
                } else {
                    fragment.onParentVisibilityChanged(false)
                }
            }
    }

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_VIDEO_LIST = "video_list"
        const val ARG_INITIAL_INDEX = "initial_index"
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


