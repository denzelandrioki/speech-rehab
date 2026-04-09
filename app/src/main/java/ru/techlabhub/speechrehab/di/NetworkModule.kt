package ru.techlabhub.speechrehab.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.techlabhub.speechrehab.BuildConfig
import ru.techlabhub.speechrehab.data.remote.RetryInterceptor
import ru.techlabhub.speechrehab.data.remote.api.ArasaacApi
import ru.techlabhub.speechrehab.data.remote.api.PexelsApi
import ru.techlabhub.speechrehab.data.remote.api.PixabayApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
            }
        return OkHttpClient.Builder()
            // На эмуляторе HTTP/2 к CDN иногда даёт reset/timeout — только HTTP/1.1 стабильнее.
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(maxRetries = 2))
            .addInterceptor(logging)
            .build()
    }

    /** Длинные ответы при скачивании PNG; не смешиваем с Retrofit call timeout. */
    @Provides
    @Singleton
    @ImageDownloadClient
    fun provideImageDownloadOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

    private fun buildRetrofit(
        client: OkHttpClient,
        json: Json,
        baseUrl: String,
    ): Retrofit {
        val mediaType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(mediaType))
            .build()
    }

    @Provides
    @Singleton
    @ArasaacRetrofit
    fun provideArasaacRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(client, json, "https://api.arasaac.org/")

    @Provides
    @Singleton
    @PixabayRetrofit
    fun providePixabayRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(client, json, "https://pixabay.com/")

    @Provides
    @Singleton
    @PexelsRetrofit
    fun providePexelsRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(client, json, "https://api.pexels.com/")

    @Provides
    @Singleton
    fun provideArasaacApi(
        @ArasaacRetrofit retrofitClient: Retrofit,
    ): ArasaacApi = retrofitClient.create(ArasaacApi::class.java)

    @Provides
    @Singleton
    fun providePixabayApi(
        @PixabayRetrofit retrofitClient: Retrofit,
    ): PixabayApi = retrofitClient.create(PixabayApi::class.java)

    @Provides
    @Singleton
    fun providePexelsApi(
        @PexelsRetrofit retrofitClient: Retrofit,
    ): PexelsApi = retrofitClient.create(PexelsApi::class.java)
}
