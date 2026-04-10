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
    val displayTextRu: String = "",
    val displayTextEn: String = "",
    /** Относительный путь в assets, например `bundled/table.png`; пусто — нет встроенной картинки. */
    val bundledAssetName: String = "",
    val categoryId: Long,
    val enabled: Boolean = true,
    val isCustom: Boolean = false,
    val consecutiveCorrect: Int = 0,
    val consecutiveIncorrect: Int = 0,
)
