package com.fildy.di

import com.fildy.data.repository.FarmRepository
import com.fildy.data.repository.FarmRepositoryImpl
import com.fildy.data.repository.RainfallRepositoryImpl
import com.fildy.data.repository.RainfallRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFarmRepository(impl: FarmRepositoryImpl): FarmRepository

    @Binds
    @Singleton
    abstract fun bindRainfallRepository(impl: RainfallRepositoryImpl): RainfallRepository
}
