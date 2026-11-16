package com.fildy.data.local

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs several DAO calls atomically: all of them are saved, or none. */
interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

/** [TransactionRunner] using a Room database transaction. */
class RoomTransactionRunner @Inject constructor(
    private val database: FildyDatabase,
) : TransactionRunner {
    override suspend fun <T> invoke(block: suspend () -> T): T = database.withTransaction(block)
}
