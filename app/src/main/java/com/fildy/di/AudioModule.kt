package com.fildy.di

import com.fildy.data.audio.AudioPlayer
import com.fildy.data.audio.AudioRecorder
import com.fildy.data.audio.MediaAudioPlayer
import com.fildy.data.audio.MediaAudioRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/** Recorder and player hold per-screen state, so each ViewModel gets its own instances. */
@Module
@InstallIn(ViewModelComponent::class)
abstract class AudioModule {

    @Binds
    abstract fun bindAudioRecorder(impl: MediaAudioRecorder): AudioRecorder

    @Binds
    abstract fun bindAudioPlayer(impl: MediaAudioPlayer): AudioPlayer
}
