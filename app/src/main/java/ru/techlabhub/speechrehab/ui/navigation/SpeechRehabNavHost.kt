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
import ru.techlabhub.speechrehab.ui.vocabulary.VocabularyScreen

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
