package ru.techlabhub.speechrehab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.techlabhub.speechrehab.data.local.seed.VocabularyCatalog
import ru.techlabhub.speechrehab.data.local.dao.AnswerAttemptDao
import ru.techlabhub.speechrehab.data.local.dao.CachedImageDao
import ru.techlabhub.speechrehab.data.local.dao.CategoryDao
import ru.techlabhub.speechrehab.data.local.dao.TrainingSessionDao
import ru.techlabhub.speechrehab.data.local.dao.WordDao
import ru.techlabhub.speechrehab.data.local.entity.AnswerAttemptEntity
import ru.techlabhub.speechrehab.data.local.entity.CachedImageEntity
import ru.techlabhub.speechrehab.data.local.entity.CategoryEntity
import ru.techlabhub.speechrehab.data.local.entity.TrainingSessionEntity
import ru.techlabhub.speechrehab.data.local.entity.WordEntity

@Database(
    entities = [
        CategoryEntity::class,
        WordEntity::class,
        CachedImageEntity::class,
        TrainingSessionEntity::class,
        AnswerAttemptEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SpeechRehabDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao
    abstract fun cachedImageDao(): CachedImageDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun answerAttemptDao(): AnswerAttemptDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE words ADD COLUMN displayText TEXT NOT NULL DEFAULT ''")
                    VocabularyCatalog.englishToRussianMap().forEach { (en, ru) ->
                        db.execSQL(
                            "UPDATE words SET displayText = ? WHERE text = ?",
                            arrayOf(ru, en),
                        )
                    }
                }
            }
    }
}
