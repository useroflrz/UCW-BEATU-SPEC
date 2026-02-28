package com.ucw.beatu.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 存储空间低广播接收器：监听系统存储空间低事件。
 * 部分机型仍支持此广播，作为存储监控的补充。
 */
class StorageLowReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_DEVICE_STORAGE_LOW) return
        Log.d(TAG, "收到存储空间低广播")
        StorageLowNotifyHelper.notifyIfLow(context)
    }

    companion object {
        private const val TAG = "StorageLowReceiver"
    }
}
