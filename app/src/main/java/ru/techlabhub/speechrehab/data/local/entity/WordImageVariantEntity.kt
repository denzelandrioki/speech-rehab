package ru.techlabhub.speechrehab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Один сохранённый вариант иллюстрации для слова (несколько строк на [wordId]).
 * Дубликаты одного и того же remote-объекта отсекаются по [fetchSignature].
 */
@Entity(
    tableName = "word_image_variants",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["wordId"]),
        Index(value = ["wordId", "fetchSignature"], unique = true),
    ],
)
data class WordImageVariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val remoteUrl: String,
    val localFilePath: String,
    val sourceName: String,
    /** Уникальный ключ варианта в пределах слова (провайдер + url или внешний id). */
    val fetchSignature: String,
    val wasShown: Boolean,
    val lastShownAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)
