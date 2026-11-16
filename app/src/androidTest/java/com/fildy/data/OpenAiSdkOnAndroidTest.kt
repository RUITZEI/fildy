package com.fildy.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fildy.data.assistant.AssistantException
import com.fildy.data.assistant.SystemPrompt
import com.fildy.data.assistant.openai.OpenAiAssistant
import com.fildy.data.assistant.openai.OpenAiConfig
import com.fildy.data.assistant.tools.RainfallBetweenDatesTool
import com.fildy.data.assistant.tools.RainfallOnDateTool
import com.fildy.data.local.FildyDatabase
import com.openai.errors.UnauthorizedException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the OpenAI Java SDK works on Android (Jackson, OkHttp 5, our tool schemas) by sending a
 * real request with an invalid key: OpenAI must answer 401. Uses no credits.
 */
@RunWith(AndroidJUnit4::class)
class OpenAiSdkOnAndroidTest {

    private lateinit var database: FildyDatabase

    @Before
    fun setUp() = runBlocking {
        database = inMemoryDatabase()
        seeder(database).seedIfEmpty()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun invalidKey_reachesOpenAiAndIsRejected() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val rainfall = rainfallRepository(database)
        val assistant = OpenAiAssistant(
            config = OpenAiConfig(apiKey = "sk-invalid-key-for-tests"),
            systemPrompt = SystemPrompt(farmRepository(database), testClock),
            tools = setOf(RainfallOnDateTool(rainfall, json), RainfallBetweenDatesTool(rainfall, json)),
        )

        try {
            assistant.answer("¿Cuánto llovió ayer?")
            fail("Expected the invalid key to be rejected")
        } catch (e: AssistantException) {
            assertTrue("Expected a 401 from OpenAI, got: ${e.cause}", e.cause is UnauthorizedException)
        }
    }
}
