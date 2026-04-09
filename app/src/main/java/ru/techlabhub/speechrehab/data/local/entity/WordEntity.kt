package ru.techlabhub.speechrehab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index("enabled")],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    /** Русская подпись; пустая строка — в UI используется [text]. */
    val displayText: String = "",
    val categoryId: Long,
    val enabled: Boolean = true,
    val isCustom: Boolean = false,
    /** Серия подряд правильных ответов (для веса; обновляется при записи попытки). */
    val consecutiveCorrect: Int = 0,
    /** Серия подряд неправильных ответов. */
    val consecutiveIncorrect: Int = 0,
)
