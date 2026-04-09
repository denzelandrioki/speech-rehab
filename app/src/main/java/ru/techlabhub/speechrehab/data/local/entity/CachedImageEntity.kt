package ru.techlabhub.speechrehab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_images",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["wordId"], unique = true)],
)
data class CachedImageEntity(
    @PrimaryKey val wordId: Long,
    val remoteUrl: String,
    val localFilePath: String,
    val sourceName: String,
    val updatedAtEpochMillis: Long,
)
