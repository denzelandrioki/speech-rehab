package ru.techlabhub.speechrehab.di

import javax.inject.Qualifier

/**
 * Квалификаторы Dagger/Hilt: несколько экземпляров одного типа ([Retrofit], [okhttp3.OkHttpClient])
 * различаются аннотациями при внедрении.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ArasaacRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PixabayRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PexelsRetrofit

/** Отдельный [okhttp3.OkHttpClient] без Retrofit — только скачивание байтов изображений. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageDownloadClient

/** [kotlinx.coroutines.CoroutineScope] приложения (не отменяется при `onCleared` у ViewModel). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
