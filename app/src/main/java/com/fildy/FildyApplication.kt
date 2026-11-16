package com.fildy

import android.app.Application
import android.util.Log
import com.fildy.data.seed.DatabaseSeeder
import com.fildy.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class FildyApplication : Application() {

    @Inject lateinit var databaseSeeder: DatabaseSeeder

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Fills the database on the first launch only; no-op afterwards.
        applicationScope.launch {
            runCatching { databaseSeeder.seedIfEmpty() }
                .onFailure { Log.e("Fildy.Seeder", "Seeding failed", it) }
        }
    }
}
