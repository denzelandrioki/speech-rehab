package ru.techlabhub.speechrehab.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.TrainingMode

@Composable
fun trainingModeLabel(mode: TrainingMode): String =
    when (mode) {
        TrainingMode.RANDOM -> stringResource(R.string.mode_random)
        TrainingMode.BY_CATEGORY -> stringResource(R.string.mode_by_category)
        TrainingMode.HARD_WORDS -> stringResource(R.string.mode_hard_words)
        TrainingMode.NEW_ONLY -> stringResource(R.string.mode_new_only)
        TrainingMode.MIXED -> stringResource(R.string.mode_mixed)
    }
