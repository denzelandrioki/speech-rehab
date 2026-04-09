package ru.techlabhub.speechrehab

import android.app.Application
import ru.techlabhub.speechrehab.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Класс [Application] для всего процесса Android.
 *
 * [HiltAndroidApp] помечает приложение как корень графа зависимостей Hilt (singleton-компоненты,
 * модули `DatabaseModule`, `NetworkModule` и т.д.).
 *
 * В отладочной сборке ([BuildConfig.DEBUG]) подключается [Timber.DebugTree] —
 * удобные логи в Logcat с тегом и стеком; в release можно заменить на отправку в Crashlytics и т.п.
 */
@HiltAndroidApp
class SpeechRehabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
