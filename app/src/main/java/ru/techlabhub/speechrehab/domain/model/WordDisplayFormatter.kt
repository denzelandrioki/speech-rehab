package ru.techlabhub.speechrehab.domain.model

/**
 * Подпись слова для UI в зависимости от [TrainingTextLanguage].
 * Fallback: если строка пуста, используется канонический [WordItem.text].
 */
object WordDisplayFormatter {
    fun displayLabel(
        word: WordItem,
        mode: TrainingTextLanguage,
    ): String =
        when (mode) {
            TrainingTextLanguage.NONE -> ""
            TrainingTextLanguage.RUSSIAN -> word.displayTextRu.ifBlank { word.text }
            TrainingTextLanguage.ENGLISH -> word.displayTextEn.ifBlank { word.text }
            TrainingTextLanguage.BOTH -> {
                val ru = word.displayTextRu.ifBlank { word.text }
                val en = word.displayTextEn.ifBlank { word.text }
                when {
                    ru == en -> ru
                    else -> "$ru / $en"
                }
            }
        }

    /** Для списков (словарь/статистика), когда нужна одна строка при режиме BOTH — кратко. */
    fun vocabularyLine(
        word: WordItem,
        mode: TrainingTextLanguage,
    ): String =
        when (mode) {
            TrainingTextLanguage.NONE -> word.text
            TrainingTextLanguage.RUSSIAN -> word.displayTextRu.ifBlank { word.text }
            TrainingTextLanguage.ENGLISH -> word.displayTextEn.ifBlank { word.text }
            TrainingTextLanguage.BOTH ->
                "${word.displayTextRu.ifBlank { "—" }} · ${word.displayTextEn.ifBlank { word.text }}"
        }

    /** Подпись строки статистики по полям из агрегата SQL. */
    fun formatRank(
        canonicalText: String,
        displayTextRu: String,
        displayTextEn: String,
        mode: TrainingTextLanguage,
    ): String {
        val w =
            WordItem(
                id = 0L,
                text = canonicalText,
                displayTextRu = displayTextRu,
                displayTextEn = displayTextEn,
                bundledAssetName = "",
                categoryId = 0L,
                categoryName = "",
                enabled = true,
                isCustom = false,
            )
        val label = displayLabel(w, mode)
        // В статистике при NONE на карточке всё равно показываем канонический ключ, иначе строка пустая.
        return label.ifBlank { canonicalText }
    }
}
