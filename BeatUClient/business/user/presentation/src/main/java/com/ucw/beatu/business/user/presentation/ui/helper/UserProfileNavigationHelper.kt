package com.ucw.beatu.business.user.presentation.ui.helper

import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ucw.beatu.business.user.domain.model.UserWork
import com.ucw.beatu.business.user.presentation.ui.UserWorksViewerFragment
import com.ucw.beatu.shared.common.model.VideoItem
import com.ucw.beatu.shared.common.model.VideoOrientation
import com.ucw.beatu.shared.common.navigation.NavigationHelper
import com.ucw.beatu.shared.common.navigation.NavigationIds
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.router.UserProfileVideoClickHost
import kotlinx.coroutines.launch

/**
 * 管理用户主页的导航相关逻辑
 */
class UserProfileNavigationHelper(
    private val fragment: Fragment,
    private val isReadOnly: Boolean,
    private val getUser: () -> com.ucw.beatu.business.user.domain.model.User?,
    private val getUserWorks: () -> List<UserWork>,
    private val database: BeatUDatabase? = null  // ✅ 新增：数据库依赖，用于查询用户视频交互状态
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

        val currentUserId = getUser()?.id ?: "BEATU"
        
        // ✅ 修复：使用协程查询用户视频交互状态，并转换为 VideoItem
        fragment.lifecycleScope.launch {
            val videoItems = ArrayList<VideoItem>()
            val interactionDao = database?.videoInteractionDao()
            val userDao = database?.userDao()
            
            works.forEach { work ->
                // ✅ 修复：通过视频的 authorId 查询用户信息，获取正确的 authorName
                val videoAuthorId = work.authorId.ifEmpty { currentUserId }
                var videoAuthorName = "BeatU 用户"
                var videoAuthorAvatar = work.authorAvatar
                
                // 通过 authorId 查询用户信息
                if (videoAuthorId.isNotEmpty()) {
                    try {
                        val userEntity = userDao?.getUserById(videoAuthorId)
                        if (userEntity != null) {
                            videoAuthorName = userEntity.userName
                            // 如果 work.authorAvatar 为空，使用用户表中的 avatarUrl
                            if (videoAuthorAvatar.isNullOrBlank() && !userEntity.avatarUrl.isNullOrBlank()) {
                                videoAuthorAvatar = userEntity.avatarUrl
                            }
                            Log.d(TAG, "通过authorId查询到用户信息: authorId=$videoAuthorId, authorName=$videoAuthorName")
                        } else {
                            Log.w(TAG, "未查询到用户信息: authorId=$videoAuthorId，使用默认值")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "查询用户信息失败: authorId=$videoAuthorId", e)
                    }
                }
                
                // 查询用户视频交互状态
                val interaction = interactionDao?.getInteraction(work.id, currentUserId)
                val isLiked = interaction?.isLiked ?: false
                val isFavorited = interaction?.isFavorited ?: false
                
                // 转换为 VideoItem，包含点赞/收藏状态和用户信息
                val videoItem = work.toVideoItem(
                    authorName = videoAuthorName,  // ✅ 修复：使用通过 videoId 查询到的 authorName
                    authorId = videoAuthorId,
                    authorAvatar = videoAuthorAvatar,
                    isLiked = isLiked,
                    isFavorited = isFavorited
                )
                videoItems.add(videoItem)
            }
            
            val initialIndex = works.indexOfFirst { it.id == selectedWorkId }.let { index ->
                if (index == -1) 0 else index
            }
            
            // 继续导航逻辑（authorName 不再需要，因为每个 VideoItem 都有自己的 authorName）
            navigateToUserWorksViewerInternal(videoItems, initialIndex, currentUserId, "")
        }
    }
    
    /**
     * 内部导航方法（在协程中调用）
     */
    private fun navigateToUserWorksViewerInternal(
        videoItems: ArrayList<VideoItem>,
        initialIndex: Int,
        currentUserId: String,
        authorName: String
    ) {

        // 在只读模式下（从DialogFragment中显示），通过接口回调父 Fragment；否则使用findNavController()导航
        if (isReadOnly) {
            val host = fragment.parentFragment as? UserProfileVideoClickHost
            if (host != null) {
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
     * ✅ 修复：添加 authorId、authorAvatar、isLiked、isFavorited 参数
     */
    private fun UserWork.toVideoItem(
        authorName: String,
        authorId: String,
        authorAvatar: String?,
        isLiked: Boolean,
        isFavorited: Boolean
    ): VideoItem {
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
            authorId = authorId,  // ✅ 新增：使用传入的 authorId
            authorAvatar = authorAvatar,  // ✅ 新增：使用传入的 authorAvatar
            likeCount = likeCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            commentCount = commentCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            favoriteCount = favoriteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            shareCount = shareCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            isLiked = isLiked,  // ✅ 新增：使用查询到的点赞状态
            isFavorited = isFavorited,  // ✅ 新增：使用查询到的收藏状态
            orientation = videoOrientation  // ✅ 修复：使用真实的 orientation 值
        )
    }
}

