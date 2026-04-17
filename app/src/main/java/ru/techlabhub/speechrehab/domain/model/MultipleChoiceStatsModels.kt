package ru.techlabhub.speechrehab.domain.model

/** Сводка по самостоятельному режиму (4 варианта), отдельно от assisted. */
data class MultipleChoiceStatsSnapshot(
    val totalAttempts: Int,
    val totalCorrect: Int,
    val accuracyPercent: Float,
    val avgResponseTimeMillis: Float?,
    val daily: List<DailyStats>,
    val hardestWords: List<WordRank>,
    val categoryProgress: List<CategoryAggregate>,
    val trend7: TrendResult,
    val confusionPairs: List<ConfusionPairStat>,
    val topWrongSelections: List<WrongSelectionStat>,
)

data class ConfusionPairStat(
    val correctLabel: String,
    val wrongLabel: String,
    val count: Int,
)

data class WrongSelectionStat(
    val label: String,
    val count: Int,
)
