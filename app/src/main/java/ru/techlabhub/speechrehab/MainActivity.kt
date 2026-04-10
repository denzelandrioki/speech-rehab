package ru.techlabhub.speechrehab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import ru.techlabhub.speechrehab.domain.model.AppLanguage
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.ui.navigation.SpeechRehabNavHost
import ru.techlabhub.speechrehab.ui.theme.SpeechRehabTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Единственная [android.app.Activity] приложения.
 *
 * Точка входа в UI: включает отображение контента «от края до края» ([enableEdgeToEdge]),
 * оборачивает экран в [SpeechRehabTheme] (тёмная/светлая тема Material 3) и показывает
 * [ru.techlabhub.speechrehab.ui.navigation.SpeechRehabNavHost] — граф навигации между главным меню,
 * тренировкой, статистикой, настройками и словарём.
 *
 * Язык интерфейса: [AppCompatDelegate.setApplicationLocales] по настройке из DataStore.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            userPreferencesRepository.preferencesFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { lang ->
                    val locales =
                        when (lang) {
                            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                            AppLanguage.RUSSIAN -> LocaleListCompat.forLanguageTags("ru")
                            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
                        }
                    AppCompatDelegate.setApplicationLocales(locales)
                }
        }
        enableEdgeToEdge()
        setContent {
            SpeechRehabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpeechRehabNavHost()
                }
            }
        }
    }
}
