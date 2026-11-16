package com.fildy.data.model

import java.time.Instant

/** A change to the herd, kept so it can be undone and shown as history. */
sealed interface StockChange {
    val id: Long
    val category: CattleCategory
    val recordedAt: Instant

    /** [headCount] animals moved from [fromPaddockId] to [toPaddockId]. */
    data class Move(
        override val id: Long,
        override val category: CattleCategory,
        override val recordedAt: Instant,
        val fromPaddockId: Long,
        val toPaddockId: Long,
        val headCount: Int,
    ) : StockChange

    /** The count in [paddockId] was updated from [previousHeadCount] to [newHeadCount]. */
    data class Correction(
        override val id: Long,
        override val category: CattleCategory,
        override val recordedAt: Instant,
        val paddockId: Long,
        val previousHeadCount: Int,
        val newHeadCount: Int,
    ) : StockChange
}
