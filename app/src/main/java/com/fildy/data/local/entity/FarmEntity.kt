package com.fildy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fildy.data.model.Farm

@Entity(tableName = "farm")
data class FarmEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val locality: String,
    val province: String,
)

fun FarmEntity.toDomain() = Farm(id = id, name = name, locality = locality, province = province)
