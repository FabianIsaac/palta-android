package com.calculadoracalorias.app.data.local.entity

import androidx.room.Entity

/**
 * Entidad Room para el registro de suplementos tomados en una fecha específica (YYYY-MM-DD).
 */
@Entity(
    tableName = "supplement_logs",
    primaryKeys = ["date", "supplementId"]
)
data class SupplementLogEntity(
    val date: String,
    val supplementId: String,
    val takenTimestamp: Long
)
