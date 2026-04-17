package ru.techlabhub.speechrehab.ui.navigation

/**
 * Строковые идентификаторы маршрутов для Jetpack Navigation (Compose).
 *
 * Используются в [NavHost] как `startDestination` и в `navigate()` / `composable(...)`.
 * Значения — простые литералы без аргументов; при добавлении параметров понадобятся шаблоны вида `"user/{id}"`.
 */
object Routes {
    const val Home = "home"
    const val Training = "training"
    const val MultipleChoiceTraining = "multiple_choice_training"
    const val Statistics = "statistics"
    const val Settings = "settings"
    const val Vocabulary = "vocabulary"
}
