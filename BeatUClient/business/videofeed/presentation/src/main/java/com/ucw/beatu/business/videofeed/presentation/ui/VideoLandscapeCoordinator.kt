package com.ucw.beatu.business.videofeed.presentation.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.ucw.beatu.shared.common.threading.TaskNames
import com.ucw.beatu.shared.common.threading.TaskRunner
import com.ucw.beatu.shared.common.threading.ThreadTaskPatterns
import com.ucw.beatu.shared.common.model.FeedContentType
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation
import com.ucw.beatu.shared.common.navigation.LandscapeLaunchContract
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.router.RouterRegistry

/**
 * 统一承接横竖屏切换与回流恢复流程，减少 Fragment 体积并收敛状态机。
 */
class VideoLandscapeCoordinator(
    private val tag: String,
    private val isAddedProvider: () -> Boolean,
    private val isViewVisibleOnScreenProvider: () -> Boolean,
    private val viewProvider: () -> View?,
    private val contextProvider: () -> Context?,
    private val navControllerProvider: () -> NavController?,
    private val parentFragmentProvider: () -> Any?,
    private val videoItemProvider: () -> VideoItem?,
    private val playerViewProvider: () -> PlayerView?,
    private val hasPreparedPlayerProvider: () -> Boolean,
    private val setHasPreparedPlayer: (Boolean) -> Unit,
    private val currentVideoIdProvider: () -> Long?,
    private val mediaPlayerProvider: () -> ExoPlayer?,
    private val persistPlaybackSession: () -> Any?,
    private val peekPlaybackSession: (Long) -> Any?,
    private val playVideo: (Long, String) -> Unit,
    private val onHostResume: (PlayerView) -> Unit,
    private val resumePlayback: () -> Unit
) {
    private var navigatingToLandscape = false
    private var isRestoringFromLandscape = false
    private var lastFullScreenClickTime = 0L

    fun isNavigatingToLandscape(): Boolean = navigatingToLandscape

    fun markHostResumed() {
        navigatingToLandscape = false
    }

    fun shouldSkipOnStartForLandscapeRestore(videoId: Long): Boolean {
        if (isRestoringFromLandscape) {
            Log.d(tag, "onStart: 正在从横屏恢复，跳过，等待 restorePlayerFromLandscape 处理")
            return true
        }

        val hasSession = peekPlaybackSession(videoId) != null
        if (hasSession) {
            Log.d(tag, "onStart: 检测到播放会话，设置 isRestoringFromLandscape=true，等待 restorePlayerFromLandscape 处理，videoId=$videoId")
            isRestoringFromLandscape = true
            return true
        }
        return false
    }

    fun handleFullScreenButtonClick() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFullScreenClickTime < 500) {
            Log.d(tag, "handleFullScreenButtonClick: 点击过快，忽略")
            return
        }
        lastFullScreenClickTime = currentTime

        if (navigatingToLandscape) {
            Log.d(tag, "handleFullScreenButtonClick: 正在导航中，忽略")
            return
        }

        val item = videoItemProvider() ?: run {
            Log.e(tag, "handleFullScreenButtonClick: videoItem is null")
            return
        }
        if (item.type == FeedContentType.IMAGE_POST) {
            Log.d(tag, "handleFullScreenButtonClick: 图文内容不支持横屏")
            return
        }
        if (item.orientation != VideoOrientation.LANDSCAPE) {
            Log.d(tag, "handleFullScreenButtonClick: 当前视频不是 LANDSCAPE，不支持横屏")
            return
        }

        openLandscapeMode()
    }

    fun openLandscapeMode(videoList: List<VideoItem>? = null, currentIndex: Int? = null) {
        val navController = navControllerProvider()
        if (navController == null) {
            Log.e(tag, "NavController not found, cannot open landscape mode")
            return
        }
        val context = contextProvider()
        if (context == null) {
            Log.e(tag, "Context is null, cannot open landscape mode")
            return
        }
        val item = videoItemProvider() ?: run {
            Log.e(tag, "openLandscapeMode: videoItem null")
            return
        }

        (parentFragmentProvider() as? RecommendFragment)?.notifyEnterLandscapeMode()
        RouterRegistry.getUserWorksViewerRouter()?.let {
            Log.d(tag, "openLandscapeMode: 通知 UserWorksViewerFragment 进入横屏模式")
            it.notifyEnterLandscapeMode()
        } ?: Log.d(tag, "openLandscapeMode: UserWorksViewerRouter 未注册，可能不在 UserWorksViewerFragment 中")

        mediaPlayerProvider()?.let { player ->
            val currentPosition = player.currentPosition
            Log.d(tag, "openLandscapeMode: 保存播放会话，当前播放位置=${currentPosition}ms")
            val session = persistPlaybackSession()
            Log.d(tag, "openLandscapeMode: 播放会话保存结果=${session != null}")
            PlayerView.switchTargetView(player, playerViewProvider(), null)
        } ?: Log.w(tag, "openLandscapeMode: 播放器为null，无法保存会话")

        val currentDestId = navController.currentDestination?.id
        val userWorksViewerDestId = NavigationHelper.getResourceId(context, NavigationIds.USER_WORKS_VIEWER)
        val actionId = if (currentDestId == userWorksViewerDestId) {
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_USER_WORKS_VIEWER_TO_LANDSCAPE)
        } else {
            NavigationHelper.getResourceId(context, NavigationIds.ACTION_FEED_TO_LANDSCAPE)
        }
        if (actionId == 0) {
            Log.e(tag, "Navigation action not found: currentDestId=$currentDestId, userWorksViewerDestId=$userWorksViewerDestId")
            return
        }

        val finalVideoList = videoList ?: run {
            if (currentDestId == userWorksViewerDestId) {
                RouterRegistry.getUserWorksViewerRouter()?.getCurrentVideoList()?.also {
                    Log.d(tag, "openLandscapeMode: 从 UserWorksViewerRouter 获取视频列表，数量=${it.size}")
                }
            } else null
        }

        val finalCurrentIndex = currentIndex ?: run {
            if (currentDestId == userWorksViewerDestId) {
                RouterRegistry.getUserWorksViewerRouter()?.getCurrentVideoIndex()?.also {
                    Log.d(tag, "openLandscapeMode: 从 UserWorksViewerRouter 获取当前索引=$it")
                }
            } else null
        }

        val args = Bundle().apply {
            putLong(LandscapeLaunchContract.EXTRA_VIDEO_ID, item.id)
            putString(LandscapeLaunchContract.EXTRA_VIDEO_URL, item.videoUrl)
            putString(LandscapeLaunchContract.EXTRA_VIDEO_TITLE, item.title)
            putString(LandscapeLaunchContract.EXTRA_VIDEO_AUTHOR, item.authorName)
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_LIKE, item.likeCount.toInt())
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_COMMENT, item.commentCount.toInt())
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_FAVORITE, item.favoriteCount.toInt())
            putInt(LandscapeLaunchContract.EXTRA_VIDEO_SHARE, item.shareCount.toInt())
            val sourceDestinationId = navController.currentDestination?.id ?: 0
            putInt(LandscapeLaunchContract.EXTRA_SOURCE_DESTINATION, sourceDestinationId)
            Log.d(tag, "openLandscapeMode: 记录来源页面 ID=$sourceDestinationId")
        }

        if (finalVideoList != null && finalVideoList.isNotEmpty()) {
            val landscapeVideoList = finalVideoList.filter { it.orientation == VideoOrientation.LANDSCAPE }
            if (landscapeVideoList.isEmpty()) {
                Log.w(tag, "openLandscapeMode: 视频列表中没有 LANDSCAPE 视频")
                return
            }
            if (item.orientation != VideoOrientation.LANDSCAPE) {
                Log.w(tag, "openLandscapeMode: 当前视频不是 LANDSCAPE 视频")
                return
            }
            args.putParcelableArrayList(LandscapeLaunchContract.EXTRA_VIDEO_LIST, ArrayList(landscapeVideoList))
            val index = landscapeVideoList.indexOfFirst { it.id == item.id }.let { if (it == -1) 0 else it }
            val normalizedIndex = (finalCurrentIndex ?: index).coerceIn(0, landscapeVideoList.lastIndex)
            args.putInt(LandscapeLaunchContract.EXTRA_CURRENT_INDEX, normalizedIndex)
            Log.d(tag, "openLandscapeMode: 传递 LANDSCAPE 列表，过滤后数量=${landscapeVideoList.size}，当前索引=$normalizedIndex")
        } else {
            if (item.orientation != VideoOrientation.LANDSCAPE) {
                Log.w(tag, "openLandscapeMode: 当前视频不是 LANDSCAPE 视频，无法进入横屏模式")
                return
            }
        }

        navigatingToLandscape = true
        viewProvider()?.post {
            runCatching { navController.navigate(actionId, args) }
                .onFailure {
                    navigatingToLandscape = false
                    (parentFragmentProvider() as? RecommendFragment)?.notifyExitLandscapeMode()
                    Log.e(tag, "Failed to navigate to landscape fragment", it)
                }
        }
    }

    fun restorePlayerFromLandscape() {
        if (!isAddedProvider() || videoItemProvider() == null) {
            Log.w(tag, "restorePlayerFromLandscape: Fragment not ready, skip")
            return
        }
        val item = videoItemProvider() ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            Log.d(tag, "restorePlayerFromLandscape: skip for image post ${item.id}")
            return
        }
        val pv = playerViewProvider() ?: run {
            Log.w(tag, "restorePlayerFromLandscape: playerView is null, skip")
            return
        }

        Log.d(tag, "restorePlayerFromLandscape: 开始恢复播放器，videoId=${item.id}")
        val sessionBeforePlay = peekPlaybackSession(item.id)
        Log.d(tag, "restorePlayerFromLandscape: 会话存在=${sessionBeforePlay != null}")
        isRestoringFromLandscape = true

        playVideo(item.id, item.videoUrl)

        pv.post {
            if (!isAddedProvider()) {
                Log.w(tag, "restorePlayerFromLandscape: Fragment not added after post")
                isRestoringFromLandscape = false
                return@post
            }

            TaskRunner.run(
                name = TaskNames.of(
                    feature = "ui_video_landscape_coordinator",
                    action = "restore_player_from_landscape"
                ),
                timeoutMs = 8_000L,
                onError = { throwable -> Log.e(tag, "restore task failed", throwable) }
            ) {
                var retryCount = 0
                val maxRetries = 50
                while (retryCount < maxRetries) {
                    val currentVideoId = currentVideoIdProvider()
                    if (currentVideoId == item.id) {
                        break
                    }
                    if (!ThreadTaskPatterns.sleepOrStop(10L)) {
                        isRestoringFromLandscape = false
                        return@run
                    }
                    retryCount++
                }

                if (!isAddedProvider()) {
                    Log.w(tag, "restorePlayerFromLandscape: Fragment not added after delay")
                    isRestoringFromLandscape = false
                    return@run
                }

                val sessionBeforeResume = peekPlaybackSession(item.id)
                Log.d(tag, "restorePlayerFromLandscape: 调用 onHostResume 前，会话存在=${sessionBeforeResume != null}")
                onHostResume(pv)
                setHasPreparedPlayer(true)

                pv.postDelayed({
                    val restoredPosition = mediaPlayerProvider()?.currentPosition ?: 0
                    Log.d(tag, "restorePlayerFromLandscape: 播放器已恢复，currentPosition=${restoredPosition}ms")
                    if (sessionBeforeResume != null) {
                        pv.postDelayed({
                            val finalPosition = mediaPlayerProvider()?.currentPosition ?: 0
                            Log.d(tag, "restorePlayerFromLandscape: 延迟检查后，currentPosition=${finalPosition}ms")
                        }, 200)
                    }
                }, 200)

                pv.postDelayed({
                    isRestoringFromLandscape = false
                    val finalPosition = mediaPlayerProvider()?.currentPosition ?: 0
                    Log.d(tag, "restorePlayerFromLandscape: 恢复完成，当前播放位置=${finalPosition}ms")
                }, 50)
            }
        }
    }

    fun restorePlayerFromUserWorksViewer() {
        if (!isAddedProvider() || videoItemProvider() == null) {
            Log.w(tag, "restorePlayerFromUserWorksViewer: Fragment not ready, skip")
            return
        }
        val item = videoItemProvider() ?: return
        if (item.type == FeedContentType.IMAGE_POST) {
            Log.d(tag, "restorePlayerFromUserWorksViewer: skip for image post ${item.id}")
            return
        }
        val pv = playerViewProvider() ?: run {
            Log.w(tag, "restorePlayerFromUserWorksViewer: playerView is null, skip")
            return
        }
        Log.d(tag, "restorePlayerFromUserWorksViewer: 恢复播放器，videoId=${item.id}")

        isRestoringFromLandscape = true
        playVideo(item.id, item.videoUrl)

        pv.post {
            if (!isAddedProvider()) {
                Log.w(tag, "restorePlayerFromUserWorksViewer: Fragment not added after post")
                isRestoringFromLandscape = false
                return@post
            }

            onHostResume(pv)
            setHasPreparedPlayer(true)
            Log.d(tag, "restorePlayerFromUserWorksViewer: 播放器已恢复，playerView.player=${pv.player}")

            pv.postDelayed({
                if (isAddedProvider() && isViewVisibleOnScreenProvider()) {
                    resumePlayback()
                }
                isRestoringFromLandscape = false
            }, 50)
        }
    }

    fun onPlayerPauseIfNeeded(pauseAction: () -> Unit) {
        if (!navigatingToLandscape && hasPreparedPlayerProvider()) {
            pauseAction()
        }
    }
}
