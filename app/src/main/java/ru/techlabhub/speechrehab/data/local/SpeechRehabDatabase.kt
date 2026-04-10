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

/**
 * Локальная база Room: категории, слова, кэш путей картинок, сессии тренировок, попытки ответов.
 *
 * Версия 3: [WordEntity.displayTextRu], [WordEntity.displayTextEn], [WordEntity.bundledAssetName] (миграция [MIGRATION_2_3]).
 */
@Database(
    entities = [
        CategoryEntity::class,
        WordEntity::class,
        CachedImageEntity::class,
        TrainingSessionEntity::class,
        AnswerAttemptEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class SpeechRehabDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao
    abstract fun cachedImageDao(): CachedImageDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun answerAttemptDao(): AnswerAttemptDao

    companion object {
        /** Добавление русских подписей к существующим строкам словаря без потери данных. */
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

        /**
         * displayText → displayTextRu + displayTextEn (копия из text), колонка bundledAssetName.
         * Старые пользователи: русская подпись переносится из displayText, английская = text.
         */
        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `words_new` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `text` TEXT NOT NULL,
                          `displayTextRu` TEXT NOT NULL DEFAULT '',
                          `displayTextEn` TEXT NOT NULL DEFAULT '',
                          `bundledAssetName` TEXT NOT NULL DEFAULT '',
                          `categoryId` INTEGER NOT NULL,
                          `enabled` INTEGER NOT NULL DEFAULT 1,
                          `isCustom` INTEGER NOT NULL DEFAULT 0,
                          `consecutiveCorrect` INTEGER NOT NULL DEFAULT 0,
                          `consecutiveIncorrect` INTEGER NOT NULL DEFAULT 0,
                          FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO words_new (id, text, displayTextRu, displayTextEn, bundledAssetName, categoryId, enabled, isCustom, consecutiveCorrect, consecutiveIncorrect)
                        SELECT id, text, displayText, text, '', categoryId, enabled, isCustom, consecutiveCorrect, consecutiveIncorrect FROM words
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE words")
                    db.execSQL("ALTER TABLE words_new RENAME TO words")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_categoryId` ON `words` (`categoryId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_enabled` ON `words` (`enabled`)")
                }
            }
    }
}
