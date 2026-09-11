# Diseño Técnico: Escaneo de Tablas Nutricionales y Mejoras en Suplementos

## 1. Arquitectura General y Flujo de Datos

Se mantiene el cumplimiento estricto del patrón **Clean Architecture + MVVM / UDF** y las pautas offline-first del proyecto.

```
+-----------------------------------------------------------------------------------+
|                                  CAPA DE UI                                       |
|                                                                                   |
|  [SupplementsScreen]                           [AddIngredientSheet (Personalizado)]|
|   - Sugerencias colapsables                     - Botón Escanear Tabla            |
|   - Badge: "X tomas registradas"                - Selector Cámara / Galería       |
|   - Botón Escanear Tabla (Cámara/Galería)                                         |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                              CAPA DE PRESENTACIÓN                                 |
|                                                                                   |
|  [SupplementsViewModel]                        [EditMealViewModel / ScanReview]   |
|   - State: isScanningLabel, intakeCounts        - Estado de carga y autocompletado|
|   - Eventos: OnScanLabelClicked, OnImageSelected                                  |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                 CAPA DE DOMINIO                                   |
|                                                                                   |
|  [AnalyzeNutritionLabelUseCase]           [GetSupplementIntakeCountUseCase]       |
|   - imageBytes -> NutritionLabelResult     - supplementId -> Int (total tomas)    |
|                                                                                   |
|  Interfaces: [NutritionLabelAnalyzer], [SupplementRepository]                     |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                  CAPA DE DATOS                                    |
|                                                                                   |
|  [OpenAiCompatibleNutritionLabelAnalyzer]      [Room: SupplementLogDao]           |
|   - Ktor Client + Multi-provider LLM            - SELECT COUNT(*) FROM            |
|   - Prompt nutricional especializado              supplement_logs WHERE ...       |
+-----------------------------------------------------------------------------------+
```

---

## 2. Modelos de Dominio y Contratos

### `NutritionLabelScanResult`
Ubicación: `com.calculadoracalorias.app.domain.model`
```kotlin
data class NutritionLabelScanResult(
    val productName: String? = null,
    val servingDescription: String, // Ej: "1 scoop (33g)" o "1 porción (30g)"
    val servingGrams: Double? = null, // Ej: 33.0
    val calories: Double,           // Ej: 110.0
    val proteinGrams: Double,       // Ej: 25.0
    val carbsGrams: Double,         // Ej: 1.0
    val fatGrams: Double            // Ej: 1.0
)
```

### `NutritionLabelAnalyzer` (Interface)
Ubicación: `com.calculadoracalorias.app.domain.repository`
```kotlin
interface NutritionLabelAnalyzer {
    suspend fun analyzeNutritionLabel(imageBytes: ByteArray): Result<NutritionLabelScanResult>
}
```

### `AnalyzeNutritionLabelUseCase`
Ubicación: `com.calculadoracalorias.app.domain.usecase`
Coordina la validación del arreglo de bytes y delega al analizador multimodal.

---

## 3. Implementación del Analizador Multimodal (`Data`)

### Especialización del Prompt Nutricional
En `OpenAiCompatibleNutritionLabelAnalyzer`, el prompt del sistema instruye al modelo de visión a comportarse como un experto en rotulado nutricional:
1. **Prioridad en Porción Habitual:** Si la tabla presenta dos columnas (ej. "Por 100g" y "Por Porción / 1 scoop"), debe extraer **estrictamente los valores correspondientes a la porción de consumo habitual**.
2. **Filtrado de Ruido:** Ignorar porcentajes de valor diario (`% DV`), vitaminas o minerales auxiliares, extrayendo únicamente calorías (`kcal`) y macronutrientes esenciales en gramos (`g`).
3. **Inferencia de Nombre:** Si en la imagen se observa el título del producto o envase (por ejemplo "Iso Whey 100%"), extraerlo en `product_name`.
4. **Estructura JSON Determinista:** Respuesta única y limpia en formato JSON:
```json
{
  "product_name": "Proteína Whey Vainilla",
  "serving_description": "1 scoop (33g)",
  "serving_grams": 33.0,
  "calories": 110.0,
  "protein": 25.0,
  "carbs": 1.0,
  "fat": 1.0
}
```

---

## 4. Contador de Tomas en la Rutina de Suplementos

### Modificaciones en Persistencia
En `SupplementLogDao`, añadir consulta reactiva o síncrona:
```kotlin
@Query("SELECT COUNT(*) FROM supplement_logs WHERE supplementId = :supplementId")
fun getIntakeCountForSupplement(supplementId: String): Flow<Int>

@Query("SELECT supplementId, COUNT(*) as count FROM supplement_logs GROUP BY supplementId")
fun getAllSupplementIntakeCounts(): Flow<List<SupplementIntakeCountTuple>>
```

### Exposición en `SupplementsUiState`
```kotlin
data class SupplementsUiState(
    // ... campos existentes ...
    val areSuggestionsExpanded: Boolean = false,
    val intakeCounts: Map<String, Int> = emptyMap(),
    val isScanningLabel: Boolean = false
)
```

---

## 5. Decisiones de Interfaz de Usuario (Jetpack Compose)

### Sugerencias Colapsables en `SupplementsScreen`
- Reemplazar la tarjeta estática por un encabezado expandible:
  - Fila con icono de bombilla (`💡`), título "Sugerencias populares (X disponibles)" y un icono animado de flecha (`Icons.Default.ExpandMore` / `ExpandLess`).
  - Al hacer clic, conmuta `areSuggestionsExpanded`.
  - Animación suave `AnimatedVisibility` para mostrar u ocultar los chips de suplementos preconfigurados.

### Tarjeta de Suplemento en Rutina Activa (`SupplementItemCard`)
- Añadir debajo del desglose de dosis y macros una insignia visual con el contador de tomas:
  - Formato: `"🏆 $count tomas registradas"` (o `"Sin tomas registradas"` si es 0).
  - Estilo sutil con tipografía `labelMedium` y color primario o secundario.

### Selector de Imagen Modal (Cámara o Galería)
- Diálogo modal accesible desde el formulario:
  - Opción 1: **Tomar foto con cámara** (`rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview())`).
  - Opción 2: **Elegir de la galería** (`rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())`).
- Al obtener el `Bitmap` o `Uri`, se convierte a `ByteArray` y se envía al evento del ViewModel correspondiente.
