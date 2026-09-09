package com.calculadoracalorias.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosageDescription: String,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val isActive: Boolean,
    val isCustom: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
