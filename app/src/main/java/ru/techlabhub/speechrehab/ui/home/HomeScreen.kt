package ru.techlabhub.speechrehab.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.techlabhub.speechrehab.R

/**
 * Главный экран: крупные кнопки для запуска тренировки и перехода к статистике, настройкам и словарю.
 * Отдельного ViewModel нет — только колбэки, которые передаёт навигационный хост.
 */
@Composable
fun HomeScreen(
    onStartTraining: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVocabulary: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        HomeBigButton(text = stringResource(R.string.home_start_training), onClick = onStartTraining)
        HomeBigButton(text = stringResource(R.string.home_statistics), onClick = onOpenStatistics)
        HomeBigButton(text = stringResource(R.string.home_settings), onClick = onOpenSettings)
        HomeBigButton(text = stringResource(R.string.home_vocabulary), onClick = onOpenVocabulary)
    }
}

@Composable
private fun HomeBigButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}
