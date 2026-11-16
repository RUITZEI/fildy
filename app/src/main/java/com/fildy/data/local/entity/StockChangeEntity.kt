package com.fildy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.StockChange
import java.time.Instant

enum class StockChangeType { MOVE, CORRECTION }

/**
 * Both kinds of [StockChange] in one table. Columns only one type uses are nullable:
 * - MOVE: [paddockId] is the source, [toPaddockId] the destination, [headCount] the animals moved.
 * - CORRECTION: [paddockId] is the corrected paddock, [headCount] the new count and
 *   [previousHeadCount] the count before, so it can be undone.
 */
@Entity(
    tableName = "stock_change",
    foreignKeys = [
        ForeignKey(
            entity = PaddockEntity::class,
            parentColumns = ["id"],
            childColumns = ["paddockId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PaddockEntity::class,
            parentColumns = ["id"],
            childColumns = ["toPaddockId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("paddockId"), Index("toPaddockId")],
)
data class StockChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: StockChangeType,
    val category: CattleCategory,
    val paddockId: Long,
    val toPaddockId: Long?,
    val headCount: Int,
    val previousHeadCount: Int?,
    val recordedAt: Instant,
)

fun StockChangeEntity.toDomain(): StockChange = when (type) {
    StockChangeType.MOVE -> StockChange.Move(
        id = id,
        category = category,
        recordedAt = recordedAt,
        fromPaddockId = paddockId,
        toPaddockId = checkNotNull(toPaddockId) { "MOVE $id without toPaddockId" },
        headCount = headCount,
    )
    StockChangeType.CORRECTION -> StockChange.Correction(
        id = id,
        category = category,
        recordedAt = recordedAt,
        paddockId = paddockId,
        previousHeadCount = checkNotNull(previousHeadCount) { "CORRECTION $id without previousHeadCount" },
        newHeadCount = headCount,
    )
}

fun StockChange.toEntity(): StockChangeEntity = when (this) {
    is StockChange.Move -> StockChangeEntity(
        id = id,
        type = StockChangeType.MOVE,
        category = category,
        paddockId = fromPaddockId,
        toPaddockId = toPaddockId,
        headCount = headCount,
        previousHeadCount = null,
        recordedAt = recordedAt,
    )
    is StockChange.Correction -> StockChangeEntity(
        id = id,
        type = StockChangeType.CORRECTION,
        category = category,
        paddockId = paddockId,
        toPaddockId = null,
        headCount = newHeadCount,
        previousHeadCount = previousHeadCount,
        recordedAt = recordedAt,
    )
}
