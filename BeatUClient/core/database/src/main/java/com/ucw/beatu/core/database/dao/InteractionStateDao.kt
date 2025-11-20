package com.ucw.beatu.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ucw.beatu.core.database.entity.InteractionStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionStateDao {
    @Query("SELECT * FROM interaction_state WHERE videoId = :videoId")
    fun observe(videoId: String): Flow<InteractionStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: InteractionStateEntity)
}
