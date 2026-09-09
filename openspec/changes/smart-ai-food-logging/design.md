# Diseño Técnico: Registro Inteligente con IA y Lenguaje Natural (smart-ai-food-logging)

## 1. Arquitectura General y Flujo Unidireccional (UDF)

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           Capa de Presentación                          │
│                                                                         │
│  [ FoodCameraScreen / FoodScanReviewScreen / QuickNaturalEntrySheet ]   │
│         │                                      ▲                        │
│   Eventos de Usuario                           │ Observa UiState        │
│         ▼                                      │                        │
│  [ FoodScanReviewViewModel ]                   │                        │
└────────────────────────────┬───────────────────┴────────────────────────┘
                             │ invoca
┌────────────────────────────▼────────────────────────────────────────────┐
│                             Capa de Dominio                             │
│                                                                         │
│  - ParseNaturalLanguageMealUseCase                                      │
│  - AnalyzeFoodImageUseCase                                              │
│  - RecalculatePortionUseCase                                            │
│  - Modelos: NaturalLanguageMealAnalyzer, ScannedFoodItem, HouseholdPortion│
└────────────────────────────┬────────────────────────────────────────────┘
                             │ implementa
┌────────────────────────────▼────────────────────────────────────────────┐
│                              Capa de Datos                              │
│                                                                         │
│  - Gemini / MiniMax NaturalLanguageMealAnalyzerImpl                     │
│  - FoodVisionAnalyzerFactory (MiniMax / Gemini + Fallback informativo)   │
│  - LocalFoodCatalogRepository (Expandido con café, té y medidas caseras) │
│  - Room Database (MealDao, DailyLogDao)                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Modelos de Dominio y Contratos

### 2.1. Soporte para Medidas Caseras (`HouseholdPortion`)
```kotlin
package com.calculadoracalorias.app.domain.model

enum class HouseholdUnit(val displayName: String) {
    GRAMS("g"),
    MILLILITERS("ml"),
    CUP("taza"),
    MUG("tazón"),
    TABLESPOON("cda"),
    TEASPOON("cdta"),
    UNIT("unidad"),
    PORTION("porción")
}

data class HouseholdPortion(
    val unit: HouseholdUnit,
    val quantity: Double,
    val equivalentGrams: Double
)
```

### 2.2. Interfaz de Análisis en Lenguaje Natural (`NaturalLanguageMealAnalyzer`)
```kotlin
package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.DetectedMealResult

interface NaturalLanguageMealAnalyzer {
    suspend fun analyzeTextDescription(description: String): Result<DetectedMealResult>
}
```

### 2.3. Caso de Uso: `ParseNaturalLanguageMealUseCase`
```kotlin
package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer

class ParseNaturalLanguageMealUseCase(
    private val analyzer: NaturalLanguageMealAnalyzer
) {
    suspend operator fun invoke(description: String): Result<DetectedMealResult> {
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa una descripción de tu comida."))
        }
        return analyzer.analyzeTextDescription(description.trim())
    }
}
```

---

## 3. Estrategia de IA y Prompts

### 3.1. Prompt para Reconocimiento de Lenguaje Natural
El analizador en la nube recibe el texto del usuario (ej: *"una taza de café negro con 1 de azúcar"*) y responde estrictamente con un JSON estructurado, calculando automáticamente las calorías y macronutrientes correspondientes:

```text
Eres un asistente nutricional experto adaptado a Chile.
El usuario describirá lo que comió o bebió en lenguaje cotidiano.
Tu tarea es:
1. Identificar cada alimento o bebida individual.
2. Si el usuario menciona medidas cotidianas ("1 taza", "medio pan", "un vaso"), conviértelo a gramos/mililitros estimados promedio.
3. Calcular calorías, proteínas, carbohidratos y grasas basados en tablas nutricionales estándar.
4. Si es una infusión simple sin azúcar (café negro, té), las calorías son insignificantes (~2 kcal).

Responde EXCLUSIVAMENTE con el siguiente JSON:
{
  "suggested_meal_category": "Desayuno",
  "detected_items": [
    {
      "name": "Café negro",
      "serving_grams": 200.0,
      "calories_per_100g": 1.0,
      "protein_per_100g": 0.1,
      "carbs_per_100g": 0.0,
      "fat_per_100g": 0.0,
      "confidence": 0.98
    }
  ]
}
```

---

## 4. Corrección del Analizador de Visión Local

### Problema:
Actualmente `LocalLiteRtVisionAnalyzer.classifyImage()` retorna siempre `listOf("pollo", "arroz")`.

### Solución:
- En lugar de inventar una detección falsa de alimentos que confunde al usuario, si no hay un modelo TFLite de visión real descargado localmente, el motor local debe indicar honestamente que no hay coincidencia visual confiable y sugerir ingresar el alimento por texto o buscar en el catálogo.
- La pantalla de cámara mostrará un estado visible: **"Modo IA en la Nube Activo"** o **"Modo Offline / Búsqueda"** para total transparencia.

---

## 5. Expansión del Catálogo Local (`LocalFoodCatalogRepository`)
Se agregan elementos clave de la rutina chilena:
- **Café solo / espresso:** 2 kcal / 100ml (P: 0.1g, C: 0.2g, G: 0.0g)
- **Café con leche descremada:** 35 kcal / 100ml (P: 3.4g, C: 4.8g, G: 0.2g)
- **Café con leche entera:** 62 kcal / 100ml (P: 3.2g, C: 4.7g, G: 3.5g)
- **Té solo (negro/verde/hierbas):** 1 kcal / 100ml (P: 0.0g, C: 0.2g, G: 0.0g)
- **Azúcar blanca:** 387 kcal / 100g (P: 0.0g, C: 100.0g, G: 0.0g) -> ~20 kcal por cucharadita (5g)
- **Endulzante / Sucralosa / Stevia:** 0 kcal / 100g
- **Leche descremada:** 34 kcal / 100ml
- **Yogur natural:** 59 kcal / 100g
