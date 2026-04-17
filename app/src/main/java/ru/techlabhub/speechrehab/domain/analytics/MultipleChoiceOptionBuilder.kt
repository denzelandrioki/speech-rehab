package ru.techlabhub.speechrehab.domain.analytics

import ru.techlabhub.speechrehab.domain.model.WordItem
import kotlin.random.Random

/**
 * Собирает 4 уникальных слова для вопроса: правильное + до 3 дистракторов (сначала та же категория, затем пул).
 */
object MultipleChoiceOptionBuilder {
    private const val OPTION_COUNT = 4

    /**
     * @return ровно 4 слова в случайном порядке, или `null` если в [pool] меньше 4 разных слов с [correct].
     */
    fun buildOptions(
        correct: WordItem,
        pool: List<WordItem>,
        random: Random = Random.Default,
    ): List<WordItem>? {
        val distinct = pool.distinctBy { it.id }
        if (distinct.size < OPTION_COUNT) return null
        if (distinct.none { it.id == correct.id }) return null

        val others = distinct.filter { it.id != correct.id }
        val sameCategory = others.filter { it.categoryId == correct.categoryId }.shuffled(random)
        val picks = mutableListOf<WordItem>()
        picks.addAll(sameCategory.take(3))
        if (picks.size < 3) {
            val need = 3 - picks.size
            val rest = others.filter { w -> picks.none { it.id == w.id } }.shuffled(random)
            picks.addAll(rest.take(need))
        }
        val withCorrect = (picks.take(3) + correct).distinctBy { it.id }.toMutableList()
        if (withCorrect.size < OPTION_COUNT) {
            val missing = OPTION_COUNT - withCorrect.size
            val extra =
                others
                    .filter { o -> withCorrect.none { it.id == o.id } }
                    .shuffled(random)
                    .take(missing)
            withCorrect.addAll(extra)
        }
        if (withCorrect.size < OPTION_COUNT) return null
        return withCorrect.take(OPTION_COUNT).shuffled(random)
    }
}
