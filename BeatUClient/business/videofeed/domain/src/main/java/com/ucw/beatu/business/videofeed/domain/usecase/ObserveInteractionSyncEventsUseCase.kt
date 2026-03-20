package com.ucw.beatu.business.videofeed.domain.usecase

import com.ucw.beatu.business.videofeed.domain.model.InteractionSyncEvent
import com.ucw.beatu.business.videofeed.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 观察互动异步同步事件（失败回滚/提示）的 UseCase。
 */
class ObserveInteractionSyncEventsUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(): Flow<InteractionSyncEvent> = repository.observeInteractionSyncEvents()
}

