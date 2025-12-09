package com.ucw.beatu.business.user.presentation.ui.helper

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.ucw.beatu.business.user.presentation.viewmodel.UserProfileViewModel
import com.ucw.beatu.shared.designsystem.util.IOSButtonEffect

/**
 * 管理用户主页的关注按钮逻辑
 */
class UserProfileFollowButtonManager(
    private val fragment: Fragment,
    private val btnFollow: MaterialButton,
    private val viewModel: UserProfileViewModel,
    private val getTargetUserId: () -> String?,
    private val getCurrentUserId: () -> String
) {
    companion object {
        private const val TAG = "UserProfileFollowButtonManager"
    }

    /**
     * 设置关注按钮
     */
    fun setupFollowButton() {
        IOSButtonEffect.applyIOSEffect(btnFollow) {
            val targetUserId = getTargetUserId()
            if (targetUserId == null) {
                Log.e(TAG, "Cannot follow/unfollow: user ID is null")
                Toast.makeText(fragment.requireContext(), "无法获取用户信息", Toast.LENGTH_SHORT).show()
                return@applyIOSEffect
            }
            val currentUserId = getCurrentUserId()
            Log.d(TAG, "Follow button clicked, userId: $targetUserId, currentUserId: $currentUserId")
            val isFollowing = viewModel.isFollowing.value ?: false
            Log.d(TAG, "Current follow status: $isFollowing")
            if (isFollowing) {
                Log.d(TAG, "Unfollowing user: $targetUserId")
                viewModel.unfollowUser(targetUserId, currentUserId)
            } else {
                Log.d(TAG, "Following user: $targetUserId")
                viewModel.followUser(targetUserId, currentUserId)
            }
        }
        // 确保按钮可以接收点击事件
        btnFollow.isClickable = true
        btnFollow.isFocusable = true
        btnFollow.isFocusableInTouchMode = true
        btnFollow.isEnabled = true
        // 确保按钮在最上层，不被其他视图遮挡
        btnFollow.bringToFront()
        Log.d(TAG, "Follow button setup completed, visibility: ${btnFollow.visibility}, clickable: ${btnFollow.isClickable}, enabled: ${btnFollow.isEnabled}")
    }

    /**
     * 更新关注按钮状态
     * @param isFollowing 是否已关注
     * @param isInteracting 是否正在交互中（防止重复点击）
     */
    fun updateFollowButton(isFollowing: Boolean?, isInteracting: Boolean = false) {
        // ✅ 修复：乐观更新 - 即使 isInteracting = true，也保持按钮可用，只防止重复点击
        // isInteracting 只用于防止重复点击，不用于禁用按钮
        val isEnabled = isFollowing != null
        when (isFollowing) {
            true -> {
                // ✅ 修复：如果正在同步，显示"取消关注"文本，让用户知道状态已更新
                btnFollow.text = if (isInteracting) "取消关注..." else "取消关注"
                btnFollow.isEnabled = isEnabled
                btnFollow.isClickable = isEnabled && !isInteracting  // ✅ 修复：isInteracting 时禁用点击，防止重复操作
                btnFollow.alpha = if (isEnabled) 1.0f else 0.5f
            }
            false -> {
                // ✅ 修复：如果正在同步，显示"关注"文本，让用户知道状态已更新
                btnFollow.text = if (isInteracting) "关注..." else "关注"
                btnFollow.isEnabled = isEnabled
                btnFollow.isClickable = isEnabled && !isInteracting  // ✅ 修复：isInteracting 时禁用点击，防止重复操作
                btnFollow.alpha = if (isEnabled) 1.0f else 0.5f
            }
            null -> {
                btnFollow.text = "关注"
                btnFollow.isEnabled = false
                btnFollow.isClickable = false
                btnFollow.alpha = 0.5f
            }
        }
        // 确保按钮可以接收点击事件
        btnFollow.bringToFront()
        Log.d(TAG, "Follow button updated: text=${btnFollow.text}, enabled=${btnFollow.isEnabled}, clickable=${btnFollow.isClickable}, isInteracting=$isInteracting")
    }
}

