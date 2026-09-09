# Diseño Técnico: Ventana Inteligente de Comidas y Carga Inmediata de Foto

## 1. Arquitectura y Flujo General

El cambio afecta la capa de presentación (cámara y revisión de escaneo) y la capa de dominio (guardado y consolidación de comidas):

```text
[Cámara / Galería]
       | (Toque de captura / selección)
       v
  Deshabilitar botón de obturador
       |
       v
  Navegación inmediata a AppDestination.REVIEW con isLoading = true
       |
       v
+-------------------------------------------------------------+
|               Pantalla de Carga Espaciosa                   |
|                                                             |
|                         (Spinner)                           |
|                                                             |
|               Analizando tu plato con IA...                 |
|       Identificando ingredientes y porciones estimadas      |
|                                                             |
|   +-----------------------------------------------------+   |
|   | 💡 Tip nutricional chileno:                         |   |
|   | Las legumbres como las lentejas aportan fibra y     |   |
|   | proteína de absorción lenta.                        |   |
|   +-----------------------------------------------------+   |
+-------------------------------------------------------------+
       |
       | (Termina llamada a AnalyzeFoodImageUseCase)
       v
  FoodScanReviewScreen muestra los alimentos detectados
       |
       | ¿Existe comida previa en la misma categoría hace <= 30 min?
       +---> SÍ: Muestra banner "Se sumará a tu Almuerzo reciente (hace 15 min)"
       +---> NO: Sin banner de consolidación (será un registro nuevo)
       |
       v
  Usuario presiona "Confirmar y Guardar"
       |
       v
  SaveMealWithHealthSyncUseCase evalúa ventana temporal:
       |
       +---> <= 30 min: Actualiza comida existente (une items y sincroniza Health Connect)
       +---> > 30 min o primera comida: Inserta nuevo MealEntry
       |
       v
  FoodScanReviewViewModel limpia estrictamente su estado interno
       |
       v
  Navegación a AppDestination.SUMMARY
```

---

## 2. Decisiones Técnicas y Componentes

### 2.1. Carga Inmediata y Prevención de Doble Disparo
- En `FoodCameraScreen`:
  - Agregar un estado interno `isCapturing: Boolean`.
  - Al hacer clic en el botón de captura, `isCapturing = true` deshabilita el botón de inmediato y muestra un feedback visual en el obturador.
  - Al completarse la compresión o al seleccionar desde la galería, se notifica al contenedor principal.
- En `MainActivity`:
  - Al dispararse `onImageCaptured(imageBytes)`, `MainActivity` inmediatamente:
    1. Invoca `reviewViewModel.startImageAnalysis(imageBytes, pendingCategory)`.
    2. Cambia `currentScreen = AppDestination.REVIEW`.
  - El análisis asíncrono se ejecuta dentro de la corrutina del ViewModel o en background mientras el usuario ya se encuentra observando la pantalla de carga de revisión.

### 2.2. Pantalla de Carga Espaciosa y Cómoda
- En `FoodScanReviewScreen`, cuando `uiState.isLoading == true`:
  - Contenedor centrado con paddings generosos (`24.dp` a `32.dp`), buen espaciado vertical (`16.dp` a `24.dp`).
  - Spinner circular grande (`56.dp`).
  - Tipografía `titleMedium` / `bodyLarge` para el estado principal, y tarjeta con diseño de superficie tonal para los tips chilenos.
  - Nada de apretar elementos; diseño limpio, moderno y relajado.

### 2.3. Lógica de Consolidación de 30 Minutos (Domain)
- En `SaveMealWithHealthSyncUseCase` (o caso de uso complementario):
  - Recibe la categoría, el timestamp actual y los nuevos `items: List<ScannedFoodItem>`.
  - Consulta al repositorio si existe una comida previa en esa misma categoría para el día actual.
  - Se obtiene la comida más reciente de dicha categoría.
  - Regla:
    ```kotlin
    val THIRTY_MINUTES_MS = 30 * 60 * 1000L
    val recentMeal = mealRepository.getMostRecentMealForCategory(category, startOfDay, endOfDay)

    if (recentMeal != null && (currentTimestamp - recentMeal.timestamp) <= THIRTY_MINUTES_MS) {
        // CONSOLIDAR: Unir alimentos existentes + nuevos alimentos
        val consolidatedItems = recentMeal.items + items
        updateMealWithHealthSyncUseCase(
            mealId = recentMeal.id,
            category = category,
            timestamp = recentMeal.timestamp, // o timestamp actualizado
            items = consolidatedItems,
            existingHealthConnectRecordId = recentMeal.healthConnectRecordId
        )
    } else {
        // NUEVA COMIDA INDEPENDIENTE
        mealRepository.saveMeal(...)
    }
    ```
- En `FoodScanReviewViewModel`:
  - Al inicializarse o al cambiar la categoría seleccionada, consulta si existe una comida previa hace `<= 30 min` para actualizar en el `UiState`:
    `consolidateTargetMealMinutesAgo: Int?`
  - Si no es nulo, la UI muestra el banner espacioso informando de la consolidación automática.

### 2.4. Reset Estricto de Estado
- En `FoodScanReviewViewModel`:
  - Método `resetState()` que restaura `_uiState.value = FoodScanReviewUiState()`.
  - Se llama automáticamente al finalizar exitosamente el guardado (`OnConfirmAndSave`) y al abandonar la pantalla (`onNavigateBack`).
  - Al iniciar una descripción en lenguaje natural desde la cámara (`OnAnalyzeNaturalLanguage`), si la pantalla venía de un guardado anterior, inicia completamente limpia.
