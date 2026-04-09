package ru.techlabhub.speechrehab.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.techlabhub.speechrehab.R
import ru.techlabhub.speechrehab.domain.model.DailyStats
import ru.techlabhub.speechrehab.domain.model.TrendDirection
import ru.techlabhub.speechrehab.ui.common.categoryTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    vm: StatisticsViewModel = hiltViewModel(),
) {
    val snap by vm.snapshot.collectAsState()
    val loading by vm.loading.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.screen_statistics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val s = snap
            when {
                s == null && !loading -> {
                    Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.titleLarge)
                }
                s != null -> {
                    Text(
                        text = stringResource(R.string.stats_overall_accuracy, s.overallAccuracyPercent),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.stats_attempts, s.totalAttempts, s.totalCorrect),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Text(stringResource(R.string.stats_last_days), style = MaterialTheme.typography.titleLarge)
                    val lastDays = s.daily.takeLast(14)
                    lastDays.forEach { d ->
                        DailyRow(d)
                    }

                    Text(stringResource(R.string.stats_trends), style = MaterialTheme.typography.titleLarge)
                    TrendLine(stringResource(R.string.stats_trend_window, 7), s.trend7)
                    TrendLine(stringResource(R.string.stats_trend_window, 14), s.trend14)
                    TrendLine(stringResource(R.string.stats_trend_window, 30), s.trend30)

                    Text(stringResource(R.string.stats_hardest), style = MaterialTheme.typography.titleLarge)
                    s.hardestWords.forEach { w ->
                        Text(
                            text = stringResource(R.string.stats_word_line, w.text, w.accuracyPercent, w.attempts),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    Text(stringResource(R.string.stats_easiest), style = MaterialTheme.typography.titleLarge)
                    s.easiestWords.forEach { w ->
                        Text(
                            text = stringResource(R.string.stats_word_line, w.text, w.accuracyPercent, w.attempts),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    Text(stringResource(R.string.stats_categories), style = MaterialTheme.typography.titleLarge)
                    s.categoryProgress.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = categoryTitle(c.name),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${"%.0f".format(c.accuracyPercent)}%",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (c.accuracyPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRow(d: DailyStats) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(Locale.forLanguageTag("ru"))
    val dateLabel =
        runCatching {
            LocalDate.ofEpochDay(d.dayEpochDay).format(dateFormatter)
        }.getOrElse {
            stringResource(R.string.stats_day_fallback, d.dayEpochDay)
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(dateLabel, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${"%.0f".format(d.accuracyPercent)}% (${d.attempts})",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        LinearProgressIndicator(
            progress = { (d.accuracyPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TrendLine(
    label: String,
    t: ru.techlabhub.speechrehab.domain.model.TrendResult,
) {
    val dir =
        when (t.direction) {
            TrendDirection.IMPROVING -> stringResource(R.string.trend_improving)
            TrendDirection.DECLINING -> stringResource(R.string.trend_declining)
            TrendDirection.STABLE -> stringResource(R.string.trend_stable)
        }
    Text(
        text =
            stringResource(
                R.string.trend_line,
                label,
                dir,
                t.firstHalfAccuracy,
                t.secondHalfAccuracy,
            ),
        style = MaterialTheme.typography.bodyLarge,
    )
}
