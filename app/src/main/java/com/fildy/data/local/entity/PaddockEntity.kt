package com.fildy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fildy.data.model.Paddock

@Entity(
    tableName = "paddock",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("farmId")],
)
data class PaddockEntity(
    @PrimaryKey val id: Long,
    val farmId: Long,
    val name: String,
)

fun PaddockEntity.toDomain() = Paddock(id = id, farmId = farmId, name = name)
