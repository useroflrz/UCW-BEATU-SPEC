package com.ucw.beatu.shared.common.threading

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

object AppExecutors {
    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun io(task: () -> Unit) {
        ioExecutor.execute(task)
    }

    fun main(task: () -> Unit) {
        mainHandler.post(task)
    }

    fun submitIo(task: () -> Unit): Future<*> {
        return ioExecutor.submit(task)
    }
}
