package com.ucw.beatu.business.user.presentation.router

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ucw.beatu.business.user.presentation.ui.UserProfileFragment
import com.ucw.beatu.shared.router.UserProfileRouter

/**
 * UserProfileRouter 的实现
 * 将 UserProfileFragment 的创建逻辑封装为 Router 接口实现
 */
class UserProfileRouterImpl : UserProfileRouter {
    override fun createUserProfileFragment(userId: String, authorName: String, readOnly: Boolean): Fragment {
        // 如果authorName看起来像用户ID（纯数字），则使用userId；否则使用authorName作为用户名
        val userName = if (authorName.isNotEmpty() && !authorName.all { it.isDigit() }) {
            // authorName不是纯数字，可能是用户名
            authorName
        } else {
            // authorName是纯数字或为空，使用userId
            userId
        }
        return UserProfileFragment.newInstance(userName, readOnly)
    }
    
    override fun createUserProfileFragmentWithData(userData: Bundle, readOnly: Boolean): Fragment {
        return UserProfileFragment.newInstanceWithData(userData, readOnly)
    }
}

