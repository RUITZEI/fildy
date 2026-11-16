package com.fildy.data.seed

import com.fildy.data.model.CattleCategory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Checks the bundled seed file parses and matches the challenge's minimum data. */
class SeedFileTest {

    private val seed: SeedData = json.decodeFromString(File("src/main/assets/seed/farm_seed.json").readText())

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }

    @Test
    fun farm() {
        assertEquals("La Tranquera", seed.farm.name)
        assertEquals("Castelli", seed.farm.locality)
        assertEquals("Buenos Aires", seed.farm.province)
    }

    @Test
    fun eightPaddocks() {
        assertEquals(8, seed.paddocks.size)
        assertEquals(seed.paddocks.size, seed.paddocks.map { it.id }.toSet().size)
    }

    @Test
    fun aboutFourHundredFiftyHeadPerCategory() {
        val perCategory = seed.herd.groupBy { it.category }.mapValues { (_, counts) -> counts.sumOf { it.headCount } }
        assertEquals(CattleCategory.entries.toSet(), perCategory.keys)
        perCategory.forEach { (category, total) -> assertTrue("$category has $total", total in 440..460) }
    }

    @Test
    fun herdOnlyReferencesExistingPaddocks() {
        val paddockIds = seed.paddocks.map { it.id }.toSet()
        assertTrue(seed.herd.all { it.paddockId in paddockIds })
    }

    @Test
    fun rainfallHasOneRecordPerDay() {
        assertEquals(seed.rainfall.size, seed.rainfall.map { it.date }.toSet().size)
    }

    @Test
    fun rainfallInsideSixtyDaysAndMatchesDesign() {
        val dates = seed.rainfall.map { LocalDate.parse(it.date) }
        assertTrue(dates.max().toEpochDay() - dates.min().toEpochDay() < 60)
        assertEquals(131.0, seed.rainfall.sumOf { it.millimeters }, 0.0)
        assertEquals(18.0, seed.rainfall.maxBy { it.date }.millimeters, 0.0)
    }
}
