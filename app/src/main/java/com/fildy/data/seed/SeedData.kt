package com.fildy.data.seed

import com.fildy.data.model.CattleCategory
import kotlinx.serialization.Serializable

/** Shape of `assets/seed/farm_seed.json`. */
@Serializable
data class SeedData(
    val farm: SeedFarm,
    val paddocks: List<SeedPaddock>,
    val herd: List<SeedHerdCount>,
    val rainfall: List<SeedRainfall>,
)

@Serializable
data class SeedFarm(val id: Long, val name: String, val locality: String, val province: String)

@Serializable
data class SeedPaddock(val id: Long, val name: String)

@Serializable
data class SeedHerdCount(val paddockId: Long, val category: CattleCategory, val headCount: Int)

/** [date] in ISO format, e.g. "2026-11-15". */
@Serializable
data class SeedRainfall(val date: String, val millimeters: Double)
