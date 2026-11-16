package com.fildy.ui.home.farm

import com.fildy.data.model.CattleCategory

/** What the "Mi campo" card shows. */
sealed interface FarmUiState {
    /** The farm isn't available yet (e.g. first launch, still seeding). */
    data object Loading : FarmUiState

    data class Success(
        val paddockCount: Int,
        val totalHeadCount: Int,
        /** Farm-wide head count per category, in [CattleCategory] order; every category included. */
        val headCounts: List<CategoryCount>,
    ) : FarmUiState
}

data class CategoryCount(val category: CattleCategory, val headCount: Int)
