package ru.techlabhub.speechrehab.di

import android.content.Context
import androidx.room.Room
import ru.techlabhub.speechrehab.data.local.SpeechRehabDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Предоставляет singleton [SpeechRehabDatabase] (Room) с именем файла `speech_rehab.db` и цепочкой миграций.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SpeechRehabDatabase =
        Room.databaseBuilder(
            context,
            SpeechRehabDatabase::class.java,
            "speech_rehab.db",
        ).addMigrations(
            SpeechRehabDatabase.MIGRATION_1_2,
            SpeechRehabDatabase.MIGRATION_2_3,
            SpeechRehabDatabase.MIGRATION_3_4,
            SpeechRehabDatabase.MIGRATION_4_5,
        )
            .build()
}
