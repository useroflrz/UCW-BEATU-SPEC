package com.ucw.beatu.di

import android.content.Context
import com.ucw.beatu.shared.database.BeatUDatabase
import com.ucw.beatu.shared.database.dao.CommentDao
import com.ucw.beatu.shared.database.dao.SearchHistoryDao
import com.ucw.beatu.shared.database.dao.UserDao
import com.ucw.beatu.shared.database.dao.UserFollowDao
import com.ucw.beatu.shared.database.dao.VideoDao
import com.ucw.beatu.shared.database.dao.VideoInteractionDao
import com.ucw.beatu.shared.database.dao.WatchHistoryDao
import com.ucw.beatu.shared.database.datastore.PreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块
 * 提供Room数据库、DAO和DataStore的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BeatUDatabase {
        return BeatUDatabase.build(context, "beatu-db")
    }

    @Provides
    fun provideVideoDao(database: BeatUDatabase): VideoDao {
        return database.videoDao()
    }

    @Provides
    fun provideCommentDao(database: BeatUDatabase): CommentDao {
        return database.commentDao()
    }

    @Provides
    fun provideUserDao(database: BeatUDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideUserFollowDao(database: BeatUDatabase): UserFollowDao {
        return database.userFollowDao()
    }

    @Provides
    fun provideVideoInteractionDao(database: BeatUDatabase): VideoInteractionDao {
        return database.videoInteractionDao()
    }

    @Provides
    fun provideWatchHistoryDao(database: BeatUDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: BeatUDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): PreferencesDataStore {
        return PreferencesDataStore(context)
    }
}

