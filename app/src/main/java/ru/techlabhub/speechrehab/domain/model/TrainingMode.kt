package ru.techlabhub.speechrehab.domain.model

/**
 * Режим подбора слов для следующей карточки; фильтрация пула — [ru.techlabhub.speechrehab.domain.analytics.CardWeightEngine.filterByMode].
 *
 * **Не путать с картинками:** «новое слово» здесь = по счётчику попыток в БД, не «нет иллюстрации».
 */
enum class TrainingMode {
    /** Случайно из доступного пула (с учётом категорий в настройках). */
    RANDOM,
    /** То же по смыслу, что случайный; категории задаются фильтром включённых id. */
    BY_CATEGORY,
    /** Приоритет словам с низкой исторической точностью (если список пуст — весь пул). */
    HARD_WORDS,
    /**
     * Только слова с **0** попыток в `answer_attempts` (реализация: `COUNT(*) &lt; 1`).
     * Пустой результат **без** fallback на весь словарь — см. [ru.techlabhub.speechrehab.domain.analytics.CardWeightEngine.filterByMode].
     */
    NEW_ONLY,
    /**
     * Слова с **0 или 1** попыткой (`COUNT(*) &lt; 2`). При пустом пересечении с пулом — fallback на весь включённый пул.
     */
    FRESH_WORDS,
    /** Объединение «сложных» и остальных без жёсткого ограничения. */
    MIXED,
}
