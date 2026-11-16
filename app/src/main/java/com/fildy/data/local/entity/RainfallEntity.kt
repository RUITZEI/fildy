package com.fildy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fildy.data.model.RainfallRecord
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "rainfall",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // One record per farm and day. The unique index also serves the farmId foreign key; the
    // date index serves the "last N days" range query.
    indices = [Index(value = ["farmId", "date"], unique = true), Index("date")],
)
data class RainfallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val date: LocalDate,
    val millimeters: Double,
    val recordedAt: Instant,
)

fun RainfallRecord.toEntity() = RainfallEntity(
    id = id,
    farmId = farmId,
    date = date,
    millimeters = millimeters,
    recordedAt = recordedAt,
)

fun RainfallEntity.toDomain() = RainfallRecord(
    id = id,
    farmId = farmId,
    date = date,
    millimeters = millimeters,
    recordedAt = recordedAt,
)
