package ru.techlabhub.speechrehab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.techlabhub.speechrehab.data.local.entity.CachedImageEntity

@Dao
interface CachedImageDao {
    @Query("SELECT * FROM cached_images WHERE wordId = :wordId LIMIT 1")
    suspend fun getForWord(wordId: Long): CachedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedImageEntity)
}
