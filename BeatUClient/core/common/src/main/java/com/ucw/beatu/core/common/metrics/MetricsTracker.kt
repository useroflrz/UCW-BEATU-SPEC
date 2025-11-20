package com.ucw.beatu.core.common.metrics

import com.ucw.beatu.core.common.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MetricsTracker(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val sink: suspend (PlaybackMetrics) -> Unit
) {
    private val mutex = Mutex()

    fun track(metrics: PlaybackMetrics) {
        scope.launch {
            mutex.withLock {
                AppLogger.d(TAG, "track metrics: $metrics")
                sink(metrics)
            }
        }
    }

    companion object {
        private const val TAG = "MetricsTracker"
    }
}
