package com.ucw.beatu.shared.common.threading

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object TaskRunner {
    private const val TAG = "TaskRunner"
    private val timeoutScheduler = Executors.newSingleThreadScheduledExecutor()

    data class Handle(
        val name: String,
        val future: Future<*>,
        val timeoutFuture: ScheduledFuture<*>?
    ) {
        fun cancel(mayInterruptIfRunning: Boolean = true) {
            timeoutFuture?.cancel(true)
            future.cancel(mayInterruptIfRunning)
        }
    }

    fun run(
        name: String,
        timeoutMs: Long? = null,
        onError: ((Throwable) -> Unit)? = null,
        block: () -> Unit
    ): Handle {
        val normalizedName = TaskNames.fromLegacy(name)
        val future = AppExecutors.submitIo {
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "Task failed: $normalizedName", t)
                onError?.invoke(t)
            }
        }
        val timeoutFuture = timeoutMs?.takeIf { it > 0L }?.let { timeout ->
            timeoutScheduler.schedule(
                { if (!future.isDone) future.cancel(true) },
                timeout,
                TimeUnit.MILLISECONDS
            )
        }
        return Handle(normalizedName, future, timeoutFuture)
    }
}
