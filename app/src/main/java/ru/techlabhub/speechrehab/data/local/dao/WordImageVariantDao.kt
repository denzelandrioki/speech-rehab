package ru.techlabhub.speechrehab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.techlabhub.speechrehab.data.local.entity.WordImageVariantEntity

@Dao
interface WordImageVariantDao {
    @Query("SELECT * FROM word_image_variants WHERE wordId = :wordId")
    suspend fun listForWord(wordId: Long): List<WordImageVariantEntity>

    @Query("SELECT fetchSignature FROM word_image_variants WHERE wordId = :wordId")
    suspend fun fetchSignaturesForWord(wordId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: WordImageVariantEntity): Long

    @Query(
        "UPDATE word_image_variants SET wasShown = 1, lastShownAtEpochMillis = :now WHERE id = :id",
    )
    suspend fun markShown(
        id: Long,
        now: Long,
    )

    @Query("SELECT id FROM word_image_variants WHERE wordId = :wordId AND fetchSignature = :sig LIMIT 1")
    suspend fun getIdByWordAndSignature(
        wordId: Long,
        sig: String,
    ): Long?

    @Transaction
    suspend fun insertOrExistingId(entity: WordImageVariantEntity): Long {
        val rowId = insertIgnore(entity)
        if (rowId != -1L) return rowId
        return getIdByWordAndSignature(entity.wordId, entity.fetchSignature)
            ?: error("word_image_variants insert conflict but row missing")
    }
}
