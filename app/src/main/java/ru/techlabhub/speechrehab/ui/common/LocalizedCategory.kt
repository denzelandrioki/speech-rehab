package ru.techlabhub.speechrehab.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.techlabhub.speechrehab.R

/**
 * Возвращает русское название категории для отображения.
 *
 * В базе у сидов категорий имена на английском (как в каталоге словаря); здесь они сопоставлены строкам из `strings.xml`.
 * Неизвестное имя возвращается как есть (для пользовательских категорий в будущем).
 */
@Composable
fun categoryTitle(englishName: String): String {
    val id =
        when (englishName) {
            "Furniture" -> R.string.category_furniture
            "Home" -> R.string.category_home
            "Nature" -> R.string.category_nature
            "Animals" -> R.string.category_animals
            "Food" -> R.string.category_food
            "Transport" -> R.string.category_transport
            "Household" -> R.string.category_household
            else -> null
        }
    return id?.let { stringResource(it) } ?: englishName
}
