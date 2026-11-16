package com.fildy.di

import android.util.Log
import com.fildy.BuildConfig
import com.fildy.data.assistant.Assistant
import com.fildy.data.assistant.AssistantTool
import com.fildy.data.assistant.openai.OpenAiAssistant
import com.fildy.data.assistant.openai.OpenAiConfig
import com.fildy.data.assistant.tools.AdjustHerdTool
import com.fildy.data.assistant.tools.MoveCattleTool
import com.fildy.data.assistant.tools.PaddockHerdTool
import com.fildy.data.assistant.tools.RainfallBetweenDatesTool
import com.fildy.data.assistant.tools.RainfallOnDateTool
import com.fildy.data.assistant.tools.SetRainfallTool
import com.fildy.domain.voice.VoiceAssistantSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/** Picks the assistant provider and registers the tools it can use. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {

    /** To switch providers, bind a different [Assistant] implementation here. */
    @Binds
    @Singleton
    abstract fun bindAssistant(impl: OpenAiAssistant): Assistant

    // Tools: add a new one with another @Binds @IntoSet.

    @Binds
    @IntoSet
    abstract fun bindRainfallOnDateTool(tool: RainfallOnDateTool): AssistantTool

    @Binds
    @IntoSet
    abstract fun bindRainfallBetweenDatesTool(tool: RainfallBetweenDatesTool): AssistantTool

    @Binds
    @IntoSet
    abstract fun bindSetRainfallTool(tool: SetRainfallTool): AssistantTool

    @Binds
    @IntoSet
    abstract fun bindPaddockHerdTool(tool: PaddockHerdTool): AssistantTool

    @Binds
    @IntoSet
    abstract fun bindMoveCattleTool(tool: MoveCattleTool): AssistantTool

    @Binds
    @IntoSet
    abstract fun bindAdjustHerdTool(tool: AdjustHerdTool): AssistantTool

    companion object {
        @Provides
        @Singleton
        fun provideOpenAiConfig(): OpenAiConfig =
            OpenAiConfig(apiKey = BuildConfig.OPENAI_API_KEY).also {
                if (it.apiKey.isBlank()) Log.w(TAG, "OPENAI_API_KEY is not set; the assistant will fail")
                Log.d(TAG, "Using $it")
            }

        @Provides
        fun provideVoiceAssistantSettings(): VoiceAssistantSettings =
            VoiceAssistantSettings(speakAnswers = BuildConfig.SPEAK_ANSWERS)

        private const val TAG = "Fildy.Assistant"
    }
}
