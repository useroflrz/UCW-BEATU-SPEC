package com.ucw.beatu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * BeatU Application 类
 * Hilt 依赖注入入口
 */
@HiltAndroidApp
class BeatUApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // 初始化逻辑可以在这里添加
        // 例如：初始化日志、性能监控、崩溃收集等
    }
}

