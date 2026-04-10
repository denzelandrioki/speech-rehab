package ru.techlabhub.speechrehab.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.ImageSource
import ru.techlabhub.speechrehab.domain.model.WordDisplayFormatter
import java.io.File

/**
 * Экран тренировки: изображение (файл, asset или URL), подпись слова при включённой подсказке,
 * кнопки «верно» / «неверно» / «следующая». Состояние — [TrainingViewModel.ui].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onExit: () -> Unit,
    vm: TrainingViewModel = hiltViewModel(),
) {
    val state by vm.ui.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_training)) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.training_loading), style = MaterialTheme.typography.bodyLarge)
                }

                state.errorMessage != null -> {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    BigAction(
                        text = stringResource(R.string.training_retry),
                        onClick = { vm.loadNextCard() },
                    )
                }

                state.card != null -> {
                    val card = state.card!!
                    val label =
                        WordDisplayFormatter.displayLabel(card.word, state.trainingTextLanguage)
                    val hasImage =
                        card.imageUri != null &&
                            card.source != ImageSource.NONE

                    if (hasImage) {
                        val uri = card.imageUri!!
                        val model: Any =
                            when {
                                uri.startsWith("http", ignoreCase = true) -> uri
                                uri.startsWith("file:///android_asset/") -> uri
                                uri.startsWith("file://") -> File(uri.removePrefix("file://"))
                                else -> File(uri)
                            }
                        AsyncImage(
                            model =
                                ImageRequest.Builder(context)
                                    .data(model)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = label.ifBlank { card.word.text },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.ImageNotSupported,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                                Text(
                                    text = stringResource(R.string.training_image_unavailable_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    if (state.showWordHint && label.isNotEmpty()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RowBigButtons(
                        onCorrect = { vm.onCorrect() },
                        onIncorrect = { vm.onIncorrect() },
                        onNext = { vm.loadNextCard() },
                        answersLocked = state.lockedAfterAnswer,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowBigButtons(
    onCorrect: () -> Unit,
    onIncorrect: () -> Unit,
    onNext: () -> Unit,
    answersLocked: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onCorrect,
            enabled = !answersLocked,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Text(stringResource(R.string.training_correct), style = MaterialTheme.typography.titleLarge)
        }
        Button(
            onClick = onIncorrect,
            enabled = !answersLocked,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Text(stringResource(R.string.training_incorrect), style = MaterialTheme.typography.titleLarge)
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.training_next), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun BigAction(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}
