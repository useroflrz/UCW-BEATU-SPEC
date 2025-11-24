package com.ucw.beatu.business.landscape.presentation.di

import com.ucw.beatu.business.landscape.domain.repository.LandscapeRepository
import com.ucw.beatu.business.landscape.domain.usecase.GetLandscapeVideosUseCase
import com.ucw.beatu.business.landscape.domain.usecase.LandscapeUseCases
import com.ucw.beatu.business.landscape.domain.usecase.LoadMoreLandscapeVideosUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object LandscapePresentationModule {

    @Provides
    fun provideLandscapeUseCases(
        repository: LandscapeRepository
    ): LandscapeUseCases {
        return LandscapeUseCases(
            getLandscapeVideos = GetLandscapeVideosUseCase(repository),
            loadMoreLandscapeVideos = LoadMoreLandscapeVideosUseCase(repository)
        )
    }
}


