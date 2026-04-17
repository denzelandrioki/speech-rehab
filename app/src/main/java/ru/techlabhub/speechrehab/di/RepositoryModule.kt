package ru.techlabhub.speechrehab.di

import ru.techlabhub.speechrehab.data.preferences.UserPreferencesRepositoryImpl
import ru.techlabhub.speechrehab.data.repository.DefaultImageRepository
import ru.techlabhub.speechrehab.data.repository.DefaultMultipleChoiceAttemptRepository
import ru.techlabhub.speechrehab.data.repository.DefaultStatisticsRepository
import ru.techlabhub.speechrehab.data.repository.DefaultTrainingRepository
import ru.techlabhub.speechrehab.data.repository.DefaultWordRepository
import ru.techlabhub.speechrehab.domain.repository.ImageRepository
import ru.techlabhub.speechrehab.domain.repository.MultipleChoiceAttemptRepository
import ru.techlabhub.speechrehab.domain.repository.StatisticsRepository
import ru.techlabhub.speechrehab.domain.repository.TrainingRepository
import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository
import ru.techlabhub.speechrehab.domain.repository.WordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Связывает интерфейсы домена ([WordRepository], [TrainingRepository], …) с реализациями в `data.repository`
 * и [UserPreferencesRepositoryImpl] (DataStore).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWordRepository(impl: DefaultWordRepository): WordRepository

    @Binds
    @Singleton
    abstract fun bindTrainingRepository(impl: DefaultTrainingRepository): TrainingRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(impl: DefaultStatisticsRepository): StatisticsRepository

    @Binds
    @Singleton
    abstract fun bindImageRepository(impl: DefaultImageRepository): ImageRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindMultipleChoiceAttemptRepository(
        impl: DefaultMultipleChoiceAttemptRepository,
    ): MultipleChoiceAttemptRepository
}
