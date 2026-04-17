package ru.techlabhub.speechrehab



import android.os.Bundle

import androidx.activity.compose.setContent

import androidx.activity.enableEdgeToEdge

import androidx.appcompat.app.AppCompatActivity

import androidx.appcompat.app.AppCompatDelegate

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.Surface

import androidx.compose.ui.Modifier

import androidx.core.os.LocaleListCompat

import androidx.lifecycle.lifecycleScope

import ru.techlabhub.speechrehab.domain.model.AppLanguage

import ru.techlabhub.speechrehab.domain.repository.UserPreferencesRepository

import ru.techlabhub.speechrehab.ui.navigation.SpeechRehabNavHost

import ru.techlabhub.speechrehab.ui.theme.SpeechRehabTheme

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.distinctUntilChanged

import kotlinx.coroutines.flow.first

import kotlinx.coroutines.flow.map

import kotlinx.coroutines.launch

import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.withContext

import javax.inject.Inject



/**

 * Единственная activity приложения.

 *

 * ### Язык интерфейса (per-app locale)

 * - Тема: `Theme.SpeechRehab` (родитель AppCompat в `themes.xml`); базовый класс — [AppCompatActivity], чтобы

 *   [AppCompatDelegate.setApplicationLocales] и выбор `values` / `values-ru` для [androidx.compose.ui.res.stringResource]

 *   вели себя предсказуемо на разных OEM.

 * - Источник правды — DataStore через [UserPreferencesRepository.preferencesFlow].

 *

 * ### Что было исправлено (после прошлого обсуждения)

 * 1. **Асинхронный collect до первого кадра** — коллектор в [lifecycleScope] стартовал параллельно с [setContent],

 *    поэтому Compose успевал зафиксировать системную [android.content.res.Configuration], и UI оставался «как в системе»,

 *    даже при сохранённом «Русский» / «English».

 * 2. **Одноразовый флаг после bootstrap-[recreate]** — на части прошивок после одного [recreate] проверка

 *    «конфиг совпадает с выбором» всё ещё false; без повторного [recreate] локаль не догоняла настройку.

 * 3. **Зацикливание [recreate]** (логи Transsion) — без ограничителя при постоянном false от OEM получался бесконечный relaunch.

 *

 * ### Текущая стратегия

 * - Синхронно (до [setContent]) читаем текущий [AppLanguage] и вызываем [applyAppLanguageLocales].

 * - Если явный язык (не SYSTEM) и [configurationMatchesAppLanguage] всё ещё false — [recreate] с увеличением

 *   счётчика в [savedInstanceState] ([STATE_LOCALE_RECREATE_CHAIN]), не более [MAX_LOCALE_RECREATE_CHAIN] подряд.

 * - Дальше подписка на смену языка в настройках: при реальном изменении предпочтения и рассинхроне — снова [recreate]

 *   в пределах того же лимита; при успешном совпадении — сброс счётчика в бандле (через [onSaveInstanceState]).

 *

 * Чтение DataStore в [runBlocking] с [Dispatchers.IO] не блокирует UI-поток на время диска; [applyAppLanguageLocales]

 * выполняется на main thread в [onCreate] (activity уже на главном потоке).

 */

@AndroidEntryPoint

class MainActivity : AppCompatActivity() {



    companion object {

        /** Ключ в [savedInstanceState]: сколько раз подряд мы уже делали locale-driven [recreate]. */

        private const val STATE_LOCALE_RECREATE_CHAIN = "locale_recreate_chain_depth"



        /** Верхняя граница цепочки [recreate], чтобы не зациклиться при «лживой» конфигурации на OEM. */

        private const val MAX_LOCALE_RECREATE_CHAIN = 5

    }



    @Inject

    lateinit var userPreferencesRepository: UserPreferencesRepository



    /**

     * Если не null — при ближайшем [onSaveInstanceState] записать в бандл новую глубину цепочки

     * (или 0 для сброса ключа). Нужно, чтобы следующий инстанс activity знал, сколько [recreate] уже было.

     */

    private var localeRecreateChainDepthToSave: Int? = null



    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        val depth = localeRecreateChainDepthToSave

