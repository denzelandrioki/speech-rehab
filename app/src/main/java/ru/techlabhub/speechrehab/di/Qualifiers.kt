package ru.techlabhub.speechrehab.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ArasaacRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PixabayRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PexelsRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageDownloadClient
