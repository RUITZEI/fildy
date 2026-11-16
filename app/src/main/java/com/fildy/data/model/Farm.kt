package com.fildy.data.model

data class Farm(
    val id: Long,
    val name: String,
    val locality: String,
    val province: String,
)

data class Paddock(
    val id: Long,
    val farmId: Long,
    val name: String,
)

/** Vacas, vaquillonas, terneros, novillos, toros. */
enum class CattleCategory { COW, HEIFER, CALF, STEER, BULL }
