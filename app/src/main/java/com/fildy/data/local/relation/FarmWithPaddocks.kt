package com.fildy.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fildy.data.local.entity.FarmEntity
import com.fildy.data.local.entity.HerdCountEntity
import com.fildy.data.local.entity.PaddockEntity
import com.fildy.data.local.entity.toDomain
import com.fildy.data.model.CattleCategory
import com.fildy.data.model.FarmOverview
import com.fildy.data.model.PaddockStock

/** A paddock with its head counts, loaded by Room through [Relation]. */
data class PaddockWithHerd(
    @Embedded val paddock: PaddockEntity,
    @Relation(parentColumn = "id", entityColumn = "paddockId")
    val herd: List<HerdCountEntity>,
)

/** The farm with its paddocks and their head counts, loaded by Room in one go. */
data class FarmWithPaddocks(
    @Embedded val farm: FarmEntity,
    @Relation(entity = PaddockEntity::class, parentColumn = "id", entityColumn = "farmId")
    val paddocks: List<PaddockWithHerd>,
)

fun FarmWithPaddocks.toDomain() = FarmOverview(
    farm = farm.toDomain(),
    // Room doesn't guarantee the order of related rows.
    paddocks = paddocks.sortedBy { it.paddock.id }.map { it.toDomain() },
)

fun PaddockWithHerd.toDomain(): PaddockStock {
    val counts = herd.associate { it.category to it.headCount }
    return PaddockStock(
        paddock = paddock.toDomain(),
        // Categories without a row have zero animals.
        headCounts = CattleCategory.entries.associateWith { counts[it] ?: 0 },
    )
}
