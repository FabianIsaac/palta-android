# Diseño Técnico: Descomposición Atómica de Ingredientes y Resiliencia Offline (offline-resilient-meal-decomposition)

## 1. Arquitectura General y Flujo Unidireccional de Datos (UDF)

El diseño mantiene la estricta separación de capas (Clean Architecture) y garantiza que la lógica de negocio y el cálculo de macronutrientes permanezcan en Kotlin puro dentro de la capa `domain`.

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Capa de Presentación                              │
│                                                                             │
│  [ FoodCameraScreen / QuickNaturalLanguageEntrySheet / FoodScanReviewScreen ]│
│         │                                               ▲                   │
│   Eventos de UI (OnAnalyze, OnRetry, OnSavePending)      │ Observa UiState   │
│         ▼                                               │                   │
│  [ FoodScanReviewViewModel / DailySummaryViewModel ]    │                   │
└──────────────────────────────┬──────────────────────────┴───────────────────┘
                               │ invoca
┌──────────────────────────────▼──────────────────────────────────────────────┐
│                             Capa de Dominio                                 │
│                                                                             │
│  - ParseNaturalLanguageMealUseCase                                          │
│  - RefinePendingMealsUseCase (Nuevo)                                        │
│  - RecalculatePortionUseCase                                                │
│  - SaveMealWithHealthSyncUseCase                                            │
│  - Contratos: NaturalLanguageMealAnalyzer, MealRepository, FoodCatalogRepo  │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │ implementa
┌──────────────────────────────▼──────────────────────────────────────────────┐
│                              Capa de Datos                                  │
│                                                                             │
│  - RemoteNaturalLanguageMealAnalyzer (Prompt optimizado para descomposición)│
│  - LocalFoodCatalogRepository (Catálogo expandido con fajitas, choclo, etc.)│
│  - Room Database (MealEntryEntity con rawDescription e isPendingAiRefinement)│
│  - ConnectivityMealSyncManager (Observador de red para auto-reintento)      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Contratos y Modelos de Dominio (Pure Kotlin)

### 2.1. Actualización de `MealEntry`
```kotlin
package com.calculadoracalorias.app.domain.model

data class MealEntry(
    val id: Long,
    val category: MealCategory,
    val timestamp: Long,
    val items: List<ScannedFoodItem>,
    val summary: NutritionSummary,
    val healthConnectRecordId: String? = null,
    val rawDescription: String? = null,
    val isPendingAiRefinement: Boolean = false
)
```

### 2.2. Nuevo Caso de Uso: `RefinePendingMealsUseCase`
```kotlin
package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer

class RefinePendingMealsUseCase(
    private val mealRepository: MealRepository,
    private val analyzer: NaturalLanguageMealAnalyzer
) {
    suspend operator fun invoke(): Result<List<MealEntry>> {
        val pendingMeals = mealRepository.getPendingRefinementMeals().getOrElse { return Result.failure(it) }
        val refined = mutableListOf<MealEntry>()

        for (meal in pendingMeals) {
            val text = meal.rawDescription ?: continue
            val analysisResult = analyzer.analyzeTextDescription(text)
            if (analysisResult.isSuccess) {
                val detected = analysisResult.getOrThrow()
                val updatedMeal = meal.copy(
                    items = detected.items,
                    summary = com.calculadoracalorias.app.domain.model.NutritionSummary.fromItems(detected.items),
                    isPendingAiRefinement = false
                )
                mealRepository.updateMeal(updatedMeal)
                refined.add(updatedMeal)
            }
        }
        return Result.success(refined)
    }
}
```

---

## 3. Estrategia de IA: Descomposición Atómica de Ingredientes

