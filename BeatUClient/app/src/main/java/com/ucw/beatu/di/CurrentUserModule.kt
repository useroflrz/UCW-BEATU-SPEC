package com.ucw.beatu.di

import com.ucw.beatu.shared.common.user.CurrentUserProvider
import com.ucw.beatu.shared.common.user.DefaultCurrentUserProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CurrentUserModule {
    @Provides
    @Singleton
    fun provideCurrentUserProvider(): CurrentUserProvider = DefaultCurrentUserProvider()
}
