package com.ucw.beatu.business.user.presentation.ui.helper

import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.presentation.ui.UserWorksViewerFragment
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.router.UserProfileVideoClickHost

/**
 * 管理用户主页的导航相关逻辑
 */
class UserProfileNavigationHelper(
    private val fragment: Fragment,
    private val isReadOnly: Boolean,
    private val getUser: () -> com.ucw.beatu.business.user.domain.model.User?,
    private val getUserWorks: () -> List<UserWork>
) {
    companion object {
        private const val TAG = "UserProfileNavigationHelper"
    }

    /**
     * 导航到用户作品播放器
     */
    fun navigateToUserWorksViewer(selectedWorkId: Long) {  // ✅ 修改：从 String 改为 Long
        val works = getUserWorks()
        if (works.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "暂无可播放的视频", Toast.LENGTH_SHORT).show()
            return
        }

        val authorName = getUser()?.name ?: "BeatU 用户"
        val videoItems = ArrayList(works.map { it.toVideoItem(authorName) })
        val initialIndex = works.indexOfFirst { it.id == selectedWorkId }.let { index ->
            if (index == -1) 0 else index
        }

        // 在只读模式下（从DialogFragment中显示），通过接口回调父 Fragment；否则使用findNavController()导航
        if (isReadOnly) {
            val host = fragment.parentFragment as? UserProfileVideoClickHost
            if (host != null) {
                val currentUserId = getUser()?.id ?: "BEATU"
                host.onUserWorkClicked(currentUserId, authorName, videoItems, initialIndex)
                Log.d(TAG, "Notified parent fragment via UserProfileVideoClickHost")
            } else {
                Log.e(TAG, "Parent fragment does not implement UserProfileVideoClickHost")
                Toast.makeText(fragment.requireContext(), "无法打开视频播放器: 父Fragment未实现回调接口", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 非只读模式，使用findNavController()导航
            val navController = runCatching { fragment.findNavController() }.getOrNull()
            if (navController == null) {
                Log.e(TAG, "NavController not found, cannot open user works viewer")
                return
            }

            val actionId = NavigationHelper.getResourceId(
                fragment.requireContext(),
                NavigationIds.ACTION_USER_PROFILE_TO_USER_WORKS_VIEWER
            )
            if (actionId == 0) {
                Log.e(TAG, "Navigation action not found for user works viewer")
                return
            }

            val currentUserId = getUser()?.id ?: "current_user"
            // ✅ 修复：记录来源页面 ID，用于返回时决定返回到哪里
            val currentDestinationId = navController.currentDestination?.id ?: 0
            val bundle = bundleOf(
                UserWorksViewerFragment.ARG_USER_ID to currentUserId,
                UserWorksViewerFragment.ARG_INITIAL_INDEX to initialIndex,
                UserWorksViewerFragment.ARG_VIDEO_LIST to videoItems,
                UserWorksViewerFragment.ARG_SOURCE_DESTINATION to currentDestinationId // ✅ 修复：传递来源页面 ID
            )
            Log.d(TAG, "navigateToUserWorksViewer: 传递来源页面 ID=$currentDestinationId")
            navController.navigate(actionId, bundle)
        }
    }

    /**
     * 将 UserWork 转换为 VideoItem
     */
    private fun UserWork.toVideoItem(authorName: String): VideoItem {
        // ✅ 修复：从 UserWork 中读取真实的 orientation，而不是硬编码为 PORTRAIT
        val videoOrientation = when (orientation.uppercase()) {
            "LANDSCAPE", "HORIZONTAL" -> VideoOrientation.LANDSCAPE
            "PORTRAIT", "VERTICAL" -> VideoOrientation.PORTRAIT
            else -> VideoOrientation.PORTRAIT  // 默认值
        }
        return VideoItem(
            id = id, // ✅ 修改：直接使用 Long，无需转换
            videoUrl = playUrl,
            title = title,
            authorName = authorName,
            likeCount = likeCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            commentCount = commentCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            favoriteCount = favoriteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            shareCount = shareCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            orientation = videoOrientation  // ✅ 修复：使用真实的 orientation 值
        )
    }
}

