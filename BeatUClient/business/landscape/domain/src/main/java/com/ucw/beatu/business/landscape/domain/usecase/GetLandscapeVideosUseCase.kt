package com.ucw.beatu.business.landscape.domain.usecase

import com.ucw.beatu.business.landscape.domain.repository.LandscapeRepository
class GetLandscapeVideosUseCase(
    private val repository: LandscapeRepository
) {
    operator fun invoke(page: Int, limit: Int) = repository.getLandscapeVideos(page, limit)
}


