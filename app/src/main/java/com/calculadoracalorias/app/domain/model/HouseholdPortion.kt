package com.calculadoracalorias.app.domain.model

import java.util.Locale

/**
 * Unidades caseras cotidianas para facilitar el registro amigable sin requerir gramajes crudos.
 */
enum class HouseholdUnit(
    val displayName: String,
    val defaultGrams: Double
) {
    GRAMS("g", 1.0),
    MILLILITERS("ml", 1.0),
    CUP("taza", 200.0),
    MUG("tazón", 350.0),
    TABLESPOON("cda", 15.0),
    TEASPOON("cdta", 5.0),
    UNIT("unidad", 100.0),
    PORTION("porción", 150.0);

    companion object {
        fun fromDisplayName(name: String): HouseholdUnit? {
            val normalized = name.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull {
                it.displayName.lowercase(Locale.ROOT) == normalized ||
                it.name.lowercase(Locale.ROOT) == normalized ||
                (normalized == "tazas" && it == CUP) ||
                (normalized == "tazones" && it == MUG) ||
                (normalized == "cdas" && it == TABLESPOON) ||
                (normalized == "cucharada" && it == TABLESPOON) ||
                (normalized == "cucharadas" && it == TABLESPOON) ||
                (normalized == "cdtas" && it == TEASPOON) ||
                (normalized == "cucharadita" && it == TEASPOON) ||
                (normalized == "cucharaditas" && it == TEASPOON) ||
                (normalized == "unidades" && it == UNIT) ||
                (normalized == "porciones" && it == PORTION)
            }
        }
    }
}

/**
 * Representa una porción expresada en unidades familiares con su equivalencia estimada en gramos o ml.
 */
data class HouseholdPortion(
    val unit: HouseholdUnit,
    val quantity: Double,
    val equivalentGrams: Double
) {
    fun toDisplayText(): String {
        val qtyString = if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", quantity)
        }

        val unitLabel = if (quantity > 1.0) {
            when (unit) {
                HouseholdUnit.CUP -> "tazas"
                HouseholdUnit.MUG -> "tazones"
                HouseholdUnit.TABLESPOON -> "cdas"
                HouseholdUnit.TEASPOON -> "cdtas"
                HouseholdUnit.UNIT -> "unidades"
                HouseholdUnit.PORTION -> "porciones"
                else -> unit.displayName
            }
        } else {
            unit.displayName
        }

        val gramsFormatted = if (equivalentGrams % 1.0 == 0.0) {
            equivalentGrams.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", equivalentGrams)
        }

        return when (unit) {
            HouseholdUnit.GRAMS -> "$qtyString g"
            HouseholdUnit.MILLILITERS -> "$qtyString ml"
            HouseholdUnit.CUP, HouseholdUnit.MUG -> "$qtyString $unitLabel (~$gramsFormatted ml)"
            else -> "$qtyString $unitLabel (~$gramsFormatted g)"
        }
    }

    companion object {
        fun calculateEquivalentGrams(unit: HouseholdUnit, quantity: Double, gramPerUnit: Double? = null): Double {
            val baseGrams = gramPerUnit ?: unit.defaultGrams
            return (quantity * baseGrams).coerceAtLeast(0.0)
        }
    }
}