        localeRecreateChainDepthToSave = null

        if (depth != null) {

            if (depth <= 0) {

                outState.remove(STATE_LOCALE_RECREATE_CHAIN)

            } else {

                outState.putInt(STATE_LOCALE_RECREATE_CHAIN, depth)

            }

        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        // Глубина цепочки locale-recreate восстанавливается только при конфигурационных пересозданиях этого же task.

        val localeRecreateChainDepth = savedInstanceState?.getInt(STATE_LOCALE_RECREATE_CHAIN) ?: 0



        // Одно синхронное чтение до setContent: иначе первый composition увидит системную локаль.

        val initialLang =

            runBlocking(Dispatchers.IO) {

                userPreferencesRepository.preferencesFlow.first().appLanguage

            }

        applyAppLanguageLocales(initialLang)



        // Пока resources.configuration не совпадает с явным выбором — даём системе шанс догнать через recreate.

        val initialMismatch =

            initialLang != AppLanguage.SYSTEM && !configurationMatchesAppLanguage(initialLang)

        if (initialMismatch && localeRecreateChainDepth < MAX_LOCALE_RECREATE_CHAIN) {

            localeRecreateChainDepthToSave = localeRecreateChainDepth + 1

            recreate()

            return

        }



        // Дальнейшие смены языка из настроек (и редкие гонки) — без блокировки onCreate.

        lifecycleScope.launch {

            // Стартуем с языка, уже применённого выше: первый collect с тем же значением не считается «сменой».

            var previousAppLanguage: AppLanguage? = initialLang

            userPreferencesRepository.preferencesFlow

                .map { it.appLanguage }

                .distinctUntilChanged()

                .collect { lang ->

                    // DataStore может сигналить не на Main; delegate трогаем на главном потоке.

                    withContext(Dispatchers.Main.immediate) {

                        applyAppLanguageLocales(lang)

                    }

                    val localeChangedSinceLast =

                        previousAppLanguage != null && previousAppLanguage != lang

                    val mismatch =

                        lang != AppLanguage.SYSTEM && !configurationMatchesAppLanguage(lang)

                    // Recreate только когда пользователь (или поток настроек) реально сменил язык — не в цикле на каждом кадре.

                    val mayRecreateForMismatch =

                        mismatch &&

                            localeRecreateChainDepth < MAX_LOCALE_RECREATE_CHAIN &&

                            localeChangedSinceLast

                    if (mayRecreateForMismatch) {

                        localeRecreateChainDepthToSave = localeRecreateChainDepth + 1

                        recreate()

                    } else if (!mismatch && localeRecreateChainDepth > 0) {

                        localeRecreateChainDepthToSave = 0

                    }

                    previousAppLanguage = lang

                }

        }

        enableEdgeToEdge()

        setContent {

            SpeechRehabTheme {

                Surface(modifier = Modifier.fillMaxSize()) {

                    SpeechRehabNavHost()

                }

            }

        }

    }



    /**

     * Пробрасывает выбранный [AppLanguage] в AppCompat / систему per-app locale.

     *

     * - [AppLanguage.SYSTEM] — пустой список: следуем языку системы.

     * - Явные теги `ru-RU` / `en-US` — стабильнее подбор `values-ru` и дефолтного `values` на части устройств.

     */

    private fun applyAppLanguageLocales(lang: AppLanguage) {

        val locales =

            when (lang) {

                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()

                AppLanguage.RUSSIAN -> LocaleListCompat.forLanguageTags("ru-RU")

                AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en-US")

            }

        AppCompatDelegate.setApplicationLocales(locales)

    }



    /**

     * Сравнивает **только языковой код** первой локали activity с выбранным режимом.

     * Нужен как эвристика «догнал ли процесс/прошивка delegate», а не как полная локальная идентичность.

     */

    private fun configurationMatchesAppLanguage(lang: AppLanguage): Boolean {

        val current = resources.configuration.locales.get(0).language.lowercase()

        return when (lang) {

            AppLanguage.SYSTEM -> true

            AppLanguage.RUSSIAN -> current == "ru"

            AppLanguage.ENGLISH -> current == "en"

        }

    }

}


