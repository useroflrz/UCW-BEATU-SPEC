package com.ucw.beatu.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ucw.beatu.core.database.converter.Converters
import com.ucw.beatu.core.database.dao.CommentDao
import com.ucw.beatu.core.database.dao.InteractionStateDao
import com.ucw.beatu.core.database.dao.VideoDao
import com.ucw.beatu.core.database.entity.CommentEntity
import com.ucw.beatu.core.database.entity.InteractionStateEntity
import com.ucw.beatu.core.database.entity.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        CommentEntity::class,
        InteractionStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BeatUDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun commentDao(): CommentDao
    abstract fun interactionStateDao(): InteractionStateDao

    companion object {
        fun build(context: Context, dbName: String = "beatu-db"): BeatUDatabase =
            Room.databaseBuilder(context, BeatUDatabase::class.java, dbName)
                .fallbackToDestructiveMigration()
                .build()
    }
}
