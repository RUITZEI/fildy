package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.tools.MoveCattleTool
import com.fildy.data.local.FildyDatabase
import com.fildy.data.model.CattleCategory.COW
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The move tool against the real Room database: counts change and the move goes to the history. */
@RunWith(AndroidJUnit4::class)
class MoveCattleToolOnDatabaseTest {

    private lateinit var database: FildyDatabase

    @Before
    fun setUp() = runBlocking {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun movesCowsAndRecordsTheChange_thenUndoRestores() = runBlocking {
        val repository = farmRepository(database)
        val tool = MoveCattleTool(repository, Json { ignoreUnknownKeys = true })

        val output = tool.execute("""{"category":"COW","from_paddock":"Lote 1","to_paddock":"Lote 2","head_count":30}""")

        val data = output.data as AssistantData.CattleMoved
        assertEquals(120, database.herdCountDao().getHeadCount(1, COW))
        assertEquals(170, database.herdCountDao().getHeadCount(2, COW))
        assertEquals(listOf(data.move), repository.observeStockChanges().first())

        repository.undo(data.move)
        assertEquals(150, database.herdCountDao().getHeadCount(1, COW))
        assertEquals(140, database.herdCountDao().getHeadCount(2, COW))
    }

    @Test
    fun notEnoughCows_changesNothing() = runBlocking {
        val repository = farmRepository(database)
        val tool = MoveCattleTool(repository, Json { ignoreUnknownKeys = true })

        val output = tool.execute("""{"category":"COW","from_paddock":"Lote 1","to_paddock":"Lote 2","head_count":500}""")

        assertNull(output.data)
        assertEquals(150, database.herdCountDao().getHeadCount(1, COW))
        assertEquals(emptyList<Any>(), repository.observeStockChanges().first())
    }
}
