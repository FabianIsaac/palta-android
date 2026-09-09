package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.SupplementNutritionEstimate
import com.calculadoracalorias.app.domain.repository.SupplementNutritionAnalyzer

/**
 * Caso de uso para estimar automáticamente dosis y aporte nutricional de un suplemento.
 * Opera con un analizador remoto (MiniMax LLM) si está disponible y cuenta con un catálogo
 * heurístico local offline como respaldo inmediato.
 */
class EstimateSupplementNutritionUseCase(
    private val remoteAnalyzer: SupplementNutritionAnalyzer? = null
) {
    suspend operator fun invoke(nameOrDescription: String): Result<SupplementNutritionEstimate> {
        val trimmed = nameOrDescription.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa el nombre o descripción del suplemento."))
        }

        // Intento con analizador remoto si está provisto
        if (remoteAnalyzer != null) {
            val remoteResult = remoteAnalyzer.estimateSupplement(trimmed)
            if (remoteResult.isSuccess) {
                return remoteResult
            }
        }

        // Fallback heurístico local offline siempre disponible
        return Result.success(estimateWithLocalHeuristics(trimmed))
    }

    fun estimateWithLocalHeuristics(query: String): SupplementNutritionEstimate {
        val normalized = query.lowercase().trim()
        val capitalizedName = query.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return when {
            normalized.contains("creatina") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "5g",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0
            )
            normalized.contains("magnesio") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "400mg",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0
            )
            normalized.contains("omega") || normalized.contains("aceite de pescado") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "2 cápsulas",
                calories = 18.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 2.0
            )
            normalized.contains("proteina") || normalized.contains("proteína") ||
            normalized.contains("whey") || normalized.contains("caseina") ||
            normalized.contains("caseína") || normalized.contains("isolate") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "1 scoop (30g)",
                calories = 120.0,
                proteinGrams = 24.0,
                carbsGrams = 2.0,
                fatGrams = 1.5
            )
            normalized.contains("colageno") || normalized.contains("colágeno") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "10g",
                calories = 36.0,
                proteinGrams = 9.0,
                carbsGrams = 0.0,
                fatGrams = 0.0
            )
            normalized.contains("bcaa") || normalized.contains("aminoacidos") || normalized.contains("aminoácidos") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "5g",
                calories = 20.0,
                proteinGrams = 5.0,
                carbsGrams = 0.0,
                fatGrams = 0.0
            )
            normalized.contains("multivitaminico") || normalized.contains("multivitamínico") ||
            normalized.contains("vitamina") || normalized.contains("zinc") ||
            normalized.contains("hierro") || normalized.contains("calcio") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "1 comprimido",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0
            )
            normalized.contains("cafeina") || normalized.contains("cafeína") ||
            normalized.contains("pre entreno") || normalized.contains("pre-entreno") ||
            normalized.contains("preworkout") || normalized.contains("pre-workout") -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "1 scoop (5g)",
                calories = 5.0,
                proteinGrams = 0.0,
                carbsGrams = 1.0,
                fatGrams = 0.0
            )
            else -> SupplementNutritionEstimate(
                name = capitalizedName,
                dosageDescription = "1 porción",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0
            )
        }
    }
}
