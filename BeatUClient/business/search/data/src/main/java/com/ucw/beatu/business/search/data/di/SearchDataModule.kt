package com.ucw.beatu.business.search.data.di

import com.ucw.beatu.business.search.data.api.AISearchApiService
import com.ucw.beatu.business.search.data.repository.AISearchRepositoryImpl
import com.ucw.beatu.business.search.data.repository.SearchRepositoryImpl
import com.ucw.beatu.business.search.domain.repository.AISearchRepository
import com.ucw.beatu.business.search.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Search Data 模块的 Hilt 依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchDataModule {
    
    @Binds
    @Singleton
    abstract fun bindAISearchRepository(
        impl: AISearchRepositoryImpl
    ): AISearchRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository
}

