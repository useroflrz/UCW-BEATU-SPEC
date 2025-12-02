package com.ucw.beatu.shared.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ucw.beatu.shared.database.converter.Converters
import com.ucw.beatu.shared.database.dao.CommentDao
import com.ucw.beatu.shared.database.dao.UserDao
import com.ucw.beatu.shared.database.dao.UserVideoRelationDao
import com.ucw.beatu.shared.database.dao.UserFollowDao
import com.ucw.beatu.shared.database.dao.UserInteractionDao
import com.ucw.beatu.shared.database.dao.VideoDao
import com.ucw.beatu.shared.database.dao.WatchHistoryDao
import com.ucw.beatu.shared.database.entity.CommentEntity
import com.ucw.beatu.shared.database.entity.UserEntity
import com.ucw.beatu.shared.database.entity.UserVideoRelationEntity
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import com.ucw.beatu.shared.database.entity.UserInteractionEntity
import com.ucw.beatu.shared.database.entity.VideoEntity
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity

@Database(
    entities = [
        VideoEntity::class,
        CommentEntity::class,
        UserEntity::class,
        UserVideoRelationEntity::class,
        UserFollowEntity::class,
        UserInteractionEntity::class,
        WatchHistoryEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BeatUDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun userVideoRelationDao(): UserVideoRelationDao
    abstract fun userFollowDao(): UserFollowDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        fun build(context: Context, dbName: String = "beatu-db"): BeatUDatabase =
            Room.databaseBuilder(context, BeatUDatabase::class.java, dbName)
                .fallbackToDestructiveMigration()
                .build()
    }
}

