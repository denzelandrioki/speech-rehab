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
