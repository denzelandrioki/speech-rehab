package ru.techlabhub.speechrehab.ui.training.mc

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.ImageSource
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceOption
import ru.techlabhub.speechrehab.domain.model.MultipleChoiceQuestion
import ru.techlabhub.speechrehab.domain.model.WordDisplayFormatter
import java.io.File

/** Зелёный для верного ответа (контраст с фоном M3). */
private val McFeedbackCorrectGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleChoiceTrainingScreen(
    onExit: () -> Unit,
    vm: MultipleChoiceTrainingViewModel = hiltViewModel(),
) {
    val state by vm.ui.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_mc_training)) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.mc_loading), style = MaterialTheme.typography.bodyLarge)
                }
            }

            state.errorMessage != null -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { vm.loadNextQuestion() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Text(stringResource(R.string.training_retry), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            state.question != null -> {
                val q = state.question!!
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Оставшаяся высота — картинка или слово после ответа; кнопки всегда видны (портрет и альбом).
                    Box(
                        modifier =
                            Modifier
                                .weight(1f, fill = true)
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        McQuestionOrFeedback(
                            q = q,
                            phase = state.phase,
                            selectedWordId = state.selectedWordId,
                            context = context,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    McOptionsGrid(
                        options = q.options,
                        isLandscape = isLandscape,
                        q = q,
                        phase = state.phase,
                        selectedWordId = state.selectedWordId,
                        onOptionClick = { vm.onOptionSelected(it) },
                    )
                    if (state.phase == MultipleChoicePhase.FEEDBACK) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { vm.onContinueAfterFeedback() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            Text(stringResource(R.string.mc_next_question), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

/**
 * До ответа — изображение ([ContentScale.Fit] в отведённой области).
 * После ответа — правильное слово: зелёный при верном выборе, красный при ошибке.
 */
@Composable
private fun McQuestionOrFeedback(
    q: MultipleChoiceQuestion,
    phase: MultipleChoicePhase,
    selectedWordId: Long?,
    context: Context,
    modifier: Modifier = Modifier,
) {
    when (phase) {
        MultipleChoicePhase.CHOOSING -> McQuestionImage(q = q, context = context, modifier = modifier)
        MultipleChoicePhase.FEEDBACK -> {
            val correctLabel =
                WordDisplayFormatter.displayLabel(q.correctWord, q.displayLanguageEffective)
                    .trim()
                    .ifBlank { q.correctWord.text.ifBlank { "—" } }
            val guessedRight = selectedWordId == q.correctWord.id
            Text(
                text = correctLabel,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                color = if (guessedRight) McFeedbackCorrectGreen else MaterialTheme.colorScheme.error,
                modifier = modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun McQuestionImage(
    q: MultipleChoiceQuestion,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val card = q.imageCard
    val label = WordDisplayFormatter.displayLabel(card.word, q.displayLanguageEffective)
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
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier,
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
}

@Composable
private fun McOptionsGrid(
    options: List<MultipleChoiceOption>,
    isLandscape: Boolean,
    q: MultipleChoiceQuestion,
    phase: MultipleChoicePhase,
    selectedWordId: Long?,
    onOptionClick: (MultipleChoiceOption) -> Unit,
) {
    val choosing = phase == MultipleChoicePhase.CHOOSING
    if (isLandscape && options.size >= 4) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                McOptionButton(
                    option = options[0],
                    q = q,
                    phase = phase,
                    selectedWordId = selectedWordId,
                    enabled = choosing,
                    onClick = { onOptionClick(options[0]) },
                    modifier = Modifier.weight(1f).height(64.dp),
                )
                McOptionButton(
                    option = options[1],
                    q = q,
                    phase = phase,
                    selectedWordId = selectedWordId,
                    enabled = choosing,
                    onClick = { onOptionClick(options[1]) },
                    modifier = Modifier.weight(1f).height(64.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                McOptionButton(
                    option = options[2],
                    q = q,
                    phase = phase,
                    selectedWordId = selectedWordId,
                    enabled = choosing,
                    onClick = { onOptionClick(options[2]) },
                    modifier = Modifier.weight(1f).height(64.dp),
                )
                McOptionButton(
                    option = options[3],
                    q = q,
                    phase = phase,
                    selectedWordId = selectedWordId,
                    enabled = choosing,
                    onClick = { onOptionClick(options[3]) },
                    modifier = Modifier.weight(1f).height(64.dp),
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                McOptionButton(
                    option = opt,
                    q = q,
                    phase = phase,
                    selectedWordId = selectedWordId,
                    enabled = choosing,
                    onClick = { onOptionClick(opt) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                )
            }
        }
    }
}

@Composable
private fun McOptionButton(
    option: MultipleChoiceOption,
    q: MultipleChoiceQuestion,
    phase: MultipleChoicePhase,
    selectedWordId: Long?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val correctId = q.correctWord.id
    val isCorrectOption = option.word.id == correctId
    val isSelected = option.word.id == selectedWordId

    val colors =
        when (phase) {
            MultipleChoicePhase.CHOOSING ->
                ButtonDefaults.buttonColors()
            MultipleChoicePhase.FEEDBACK -> {
                when {
                    isCorrectOption ->
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    isSelected && !isCorrectOption ->
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    else ->
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
        }

    val shape = RoundedCornerShape(12.dp)
    val borderModifier =
        when {
            phase == MultipleChoicePhase.FEEDBACK && isCorrectOption ->
                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
            phase == MultipleChoicePhase.FEEDBACK && isSelected && !isCorrectOption ->
                Modifier.border(3.dp, MaterialTheme.colorScheme.error, shape)
            else -> Modifier
        }

    val label =
        option.label.trim().ifBlank {
            WordDisplayFormatter.displayLabel(option.word, q.displayLanguageEffective)
                .ifBlank { option.word.text.ifBlank { "—" } }
        }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(borderModifier),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = colors,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
