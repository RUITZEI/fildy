package com.fildy.data.seed

import android.content.Context
import android.util.Log
import com.fildy.data.local.TransactionRunner
import com.fildy.data.local.dao.FarmDao
import com.fildy.data.local.dao.HerdCountDao
import com.fildy.data.local.dao.PaddockDao
import com.fildy.data.local.dao.RainfallDao
import com.fildy.data.local.entity.FarmEntity
import com.fildy.data.local.entity.HerdCountEntity
import com.fildy.data.local.entity.PaddockEntity
import com.fildy.data.local.entity.RainfallEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Loads the initial farm data from `assets/seed/farm_seed.json` the first time the app runs.
 *
 * Seeding only happens when the database has no farm yet, and runs in a single transaction: if
 * the app dies halfway, nothing is committed and it simply seeds again on the next launch. Once
 * seeded, the data persists and later launches leave it untouched.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val farmDao: FarmDao,
    private val paddockDao: PaddockDao,
    private val herdCountDao: HerdCountDao,
    private val rainfallDao: RainfallDao,
    private val transaction: TransactionRunner,
    private val json: Json,
    private val clock: Clock,
) {

    suspend fun seedIfEmpty() {
        val seeded = transaction {
            if (farmDao.count() > 0) return@transaction false
            insert(parse(readSeedFile()))
            true
        }
        if (seeded) Log.i(TAG, "Database seeded from $SEED_ASSET") else Log.d(TAG, "Database already seeded")
    }

    private fun readSeedFile(): String = context.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }

    fun parse(text: String): SeedData = json.decodeFromString(text)

    private suspend fun insert(seed: SeedData) {
        val farmId = seed.farm.id
        val now = clock.instant()
        with(seed.farm) { farmDao.insert(FarmEntity(id, name, locality, province)) }
        paddockDao.insertAll(seed.paddocks.map { PaddockEntity(it.id, farmId, it.name) })
        herdCountDao.insertAll(seed.herd.map { HerdCountEntity(it.paddockId, it.category, it.headCount) })
        rainfallDao.insertAll(
            seed.rainfall.map {
                RainfallEntity(farmId = farmId, date = LocalDate.parse(it.date), millimeters = it.millimeters, recordedAt = now)
            },
        )
        Log.d(
            TAG,
            "Inserted farm '${seed.farm.name}', ${seed.paddocks.size} paddocks, " +
                "${seed.herd.sumOf { it.headCount }} head in ${seed.herd.size} counts, ${seed.rainfall.size} rain records",
        )
    }

    companion object {
        const val SEED_ASSET = "seed/farm_seed.json"
        private const val TAG = "Fildy.Seeder"
    }
}
