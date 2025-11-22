package com.ucw.beatu.shared.common.navigation

import android.content.Context
import androidx.navigation.NavController

/**
 * Navigation 辅助函数
 * 用于在 business 模块中通过字符串 ID 进行导航
 */
object NavigationHelper {
    /**
     * 通过字符串 ID 获取资源 ID
     * @param context Context 对象
     * @param resourceName 资源名称（如 "action_feed_to_userProfile"）
     * @param resourceType 资源类型（如 "id"）
     * @param packageName 包名（通常是 app 模块的包名）
     * @return 资源 ID，如果找不到则返回 0
     */
    fun getResourceId(
        context: Context,
        resourceName: String,
        resourceType: String = "id",
        packageName: String = "com.ucw.beatu"
    ): Int {
        return context.resources.getIdentifier(
            resourceName,
            resourceType,
            packageName
        )
    }
    
    /**
     * 通过字符串 ID 进行导航
     * @param navController NavController 对象
     * @param actionId 动作 ID 字符串（如 "action_feed_to_userProfile"）
     * @param context Context 对象
     */
    fun navigateByStringId(
        navController: NavController,
        actionId: String,
        context: Context
    ) {
        val resourceId = getResourceId(context, actionId)
        if (resourceId != 0) {
            navController.navigate(resourceId)
        } else {
            throw IllegalArgumentException("Navigation action not found: $actionId")
        }
    }
}
