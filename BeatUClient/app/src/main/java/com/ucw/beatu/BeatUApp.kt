package com.ucw.beatu

import android.app.Application
import android.util.Log
import com.ucw.beatu.business.user.presentation.router.UserProfileRouterImpl
import com.ucw.beatu.business.videofeed.presentation.router.VideoItemRouterImpl
import com.ucw.beatu.shared.router.RouterRegistry
import com.ucw.beatu.startup.AppStartupDataLoader
import com.ucw.beatu.sync.DataSyncService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BeatU Application 类
 * Hilt 依赖注入入口
 */
@HiltAndroidApp
class BeatUApp : Application() {

    companion object {
        private const val TAG = "BeatUApp"
    }

    @Inject
    lateinit var dataSyncService: DataSyncService

    @Inject
    lateinit var appStartupDataLoader: AppStartupDataLoader

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 注册 Router 实现
     * 通过 RouterRegistry 注册各个模块的 Router 实现，解决模块间循环依赖
     */
    private fun registerRouters() {
        RouterRegistry.registerUserProfileRouter(UserProfileRouterImpl())
        RouterRegistry.registerVideoItemRouter(VideoItemRouterImpl())
        Log.d(TAG, "registerRouters: Routers registered successfully")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Application started")
        
        try {
            // 初始化逻辑可以在这里添加
            // 例如：初始化日志、性能监控、崩溃收集等
            // 注册 Router 实现，解决模块间循环依赖
            registerRouters()
            // 启动数据同步服务（同步待同步的数据）
            dataSyncService.startSync()
            // 启动应用启动数据加载器（按照策略加载数据）
            appStartupDataLoader.startLoading()
            Log.d(TAG, "onCreate: Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error initializing application", e)
            throw e
        }
    }

}

