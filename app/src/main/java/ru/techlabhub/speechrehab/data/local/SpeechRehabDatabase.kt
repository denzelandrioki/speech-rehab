package ru.techlabhub.speechrehab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.techlabhub.speechrehab.data.local.seed.VocabularyCatalog
import ru.techlabhub.speechrehab.data.local.dao.AnswerAttemptDao
import ru.techlabhub.speechrehab.data.local.dao.CategoryDao
import ru.techlabhub.speechrehab.data.local.dao.TrainingSessionDao
import ru.techlabhub.speechrehab.data.local.dao.WordDao
import ru.techlabhub.speechrehab.data.local.dao.WordImageVariantDao
import ru.techlabhub.speechrehab.data.local.entity.AnswerAttemptEntity
import ru.techlabhub.speechrehab.data.local.entity.CategoryEntity
import ru.techlabhub.speechrehab.data.local.entity.TrainingSessionEntity
import ru.techlabhub.speechrehab.data.local.entity.WordEntity
import ru.techlabhub.speechrehab.data.local.entity.WordImageVariantEntity

/**
 * Локальная база Room: категории, слова, варианты картинок на слово, сессии тренировок, попытки ответов.
 *
 * Версия 4: `cached_images` → [WordImageVariantEntity] ([MIGRATION_3_4]).
 */
@Database(
    entities = [
        CategoryEntity::class,
        WordEntity::class,
        WordImageVariantEntity::class,
        TrainingSessionEntity::class,
        AnswerAttemptEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class SpeechRehabDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao
    abstract fun wordImageVariantDao(): WordImageVariantDao
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

        /**
         * Несколько изображений на слово: новая таблица, данные из `cached_images` переносятся построчно
         * (fetchSignature = remoteUrl для уже сохранённых строк).
         */
        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `word_image_variants` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `wordId` INTEGER NOT NULL,
                          `remoteUrl` TEXT NOT NULL,
                          `localFilePath` TEXT NOT NULL,
                          `sourceName` TEXT NOT NULL,
                          `fetchSignature` TEXT NOT NULL,
                          `wasShown` INTEGER NOT NULL DEFAULT 0,
                          `lastShownAtEpochMillis` INTEGER NOT NULL DEFAULT 0,
                          `createdAtEpochMillis` INTEGER NOT NULL,
                          FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_word_image_variants_wordId` ON `word_image_variants` (`wordId`)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_word_image_variants_wordId_fetchSignature` ON `word_image_variants` (`wordId`, `fetchSignature`)",
                    )
                    db.execSQL(
                        """
                        INSERT INTO word_image_variants (wordId, remoteUrl, localFilePath, sourceName, fetchSignature, wasShown, lastShownAtEpochMillis, createdAtEpochMillis)
                        SELECT wordId, remoteUrl, localFilePath, sourceName, remoteUrl, 0, 0, updatedAtEpochMillis FROM cached_images
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE cached_images")
                }
            }
    }
}
