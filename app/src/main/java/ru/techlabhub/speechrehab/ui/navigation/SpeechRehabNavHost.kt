package ru.techlabhub.speechrehab.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.techlabhub.speechrehab.ui.home.HomeScreen
import ru.techlabhub.speechrehab.ui.settings.SettingsScreen
import ru.techlabhub.speechrehab.ui.statistics.StatisticsScreen
import ru.techlabhub.speechrehab.ui.training.TrainingScreen
import ru.techlabhub.speechrehab.ui.training.mc.MultipleChoiceTrainingScreen
import ru.techlabhub.speechrehab.ui.vocabulary.VocabularyScreen

/**
 * Корневой навигационный граф приложения (один [androidx.navigation.NavController], плоский стек экранов).
 *
 * Стартовый экран — [Routes.Home]. Переходы на второстепенные экраны через `navigate`;
 * возврат — [androidx.navigation.NavController.popBackStack].
 * Глубокие ссылки и общий ViewModel между экранами здесь не используются (MVP).
 */
@Composable
fun SpeechRehabNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.Home,
        modifier = modifier,
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onStartTraining = { navController.navigate(Routes.Training) },
                onStartMultipleChoice = { navController.navigate(Routes.MultipleChoiceTraining) },
                onOpenStatistics = { navController.navigate(Routes.Statistics) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenVocabulary = { navController.navigate(Routes.Vocabulary) },
            )
        }
        composable(Routes.Training) {
            TrainingScreen(
                onExit = { navController.popBackStack() },
            )
        }
        composable(Routes.MultipleChoiceTraining) {
            MultipleChoiceTrainingScreen(
                onExit = { navController.popBackStack() },
            )
        }
        composable(Routes.Statistics) {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Vocabulary) {
            VocabularyScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
