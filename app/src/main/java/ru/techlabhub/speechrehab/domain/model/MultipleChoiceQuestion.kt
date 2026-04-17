package ru.techlabhub.speechrehab.domain.model

/**
 * Один вопрос самостоятельного режима: картинка + 4 варианта (подписи уже под [TrainingTextLanguage]).
 */
data class MultipleChoiceOption(
    val word: WordItem,
    val label: String,
)

data class MultipleChoiceQuestion(
    val imageCard: ImageCard,
    val correctWord: WordItem,
    val options: List<MultipleChoiceOption>,
    /** Язык подписей вариантов после обработки NONE → RUSSIAN. */
    val displayLanguageEffective: TrainingTextLanguage,
)
