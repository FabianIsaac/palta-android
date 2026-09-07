package com.calculadoracalorias.app.domain.model

/**
 * Categorías de comidas estándar adaptadas al uso cultural en Chile (es-CL).
 */
enum class MealCategory(val displayName: String) {
    DESAYUNO("Desayuno"),
    ALMUERZO("Almuerzo"),
    ONCE_CENA("Once / Cena"),
    COLACIONES("Colaciones");

    companion object {
        fun fromDisplayName(name: String): MealCategory {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: ALMUERZO
        }
    }
}
