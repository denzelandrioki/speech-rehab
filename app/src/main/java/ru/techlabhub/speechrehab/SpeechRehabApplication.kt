package ru.techlabhub.speechrehab

import android.app.Application
import android.util.Log
import ru.techlabhub.speechrehab.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Класс [Application] для всего процесса Android.
 *
 * [HiltAndroidApp] помечает приложение как корень графа зависимостей Hilt (singleton-компоненты,
 * модули `DatabaseModule`, `NetworkModule` и т.д.).
 *
 * Debug: [Timber.DebugTree]. Release: только WARN+ — меньше шума, сбои и предупреждения остаются в Logcat.
 */
@HiltAndroidApp
class SpeechRehabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseLogTree())
        }
    }

    /** Логи в release без полного verbose-шума от Timber.d. */
    private class ReleaseLogTree : Timber.Tree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?,
        ) {
            if (priority < Log.WARN) return
            val tName = tag ?: "SpeechRehab"
            val body =
                if (t != null) {
                    "$message\n${Log.getStackTraceString(t)}"
                } else {
                    message
                }
            Log.println(priority, tName, body)
        }
    }
}
