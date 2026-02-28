package com.ucw.beatu.shared.common.threading

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

object SuspendBridge {
    fun <T> runBlocking(block: suspend () -> T): T {
        val latch = CountDownLatch(1)
        val valueRef = AtomicReference<T>()
        val errorRef = AtomicReference<Throwable?>()

        block.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                result.onSuccess { valueRef.set(it) }
                result.onFailure { errorRef.set(it) }
                latch.countDown()
            }
        })

        latch.await()
        errorRef.get()?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return valueRef.get() as T
    }
}
