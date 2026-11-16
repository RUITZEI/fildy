package com.fildy.di

import android.util.Log
import com.fildy.BuildConfig
import com.fildy.data.speech.SpeechToText
import com.fildy.data.speech.TextToSpeech
import com.fildy.data.speech.elevenlabs.ElevenLabsConfig
import com.fildy.data.speech.elevenlabs.ElevenLabsSpeechToText
import com.fildy.data.speech.elevenlabs.ElevenLabsTextToSpeech
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Picks the speech provider. To switch providers, bind different implementations here. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechModule {

    @Binds
    @Singleton
    abstract fun bindSpeechToText(impl: ElevenLabsSpeechToText): SpeechToText

    @Binds
    @Singleton
    abstract fun bindTextToSpeech(impl: ElevenLabsTextToSpeech): TextToSpeech

    companion object {
        @Provides
        @Singleton
        fun provideElevenLabsConfig(): ElevenLabsConfig =
            ElevenLabsConfig(apiKey = BuildConfig.ELEVENLABS_API_KEY).also {
                if (it.apiKey.isBlank()) Log.w("Fildy.Speech", "ELEVENLABS_API_KEY is not set; speech calls will fail")
                Log.d("Fildy.Speech", "Using $it")
            }
    }
}
