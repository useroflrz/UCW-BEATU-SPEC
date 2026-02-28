package com.ucw.beatu.business.videofeed.presentation.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.transition.TransitionManager
import com.ucw.beatu.business.user.domain.model.User
import com.ucw.beatu.business.user.domain.repository.UserRepository
import com.ucw.beatu.business.videofeed.presentation.R
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.common.threading.SuspendBridge
import com.ucw.beatu.shared.common.threading.TaskNames
import com.ucw.beatu.shared.common.threading.TaskRunner
import com.ucw.beatu.shared.router.RouterRegistry

/**
 * 用户信息覆盖层协调器：
 * 负责用户信息弹窗展示/关闭、视频区域布局切换和用户作品页导航。
 */
class VideoUserInfoOverlayCoordinator(
    private val tag: String,
    private val userRepository: UserRepository,
    private val rootLayoutProvider: () -> ConstraintLayout?,
    private val playerViewProvider: () -> PlayerView?,
    private val hostContextProvider: () -> Context?,
    private val parentFragmentManagerProvider: () -> FragmentManager,
    private val navControllerProvider: () -> NavController?,
    private val hasPreparedPlayerProvider: () -> Boolean,
    private val currentVideoItemProvider: () -> VideoItem?,
    private val persistPlaybackSession: () -> Unit,
    private val mediaPlayerProvider: () -> ExoPlayer?
) {
    private var isUserInfoVisible = false
    private var userProfileDialogFragment: UserProfileDialogFragment? = null

    fun isUserInfoVisible(): Boolean = isUserInfoVisible

    fun clear() {
        hideUserInfoOverlay()
    }

    fun showUserInfoOverlay(authorId: String, authorName: String) {
        if (isUserInfoVisible) {
            hideUserInfoOverlay()
            return
        }

        val layout = rootLayoutProvider() ?: return
        if (playerViewProvider() == null) return
        isUserInfoVisible = true

        applyHalfScreenLayout(layout)

        val userId = if (authorId.isNotEmpty()) authorId else authorName
        TaskRunner.run(
            name = TaskNames.of(
                feature = "ui_video_user_overlay",
                action = "show_user_info_overlay"
            ),
            timeoutMs = 6_000L,
            onError = { throwable -> Log.e(tag, "showUserInfoOverlay failed", throwable) }
        ) {
            val dialogFragment = buildDialogFragment(authorId, authorName, userId)
            userProfileDialogFragment = dialogFragment

            userProfileDialogFragment?.setOnDismissListener {
                hideUserInfoOverlay()
            }
            userProfileDialogFragment?.setOnVideoClickListener { clickedUserId, _, videoItems, initialIndex ->
                hideUserInfoOverlay()
                navigateToUserWorksViewer(clickedUserId, videoItems, initialIndex)
            }
            userProfileDialogFragment?.show(parentFragmentManagerProvider(), "UserProfileDialog")
        }
    }

    fun hideUserInfoOverlay() {
        if (!isUserInfoVisible) return
        isUserInfoVisible = false

        userProfileDialogFragment?.dismissAllowingStateLoss()
        userProfileDialogFragment = null

        rootLayoutProvider()?.let { applyFullScreenLayout(it) }
    }

    private fun buildDialogFragment(
        authorId: String,
        authorName: String,
        userId: String
    ): UserProfileDialogFragment {
        return try {
            var user: User? = null
            if (authorId.isNotEmpty()) {
                user = SuspendBridge.runBlocking { userRepository.getUserById(authorId) }
                Log.d(tag, "Trying to get user by ID: $authorId, result: ${if (user != null) "found" else "not found"}")
            }
            if (user == null && authorName.isNotEmpty() && authorName != authorId) {
                user = SuspendBridge.runBlocking { userRepository.getUserByName(authorName) }
                Log.d(tag, "Trying to get user by name: $authorName, result: ${if (user != null) "found" else "not found"}")
            }
            if (user == null && authorName.isNotEmpty() && authorName.all { it.isDigit() }) {
                user = SuspendBridge.runBlocking { userRepository.getUserById(authorName) }
                Log.d(tag, "Trying to get user by ID (from authorName): $authorName, result: ${if (user != null) "found" else "not found"}")
            }

            if (user != null) {
                Log.d(tag, "User found: id=${user.id}, name=${user.name}, avatarUrl=${user.avatarUrl}")
                UserProfileDialogFragment.newInstanceWithData(toBundle(user))
            } else {
                Log.w(tag, "User not found in local DB: userId=$userId, authorName=$authorName, will try remote fetch")
                UserProfileDialogFragment.newInstance(userId, authorName)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to get user data: userId=$userId, authorName=$authorName", e)
            UserProfileDialogFragment.newInstance(userId, authorName)
        }
    }

    private fun toBundle(user: User): Bundle {
        return Bundle().apply {
            putString("id", user.id)
            putString("name", user.name)
            putString("avatarUrl", user.avatarUrl)
            putString("bio", user.bio)
            putLong("likesCount", user.likesCount)
            putLong("followingCount", user.followingCount)
            putLong("followersCount", user.followersCount)
        }
    }

    private fun applyHalfScreenLayout(layout: ConstraintLayout) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)

        constraintSet.clear(R.id.player_view, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.player_view, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.player_view, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.constrainPercentHeight(R.id.player_view, 0.5f)

        constraintSet.clear(R.id.image_pager, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.image_pager, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.image_pager, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.constrainPercentHeight(R.id.image_pager, 0.5f)

        constraintSet.clear(R.id.video_controls, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.video_controls, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.video_controls, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.constrainPercentHeight(R.id.video_controls, 0.5f)

        TransitionManager.beginDelayedTransition(layout)
        constraintSet.applyTo(layout)
    }

    private fun applyFullScreenLayout(layout: ConstraintLayout) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)

        constraintSet.clear(R.id.player_view, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.player_view, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.player_view, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(R.id.player_view, 1.0f)

        constraintSet.clear(R.id.image_pager, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.image_pager, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.image_pager, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(R.id.image_pager, 1.0f)

        constraintSet.clear(R.id.video_controls, ConstraintSet.BOTTOM)
        constraintSet.connect(R.id.video_controls, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(R.id.video_controls, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(R.id.video_controls, 1.0f)

        TransitionManager.beginDelayedTransition(layout)
        constraintSet.applyTo(layout)
    }

    private fun navigateToUserWorksViewer(userId: String, videoItems: ArrayList<VideoItem>, initialIndex: Int) {
        val navController = navControllerProvider()
        if (navController == null) {
            Log.e(tag, "NavController not found, cannot open user works viewer")
            return
        }

        val context = hostContextProvider()
        if (context == null) {
            Log.e(tag, "Context is null, cannot open user works viewer")
            return
        }

        val userWorksViewerDestinationId = NavigationHelper.getResourceId(context, NavigationIds.USER_WORKS_VIEWER)
        val isInUserWorksViewer = navController.currentDestination?.id == userWorksViewerDestinationId

        if (isInUserWorksViewer) {
            val router = RouterRegistry.getUserWorksViewerRouter()
            val currentUserId = router?.getCurrentUserId()
            if (router != null && currentUserId == userId) {
                Log.d(tag, "Already in user works viewer for user $userId, switching to video at index $initialIndex")
                if (router.switchToVideo(initialIndex)) {
                    return
                }
                Log.w(tag, "Failed to switch video in UserWorksViewer, falling back to navigation")
            } else {
                Log.d(tag, "Already in user works viewer but different user (current: $currentUserId, target: $userId), allowing navigation")
            }
        }

        val currentVideoItem = currentVideoItemProvider()
        if (hasPreparedPlayerProvider() && currentVideoItem?.type != FeedContentType.IMAGE_POST) {
            Log.d(tag, "navigateToUserWorksViewer: 保存播放会话，videoId=${currentVideoItem?.id}")
            persistPlaybackSession()
            mediaPlayerProvider()?.let { player ->
                PlayerView.switchTargetView(player, playerViewProvider(), null)
            }
        }

        val currentDestId = navController.currentDestination?.id
        val userWorksDestId = NavigationHelper.getResourceId(context, NavigationIds.USER_WORKS_VIEWER)
        val searchResultDestId = NavigationHelper.getResourceId(context, NavigationIds.SEARCH_RESULT)
        val searchResultVideoDestId = NavigationHelper.getResourceId(context, NavigationIds.SEARCH_RESULT_VIDEO_VIEWER)
        val userProfileDestId = NavigationHelper.getResourceId(context, NavigationIds.USER_PROFILE)

        Log.d(
            tag,
            "navigateToUserWorksViewer: currentDestId=$currentDestId, " +
                "userWorksDestId=$userWorksDestId, searchResultDestId=$searchResultDestId, " +
                "searchResultVideoDestId=$searchResultVideoDestId, userProfileDestId=$userProfileDestId, " +
                "targetUserId=$userId, initialIndex=$initialIndex, videoListSize=${videoItems.size}"
        )

        val candidateIds = listOf(
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_SEARCH_RESULT_TO_USER_WORKS_VIEWER),
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_USER_PROFILE_TO_USER_WORKS_VIEWER),
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_FEED_TO_USER_WORKS_VIEWER)
        ).filter { it != 0 }

        val usableActionId = candidateIds.firstOrNull { actionId ->
            navController.currentDestination?.getAction(actionId) != null
        }
        val actionId = usableActionId ?: userWorksDestId
        if (actionId == 0) {
            Log.e(tag, "Navigation action not found for user works viewer (currentDest=$currentDestId)")
            return
        }

        Log.d(
            tag,
            "navigateToUserWorksViewer: using actionId=$actionId, usableActionId=$usableActionId, candidates=$candidateIds"
        )

        val currentDestinationId = navController.currentDestination?.id ?: 0
        val currentVideoId = currentVideoItemProvider()?.id ?: -1L
        val bundle = bundleOf(
            "user_id" to userId,
            "initial_index" to initialIndex,
            "video_list" to videoItems,
            "source_destination" to currentDestinationId,
            "source_video_id" to currentVideoId
        )
        Log.d(tag, "navigateToUserWorksViewer: 传递来源页面 ID=$currentDestinationId, 来源视频 ID=$currentVideoId")
        navController.navigate(actionId, bundle)
    }
}
