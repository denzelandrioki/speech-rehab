package ru.techlabhub.speechrehab.domain.model

/**
 * Как подбирать **иллюстрацию** для уже выбранного слова (не путать с [TrainingMode] / «новые слова»).
 */
enum class ImageRotationMode {
    /**
     * Как раньше: встроенный asset → любой сохранённый локальный файл → сеть только если локально пусто
     * (и политика [UserTrainingPreferences.refreshRemoteWhenNoLocalImage] разрешает).
     */
    REUSE_LOCAL_FIRST,

    /**
     * После локальных asset стараться взять **новый** URL из API относительно уже сохранённых вариантов;
     * если не вышло — показать ранее сохранённый файл.
     * Семантика совпадает с [ALWAYS_TRY_NEW_REMOTE] (разные подписи в UI для будущего уточнения поведения).
     */
    PREFER_NEW_REMOTE,

    /**
     * При доступной сети и разрешённых источниках сначала запрос к API за вариантом, которого ещё нет в БД;
     * иначе fallback на локально сохранённые варианты / placeholder.
     */
    ALWAYS_TRY_NEW_REMOTE,
}
