package com.ucw.beatu

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * BeatU Application 类
 * Hilt 依赖注入入口
 */
@HiltAndroidApp
class BeatUApp : Application() {
    
    companion object {
        private const val TAG = "BeatUApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Application started")
        
        try {
            // 初始化逻辑可以在这里添加
            // 例如：初始化日志、性能监控、崩溃收集等
            Log.d(TAG, "onCreate: Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error initializing application", e)
            throw e
        }
    }
}

