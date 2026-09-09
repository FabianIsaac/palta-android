package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Caso de uso para el cálculo proporcional determinista de porciones de alimentos.
 * Permite recalcular un ítem individual o actualizar una lista de ítems según su identificador.
 */
class RecalculatePortionUseCase {

    /**
     * Recalcula un ítem individual con una nueva porción en gramos.
     * Garantiza que el gramaje no sea negativo.
     */
    operator fun invoke(item: ScannedFoodItem, newGrams: Double): ScannedFoodItem {
        val sanitizedGrams = if (newGrams < 0.0 || newGrams.isNaN()) 0.0 else roundToDecimals(newGrams, 1)
        val updatedHouseholdPortion = item.householdPortion?.let { hp ->
            val ratio = if (hp.equivalentGrams > 0.0) sanitizedGrams / hp.equivalentGrams else 1.0
            hp.copy(
                quantity = roundToDecimals(hp.quantity * ratio, 2),
                equivalentGrams = sanitizedGrams
            )
        }
        return item.copy(
            servingGrams = sanitizedGrams,
            householdPortion = updatedHouseholdPortion
        )
    }

    /**
     * Actualiza la porción de un alimento dentro de una lista de ítems detectados.
     */
    operator fun invoke(
        items: List<ScannedFoodItem>,
        itemId: String,
        newGrams: Double
    ): List<ScannedFoodItem> {
        return items.map { item ->
            if (item.id == itemId) {
                invoke(item, newGrams)
            } else {
                item
            }
        }
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        return BigDecimal(value.toString())
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}
