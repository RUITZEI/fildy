package com.fildy.data.repository

import android.util.Log
import com.fildy.data.local.TransactionRunner
import com.fildy.data.local.dao.FarmDao
import com.fildy.data.local.dao.RainfallDao
import com.fildy.data.local.entity.RainfallEntity
import com.fildy.data.local.entity.toDomain
import com.fildy.data.local.entity.toEntity
import com.fildy.data.model.RainfallRecord
import com.fildy.data.model.RainfallUpdate
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [RainfallRepository] backed by the local Room database. */
class RainfallRepositoryImpl @Inject constructor(
    private val rainfallDao: RainfallDao,
    private val farmDao: FarmDao,
    private val transaction: TransactionRunner,
    private val clock: Clock,
) : RainfallRepository {

    override fun observeRecent(days: Long): Flow<List<RainfallRecord>> {
        val from = LocalDate.now(clock).minusDays(days - 1)
        return rainfallDao.observeSince(from).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getRainfall(date: LocalDate): RainfallRecord? {
        val farm = farmDao.getFarm() ?: return null
        return rainfallDao.getByDate(farm.id, date)?.toDomain()
    }

    override suspend fun getRainfallBetween(from: LocalDate, to: LocalDate): List<RainfallRecord> {
        val farm = farmDao.getFarm() ?: return emptyList()
        return rainfallDao.getBetween(farm.id, from, to).map { it.toDomain() }
    }

    override suspend fun setRainfall(date: LocalDate, millimeters: Double): RainfallUpdate {
        require(millimeters >= 0) { "Rainfall can't be negative: $millimeters" }
        val update = transaction {
            val farm = checkNotNull(farmDao.getFarm()) { "No farm in the database" }
            val previous = rainfallDao.getByDate(farm.id, date)
            val entity = RainfallEntity(
                id = previous?.id ?: 0,
                farmId = farm.id,
                date = date,
                millimeters = millimeters,
                recordedAt = clock.instant(),
            )
            val saved = if (previous == null) {
                entity.copy(id = rainfallDao.insert(entity))
            } else {
                rainfallDao.update(entity)
                entity
            }
            RainfallUpdate(record = saved.toDomain(), previous = previous?.toDomain())
        }
        Log.i(TAG, "Set $millimeters mm on $date" + (update.previous?.let { " (replaced ${it.millimeters} mm)" } ?: ""))
        return update
    }

    override suspend fun undo(update: RainfallUpdate) {
        transaction {
            val current = rainfallDao.getByDate(update.record.farmId, update.record.date)
            require(current?.toDomain() == update.record) {
                "Rain on ${update.record.date} changed after this update; can't undo it"
            }
            val previous = update.previous
            if (previous == null) rainfallDao.delete(update.record.id) else rainfallDao.update(previous.toEntity())
        }
        Log.i(TAG, "Undid rain on ${update.record.date}: back to ${update.previous?.millimeters ?: "no record"}")
    }

    override suspend fun deleteRainfall(id: Long) {
        rainfallDao.delete(id)
        Log.i(TAG, "Deleted rain record $id")
    }

    private companion object {
        const val TAG = "Fildy.RainfallRepository"
    }
}
