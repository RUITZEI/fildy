package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.assistant.AssistantData
import com.fildy.data.assistant.tools.SetRainfallTool
import com.fildy.data.local.FildyDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** set_rainfall against the real Room database: replaces the day, and the update can be undone. */
@RunWith(AndroidJUnit4::class)
class SetRainfallToolOnDatabaseTest {

    private lateinit var database: FildyDatabase

    @Before
    fun setUp() = runBlocking {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun replacesTheDay_thenUndoRestores() = runBlocking {
        val repository = rainfallRepository(database)
        val tool = SetRainfallTool(repository, Json { ignoreUnknownKeys = true }, testClock)
        val lastNight = LocalDate.of(2026, 11, 15) // seeded with 18 mm

        val data = tool.execute("""{"date":"2026-11-15","millimeters":20}""").data as AssistantData.RainfallSaved

        assertEquals(20.0, repository.getRainfall(lastNight)!!.millimeters, 0.0)
        assertEquals(18.0, data.update.previous!!.millimeters, 0.0)
        assertEquals(8, repository.observeRecent().first().size) // still one record per day

        repository.undo(data.update)
        assertEquals(18.0, repository.getRainfall(lastNight)!!.millimeters, 0.0)
    }

    @Test
    fun newDay_andUndoRemovesIt() = runBlocking {
        val repository = rainfallRepository(database)
        val tool = SetRainfallTool(repository, Json { ignoreUnknownKeys = true }, testClock)

        val data = tool.execute("""{"date":"2026-11-16","millimeters":7.5}""").data as AssistantData.RainfallSaved
        assertEquals(9, repository.observeRecent().first().size)

        repository.undo(data.update)
        assertEquals(8, repository.observeRecent().first().size)
    }
}
