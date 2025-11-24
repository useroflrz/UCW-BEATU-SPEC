package com.ucw.beatu.business.landscape.domain.usecase

import com.ucw.beatu.business.landscape.domain.repository.LandscapeRepository
class LoadMoreLandscapeVideosUseCase(
    private val repository: LandscapeRepository
) {
    operator fun invoke(page: Int, limit: Int) = repository.loadMoreLandscapeVideos(page, limit)
}


