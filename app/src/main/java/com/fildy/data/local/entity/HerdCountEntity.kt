package com.fildy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.fildy.data.model.CattleCategory

/** One row per paddock + category. Rows with zero animals are deleted. */
@Entity(
    tableName = "herd_count",
    primaryKeys = ["paddockId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = PaddockEntity::class,
            parentColumns = ["id"],
            childColumns = ["paddockId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HerdCountEntity(
    val paddockId: Long,
    val category: CattleCategory,
    val headCount: Int,
)
