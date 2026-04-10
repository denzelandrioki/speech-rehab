package ru.techlabhub.speechrehab.domain.model

/**
 * Разрешение загрузки картинок из сети (поверх локальных источников).
 * [WIFI_ONLY] — только Wi‑Fi; на сотовой сети удалённые источники пропускаются (TODO: доработать политику).
 */
enum class OnlineImageFetchingMode {
    ENABLED,
    DISABLED,
    WIFI_ONLY,
}
