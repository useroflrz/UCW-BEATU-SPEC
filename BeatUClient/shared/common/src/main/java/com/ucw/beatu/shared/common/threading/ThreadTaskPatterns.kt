package com.ucw.beatu.shared.common.threading

object ThreadTaskPatterns {
    fun sleepOrStop(durationMs: Long): Boolean {
        return try {
            Thread.sleep(durationMs)
            !Thread.currentThread().isInterrupted
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    fun waitUntil(
        timeoutMs: Long,
        intervalMs: Long = 50L,
        condition: () -> Boolean
    ): Boolean {
        var waited = 0L
        while (waited < timeoutMs) {
            if (condition()) return true
            if (!sleepOrStop(intervalMs)) return false
            waited += intervalMs
        }
        return condition()
    }
}
