package ru.techlabhub.speechrehab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "multiple_choice_attempts",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionWordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedWordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("questionWordId"),
        Index("sessionId"),
        Index("answeredAtEpochMillis"),
        Index("categoryId"),
    ],
)
data class MultipleChoiceAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long?,
    val questionWordId: Long,
    val categoryId: Long,
    val shownAtEpochMillis: Long,
    val answeredAtEpochMillis: Long,
    val responseTimeMillis: Long,
    val selectedWordId: Long,
    val isCorrect: Boolean,
    /** [ru.techlabhub.speechrehab.domain.model.TrainingTextLanguage] после [ChoiceOptionLabelFormatter.effectiveChoiceLanguage]. */
    val displayLanguageEffective: String,
    val imageVariantId: Long? = null,
    /** Подпись выбранной кнопки на момент ответа (для отчётов при смене словаря). */
    val selectedLabelSnapshot: String,
)
