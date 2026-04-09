package ru.techlabhub.speechrehab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.techlabhub.speechrehab.ui.navigation.SpeechRehabNavHost
import ru.techlabhub.speechrehab.ui.theme.SpeechRehabTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная [android.app.Activity] приложения.
 *
 * Точка входа в UI: включает отображение контента «от края до края» ([enableEdgeToEdge]),
 * оборачивает экран в [SpeechRehabTheme] (тёмная/светлая тема Material 3) и показывает
 * [ru.techlabhub.speechrehab.ui.navigation.SpeechRehabNavHost] — граф навигации между главным меню,
 * тренировкой, статистикой, настройками и словарём.
 *
 * Аннотация [AndroidEntryPoint] подключает Hilt: во вложенных composable можно
 * использовать `hiltViewModel()` и внедрять зависимости в ViewModel.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
