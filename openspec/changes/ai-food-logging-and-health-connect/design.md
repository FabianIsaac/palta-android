# Diseño Técnico: ai-food-logging-and-health-connect

## 1. Arquitectura General y Separación de Capas

El diseño sigue estrictamente los principios de **Clean Architecture** y **Unidirectional Data Flow (UDF)**. La lógica de negocio y cálculo permanece en la capa de `domain` en Kotlin puro, sin dependencias del SDK de Android.

```
+-----------------------------------------------------------------+
|                    CAPA DE PRESENTACIÓN (UI)                    |
|                                                                 |
|   FoodCameraScreen / FoodScanReviewScreen                       |
|   FoodScanReviewViewModel                                       |
|     - Expone StateFlow<FoodScanReviewUiState>                   |
|     - Recibe FoodScanReviewEvent                                |
+-----------------------------------------------------------------+
                                |
                                v consume / despacha
+-----------------------------------------------------------------+
|                    CAPA DE DOMINIO (DOMAIN)                     |
|                                                                 |
|   Modelos:                                                      |
|     - ScannedFoodItem, DetectedMealResult, NutritionSummary     |
|   Interfaces:                                                   |
|     - FoodVisionAnalyzer                                        |
|     - HealthConnectRepository                                   |
|     - MealRepository                                            |
|   Casos de Uso:                                                 |
|     - AnalyzeFoodImageUseCase                                   |
|     - RecalculatePortionUseCase                                 |
|     - SaveMealWithHealthSyncUseCase                             |
+-----------------------------------------------------------------+
                                |
                                v implementa
+-----------------------------------------------------------------+
|                     CAPA DE DATOS (DATA)                        |
|                                                                 |
|   Vision:                                                       |
|     - LocalLiteRtVisionAnalyzer (MediaPipe / LiteRT)            |
|     - MiniMaxVisionAnalyzer (Ktor / Retrofit HTTP Client)       |
|     - FoodVisionAnalyzerFactory / Selector                      |
|   Health Connect:                                               |
|     - AndroidHealthConnectRepository (ConnectClient)            |
|   Local Database:                                               |
|     - Room Database (MealDao, MealEntity)                       |
|   Settings / Preferences:                                       |
|     - UserPreferencesRepository (DataStore para API Keys)       |
+-----------------------------------------------------------------+
```

---

## 2. Contratos e Interfaces de Dominio (Pure Kotlin)

### 2.1. Modelos de Dominio
```kotlin
data class ScannedFoodItem(
    val id: String,
    val name: String,
    val servingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val confidence: Float
) {
    val totalCalories: Double get() = (caloriesPer100g * servingGrams) / 100.0
    val totalProtein: Double get() = (proteinPer100g * servingGrams) / 100.0
    val totalCarbs: Double get() = (carbsPer100g * servingGrams) / 100.0
    val totalFat: Double get() = (fatPer100g * servingGrams) / 100.0
}

data class DetectedMealResult(
    val items: List<ScannedFoodItem>,
    val suggestedMealType: MealCategory,
    val analysisSource: VisionSource // LOCAL, MINIMAX
)

enum class MealCategory {
    DESAYUNO,
    ALMUERZO,
    ONCE_CENA,
    COLACIONES
}

enum class VisionSource {
    LOCAL_DEVICE,
    MINIMAX_CLOUD
}
```

### 2.2. Interfaces de Abstracción
```kotlin
interface FoodVisionAnalyzer {
    suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult>
}

interface HealthConnectRepository {
    suspend fun isHealthConnectAvailable(): Boolean
    suspend fun hasWriteNutritionPermission(): Boolean
    suspend fun writeNutritionRecord(
        mealName: String,
        mealCategory: MealCategory,
        timestamp: Long,
        calories: Double,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double
    ): Result<String> // Retorna el recordId de Health Connect
}
```

---

## 3. Implementaciones en Capa de Datos

