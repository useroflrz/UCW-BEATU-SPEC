package com.ucw.beatu.core.common.time

class Stopwatch {
    private var startTime: Long = 0L

    fun start() {
        startTime = System.nanoTime()
    }

    fun elapsedMillis(): Long = if (startTime == 0L) 0L else (System.nanoTime() - startTime) / 1_000_000
}