En [`RemoteNaturalLanguageMealAnalyzer.kt`](file:///Users/fabian/Documents/Desarrollo/calculadora-calorias/app/src/main/java/com/calculadoracalorias/app/data/remote/RemoteNaturalLanguageMealAnalyzer.kt), se reformula el prompt del sistema y se agregan directivas explícitas para preparaciones compuestas:

```text
Eres un asistente nutricional experto adaptado a Chile.
El usuario describirá lo que comió o bebió en lenguaje cotidiano chileno.

REGLAS ESTRICTAS DE DESCOMPOSICIÓN DE INGREDIENTES:
1. DESCOMPOSICIÓN DE PLATOS COMPUESTOS O ARMADOS:
   - Si el usuario menciona una preparación armada detallando sus componentes (ej: fajitas, tacos, sándwiches, ensaladas, bowls, burritos), NUNCA crees un solo ítem genérico combinado.
   - DEBES generar un ítem independiente en detected_items para:
     a) La base del plato (ej: "Tortillas de fajita", "Pan marraqueta", "Masa de taco").
     b) CADA proteína, vegetal, relleno, salsa o aderezo nombrado explícitamente (ej: "Carne molida", "Choclo", "Tomate picado", "Lechuga", "Yogurt griego", "Tajín").
2. ESTIMACIÓN COHERENTE DE PORCIONES:
   - Si el usuario indica cantidad para el conjunto (ej. "2 fajitas con..."), reparte porciones realistas que correspondan a esa cantidad total:
     * Base: 2 unidades de tortilla (~80g en total, 40g c/u).
     * Proteína: Porción típica de relleno (~100g de carne molida).
     * Vegetales: ~30g a 50g por vegetal mencionado.
     * Salsas/aderezos: ~30g a 50g (ej. 2 cucharadas de yogurt griego).
     * Condimentos: ~2g a 5g (ej. 1 cucharadita de tajín).
3. PLATOS TÍPICOS CERRADOS SIN INGREDIENTES DETALLADOS:
   - Si el usuario solo nombra un plato tradicional sin detallar ingredientes (ej: "una cazuela de ave", "un plato de porotos con riendas"), manténlo como un solo ítem consolidado.

FORMATO DE RESPUESTA EXCLUSIVO (JSON):
{
  "suggested_meal_category": "Once / Cena",
  "detected_items": [
    {
      "name": "Tortillas de fajita",
      "serving_grams": 80.0,
      "calories_per_100g": 300.0,
      "protein_per_100g": 8.0,
      "carbs_per_100g": 52.0,
      "fat_per_100g": 6.0,
      "confidence": 0.95,
      "household_unit": "unidad",
      "household_quantity": 2.0
    },
    {
      "name": "Carne molida cocida",
      "serving_grams": 100.0,
      "calories_per_100g": 250.0,
      "protein_per_100g": 26.0,
      "carbs_per_100g": 0.0,
      "fat_per_100g": 15.0,
      "confidence": 0.95,
      "household_unit": "porción",
      "household_quantity": 1.0
    }
  ]
}
```

---

## 4. Persistencia y Resiliencia en UI

### 4.1. Máquina de Estados en `FoodScanReviewViewModel`

```text
[ Ingreso de Texto ] ────> Estado: isAnalyzingText = true, lastRawDescription = text
                                      │
               ┌──────────────────────┴──────────────────────┐
               ▼                                             ▼
       [ Éxito con IA ]                             [ Fallo con IA ]
  - items = 7 ítems desglosados             - isAnalyzingText = false
  - isAnalyzingText = false                 - errorMessage = detalle del error
  - lastRawDescription = text              - fallbackItems = catálogo local (si hay)
                                            - lastRawDescription intacto
                                                             │
                                   ┌─────────────────────────┴────────────────────────┐
                                   ▼                                                  ▼
                        [ Botón: Reintentar ]                              [ Botón: Guardar Pendiente ]
                   Vuelve a consultar MiniMax                       Guarda con `isPendingAiRefinement = true`
```

### 4.2. Sincronización de Fondo al Recuperar Red
Al regresar la conexión a internet, un observador de conectividad activa `RefinePendingMealsUseCase`:
1. Consulta las comidas donde `isPendingAiRefinement == true`.
2. Llama a `RemoteNaturalLanguageMealAnalyzer` con el texto guardado.
3. Actualiza los ítems y los macros en la base de datos local y sincroniza con Health Connect si los permisos están concedidos.
