package com.ucw.beatu.shared.router

/**
 * Router 注册表
 * 用于在运行时注册和获取 Router 实例，避免编译时依赖
 * 
 * 使用方式：
 * 1. 在 app 模块启动时注册 Router 实现
 * 2. 在需要的地方通过 RouterRegistry 获取 Router 实例
 */
object RouterRegistry {
    private var userProfileRouter: UserProfileRouter? = null
    private var videoItemRouter: VideoItemRouter? = null
    private var userWorksViewerRouter: UserWorksViewerRouter? = null
    
    /**
     * 注册 UserProfileRouter 实现
     */
    fun registerUserProfileRouter(router: UserProfileRouter) {
        userProfileRouter = router
    }
    
    /**
     * 注册 VideoItemRouter 实现
     */
    fun registerVideoItemRouter(router: VideoItemRouter) {
        videoItemRouter = router
    }
    
    /**
     * 注册 UserWorksViewerRouter 实现
     */
    fun registerUserWorksViewerRouter(router: UserWorksViewerRouter?) {
        userWorksViewerRouter = router
    }
    
    /**
     * 获取 UserProfileRouter 实例
     */
    fun getUserProfileRouter(): UserProfileRouter? = userProfileRouter
    
    /**
     * 获取 VideoItemRouter 实例
     */
    fun getVideoItemRouter(): VideoItemRouter? = videoItemRouter
    
    /**
     * 获取 UserWorksViewerRouter 实例
     */
    fun getUserWorksViewerRouter(): UserWorksViewerRouter? = userWorksViewerRouter
}

