package com.ucw.beatu.shared.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ucw.beatu.shared.database.converter.Converters
import com.ucw.beatu.shared.database.dao.CommentDao
import com.ucw.beatu.shared.database.dao.UserDao
import com.ucw.beatu.shared.database.dao.UserFollowDao
import com.ucw.beatu.shared.database.dao.VideoDao
import com.ucw.beatu.shared.database.dao.VideoInteractionDao
import com.ucw.beatu.shared.database.dao.WatchHistoryDao
import com.ucw.beatu.shared.database.dao.SearchHistoryDao
import com.ucw.beatu.shared.database.entity.CommentEntity
import com.ucw.beatu.shared.database.entity.UserEntity
import com.ucw.beatu.shared.database.entity.UserFollowEntity
import com.ucw.beatu.shared.database.entity.VideoEntity
import com.ucw.beatu.shared.database.entity.VideoInteractionEntity
import com.ucw.beatu.shared.database.entity.WatchHistoryEntity
import com.ucw.beatu.shared.database.entity.SearchHistoryEntity

@Database(
    entities = [
        VideoEntity::class,
        CommentEntity::class,
        UserEntity::class,
        UserFollowEntity::class,
        VideoInteractionEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 9,  // ✅ 修改：数据库版本从 8 升级到 9（为 WatchHistoryEntity 添加 isPending 字段）
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BeatUDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun userFollowDao(): UserFollowDao
    abstract fun videoInteractionDao(): VideoInteractionDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        fun build(context: Context, dbName: String = "beatu-db"): BeatUDatabase =
            Room.databaseBuilder(context, BeatUDatabase::class.java, dbName)
                .fallbackToDestructiveMigration()
                .build()
    }
}

