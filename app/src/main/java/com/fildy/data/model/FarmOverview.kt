package com.fildy.data.model

/** The farm with every paddock and how many animals of each category it holds. */
data class FarmOverview(
    val farm: Farm,
    val paddocks: List<PaddockStock>,
) {
    val totalHeadCount: Int get() = paddocks.sumOf { it.totalHeadCount }

    val headCountsByCategory: Map<CattleCategory, Int>
        get() = CattleCategory.entries.associateWith { category -> paddocks.sumOf { it.headCount(category) } }
}

data class PaddockStock(
    val paddock: Paddock,
    val headCounts: Map<CattleCategory, Int>,
) {
    val totalHeadCount: Int get() = headCounts.values.sum()

    fun headCount(category: CattleCategory): Int = headCounts[category] ?: 0
}