### 3.1. Analizador MiniMax Multimodal
- **Endpoint:** MiniMax Vision / Chat Completion API con soporte de imágenes (`base64`).
- **Prompt:** Instrucción en formato System Prompt que exige respuesta en JSON estricto:
  ```json
  {
    "detected_items": [
      {
        "name": "Marraqueta",
        "serving_grams": 100.0,
        "calories_per_100g": 280.0,
        "protein_per_100g": 9.0,
        "carbs_per_100g": 56.0,
        "fat_per_100g": 1.0,
        "confidence": 0.95
      }
    ]
  }
  ```
- **Resilencia:** En caso de timeout o falta de conectividad, se reporta un error tipado permitiendo al usuario reintentar o cambiar al modo local.

### 3.2. Analizador Local (Edge)
- Emplea un modelo clasificador de alimentos embebido en el APK.
- Extrae la clase con mayor confianza y realiza una búsqueda de concordancia en el catálogo de alimentos local de Room para autocompletar la tabla nutricional base de 100g.
- Asigna una porción tentativa inicial de 100g para que el usuario la ajuste en la pantalla de revisión.

### 3.3. Integración con Health Connect
- Se utiliza `androidx.health.connect:connect-client`.
- Mapeo de `MealCategory` hacia constantes de Health Connect:
  - `DESAYUNO` -> `MealType.MEAL_TYPE_BREAKFAST`
  - `ALMUERZO` -> `MealType.MEAL_TYPE_LUNCH`
  - `ONCE_CENA` -> `MealType.MEAL_TYPE_DINNER`
  - `COLACIONES` -> `MealType.MEAL_TYPE_SNACK`
- Construcción del registro `NutritionRecord`:
  ```kotlin
  val record = NutritionRecord(
      startTime = Instant.ofEpochMilli(timestamp),
      startZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(timestamp)),
      endTime = Instant.ofEpochMilli(timestamp),
      endZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(timestamp)),
      energy = Energy.kilocalories(calories),
      totalCarbohydrate = Mass.grams(carbsGrams),
      totalFat = Mass.grams(fatGrams),
      protein = Mass.grams(proteinGrams),
      name = mealName,
      mealType = mappedMealType
  )
  ```

---

## 4. Flujo Unidireccional de UI (UDF en Compose)

### 4.1. Eventos de UI (`FoodScanReviewEvent`)
```kotlin
sealed interface FoodScanReviewEvent {
    data class OnPortionChanged(val itemId: String, val newGrams: Double) : FoodScanReviewEvent
    data class OnItemRemoved(val itemId: String) : FoodScanReviewEvent
    data class OnItemAdded(val item: ScannedFoodItem) : FoodScanReviewEvent
    data class OnMealCategoryChanged(val category: MealCategory) : FoodScanReviewEvent
    object OnConfirmAndSave : FoodScanReviewEvent
    object OnDismissError : FoodScanReviewEvent
}
```

### 4.2. Estado Inmutable de UI (`FoodScanReviewUiState`)
```kotlin
data class FoodScanReviewUiState(
    val isLoading: Boolean = false,
    val imagePath: String? = null,
    val items: List<ScannedFoodItem> = emptyList(),
    val selectedCategory: MealCategory = MealCategory.ALMUERZO,
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
```

### 4.3. Diagrama de Secuencia UDF

```
  Usuario            FoodScanReviewScreen        ViewModel             UseCases / Repos
     |                        |                      |                        |
     | 1. Cambia porción (g)  |                      |                        |
     |----------------------->|                      |                        |
     |                        | 2. OnPortionChanged  |                        |
     |                        |--------------------->|                        |
     |                        |                      | 3. Recalcula valores   |
     |                        |                      |    reactivamente       |
     |                        | 4. Nuevo UiState     |<-----------------------+
     |                        |<---------------------|                        |
     | 5. Re-renderiza UI     |                      |                        |
     |<-----------------------|                      |                        |
     |                        |                      |                        |
     | 6. Presiona "Guardar"  |                      |                        |
     |----------------------->|                      |                        |
     |                        | 7. OnConfirmAndSave  |                        |
     |                        |--------------------->|                        |
     |                        |                      | 8. Guarda en Room      |
     |                        |                      |----------------------->|
     |                        |                      | 9. Sincroniza HConnect |
     |                        |                      |----------------------->|
     |                        | 10. UiState(success) |                        |
     |                        |<---------------------|                        |
```
