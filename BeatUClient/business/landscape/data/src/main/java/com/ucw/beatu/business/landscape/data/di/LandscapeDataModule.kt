package com.ucw.beatu.business.landscape.data.di

import com.ucw.beatu.business.landscape.data.repository.LandscapeRepositoryImpl
import com.ucw.beatu.business.landscape.domain.repository.LandscapeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LandscapeDataModule {

    @Binds
    @Singleton
    abstract fun bindLandscapeRepository(
        impl: LandscapeRepositoryImpl
    ): LandscapeRepository
}


