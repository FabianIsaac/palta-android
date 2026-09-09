package com.calculadoracalorias.app.domain.model

/**
 * Modelo de dominio para suplementos nutricionales y complementos diarios (ej. Creatina, Omega 3, Proteína).
 */
data class Supplement(
    val id: String,
    val name: String,
    val dosageDescription: String,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val isActive: Boolean = true,
    val isCustom: Boolean = false,
    val isTakenToday: Boolean = false
) {
    companion object {
        val PRECONFIGURED_SUPPLEMENTS = listOf(
            Supplement(
                id = "creatina_monohidrato",
                name = "Creatina Monohidrato",
                dosageDescription = "5g",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "omega_3",
                name = "Omega 3",
                dosageDescription = "2 cápsulas",
                calories = 18.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 2.0,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "citrato_magnesio",
                name = "Citrato de Magnesio",
                dosageDescription = "400mg",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "proteina_whey",
                name = "Proteína Whey",
                dosageDescription = "1 scoop (30g)",
                calories = 120.0,
                proteinGrams = 24.0,
                carbsGrams = 2.0,
                fatGrams = 1.5,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "multivitaminico",
                name = "Multivitamínico",
                dosageDescription = "1 comprimido",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = false,
                isCustom = false
            )
        )

        val DEFAULT_OMEGA_3 = PRECONFIGURED_SUPPLEMENTS.first { it.id == "omega_3" }
    }
}
