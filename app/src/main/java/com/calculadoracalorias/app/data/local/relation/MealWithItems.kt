package com.calculadoracalorias.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity

/**
 * Representa la relación uno a muchos entre una cabecera de comida (MealEntryEntity)
 * y sus ítems de alimentos individuales (MealFoodItemEntity).
 */
data class MealWithItems(
    @Embedded val meal: MealEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealEntryId"
    )
    val items: List<MealFoodItemEntity>
)
