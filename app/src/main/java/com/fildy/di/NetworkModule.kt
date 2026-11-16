package com.fildy.di

import com.fildy.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Transcribing or synthesizing long audio can take a while.
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // Method, URL, status and timing only; the API key header is redacted anyway.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("xi-api-key")
                    },
                )
            }
        }
        .build()
}
