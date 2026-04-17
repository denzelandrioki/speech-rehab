package ru.techlabhub.speechrehab.domain.model

/**
 * Подписи вариантов в режиме multiple choice.
 *
 * **Отдельно от языка UI:** используется [TrainingTextLanguage] из настроек карточки.
 * [TrainingTextLanguage.NONE]: для выбора из 4 текстов обязателен — fallback на [TrainingTextLanguage.RUSSIAN].
 * [TrainingTextLanguage.BOTH]: «ru / en» как на карточке.
 */
object ChoiceOptionLabelFormatter {
    fun effectiveChoiceLanguage(mode: TrainingTextLanguage): TrainingTextLanguage =
        when (mode) {
            TrainingTextLanguage.NONE -> TrainingTextLanguage.RUSSIAN
            else -> mode
        }

    fun optionLabel(
        word: WordItem,
        cardTextMode: TrainingTextLanguage,
    ): String {
        val base = WordDisplayFormatter.displayLabel(word, effectiveChoiceLanguage(cardTextMode)).trim()
        // Пустая подпись даёт «пустую кнопку» в UI; канонический text — последний fallback.
        return base.ifBlank { word.text.trim().ifBlank { "—" } }
    }
}
