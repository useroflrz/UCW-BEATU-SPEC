package com.ucw.beatu.di

import com.ucw.beatu.auth.AuthLogoutImpl
import com.ucw.beatu.shared.common.auth.AuthLogout
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthLogout(impl: AuthLogoutImpl): AuthLogout
}
