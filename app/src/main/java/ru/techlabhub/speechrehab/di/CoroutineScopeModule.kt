package ru.techlabhub.speechrehab.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Долгоживущий [CoroutineScope] на уровне приложения (supervisor + IO).
 * Используется для фоновой работы после уничтожения ViewModel (например, закрытие сессии тренировки),
 * без блокировки главного потока через [kotlinx.coroutines.runBlocking].
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        val dispatcher: CoroutineDispatcher = Dispatchers.IO
        return CoroutineScope(SupervisorJob() + dispatcher)
    }
}
