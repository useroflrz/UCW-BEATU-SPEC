package com.ucw.beatu.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

/**
 * 存储空间监控：检测可用空间，低时提醒清理缓存。
 */
object StorageMonitor {

    private const val TAG = "StorageMonitor"

    /** 低空间阈值（MB），低于此值提醒用户 */
    private const val LOW_SPACE_THRESHOLD_MB = 2048L  // 2GB

    /**
     * 检查内部存储可用空间是否低于阈值
     */
    fun isStorageLow(context: Context): Boolean {
        return try {
            val statFs = StatFs(context.cacheDir.absolutePath)
            val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            val availableMb = availableBytes / (1024 * 1024)
            availableMb < LOW_SPACE_THRESHOLD_MB
        } catch (e: Exception) {
            Log.w(TAG, "检查存储空间失败", e)
            false
        }
    }

    /**
     * 获取可用空间（MB）
     */
    fun getAvailableSpaceMb(context: Context): Long {
        return try {
            val statFs = StatFs(context.cacheDir.absolutePath)
            val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            availableBytes / (1024 * 1024)
        } catch (e: Exception) {
            Log.w(TAG, "获取存储空间失败", e)
            0L
        }
    }

    /**
     * 获取缓存目录大小（MB），供清理提示参考
     */
    fun getCacheDirSizeMb(context: Context): Long {
        return try {
            dirSize(context.cacheDir) / (1024 * 1024)
        } catch (e: Exception) {
            Log.w(TAG, "获取缓存大小失败", e)
            0L
        }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        return dir.listFiles()?.sumOf { file ->
            if (file.isDirectory) dirSize(file) else file.length()
        } ?: 0L
    }
}
