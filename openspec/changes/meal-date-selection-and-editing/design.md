# Diseño Técnico: meal-date-selection-and-editing

## 1. Arquitectura y Principios de Diseño

El diseño se apega estrictamente a **Clean Architecture** y **Unidirectional Data Flow (UDF)**:
- La UI (Compose) no realiza cálculos de timestamps directamente; emite eventos (`OnTargetDateChanged`, `OnDateChanged`).
- Los ViewModels actualizan de manera atómica el `UiState` inmutable y calculan los timestamps en base a la zona horaria del dispositivo (`ZoneId.systemDefault()`).
- Los casos de uso existentes (`SaveMealWithHealthSyncUseCase` y `UpdateMealWithHealthSyncUseCase`) ya reciben `timestamp: Long`, garantizando compatibilidad total sin alterar los casos de uso ni la capa de persistencia.

---

## 2. Flujo de Datos Unidireccional (UDF)

### A. Registro de Comida con Fecha Activa del Diario

```
[DailySummaryScreen]
  |  (summaryUiState.selectedDate = 2026-09-07)
  |  Usuario pulsa "Agregar a Almuerzo" o FAB de Cámara
  v
[MainActivity / AppNavigation]
  |  pendingMealCategory = MealCategory.ALMUERZO
  |  pendingMealDate = summaryUiState.selectedDate
  v
[FoodCameraScreen]
  |  Captura foto o ingresa descripción de texto
  v
[FoodScanReviewViewModel]
  |  initialize / startImageAnalysis(targetDate = pendingMealDate)
  |  _uiState.update { it.copy(targetDate = targetDate) }
  v
[FoodScanReviewScreen]
  |  Muestra badge/chip: "Registrando para: Ayer, 7 de septiembre"
  |  (Opcional) Usuario pulsa para cambiar la fecha -> emite OnTargetDateChanged(newDate)
  |  Usuario pulsa "Confirmar y Guardar" -> emite OnConfirmAndSave
  v
[FoodScanReviewViewModel.handleConfirmAndSave]
  |  timestamp = targetDate.atTime(LocalTime.now()).atZone(zoneId).toInstant().toEpochMilli()
  |  saveMealWithHealthSyncUseCase(..., timestamp = timestamp, ...)
  v
[Room Database / Health Connect]
  |  Registro insertado con timestamp del día objetivo (2026-09-07)
  v
[DailySummaryViewModel]
  |  mealsFlow reactivo en GetDailyMealSummaryUseCase emite el nuevo resumen de Ayer
  v
[DailySummaryScreen]
  |  La comida aparece reflejada inmediatamente en el día que el usuario estaba viendo.
```

### B. Edición y Cambio de Fecha de una Comida Existente

```
[DailySummaryScreen]
  |  Usuario pulsa en una tarjeta de comida (ej. Almuerzo)
  v
[EditMealScreen]
  |  EditMealViewModel carga la comida existente (id, categoría, timestamp original)
  |  Muestra fecha formateada: "Lunes, 7 de septiembre • 13:45"
  |  Usuario pulsa "Cambiar fecha"
  v
[DatePickerDialog (Material 3)]
  |  Usuario selecciona nueva fecha (ej. 2026-09-06)
  |  Emite EditMealEvent.OnDateChanged(newDate)
  v
[EditMealViewModel]
  |  val originalTime = Instant.ofEpochMilli(state.timestamp).atZone(zoneId).toLocalTime()
  |  val newTimestamp = newDate.atTime(originalTime).atZone(zoneId).toInstant().toEpochMilli()
  |  _uiState.update { it.copy(timestamp = newTimestamp) }
  |  Usuario pulsa "Guardar Cambios" -> OnSaveMealClicked
  v
[UpdateMealWithHealthSyncUseCase]
  |  Actualiza registro en Room y registro en Health Connect
  v
[DailySummaryViewModel]
  |  La comida se traslada automáticamente del día de origen al nuevo día.
```

---

## 3. Contratos de Interfaz y Estados

### A. `FoodScanReviewContract.kt`
```kotlin
// Modificaciones en FoodScanReviewUiState:
data class FoodScanReviewUiState(
    // ... campos existentes ...
    val targetDate: LocalDate = LocalDate.now(),
    // ...
)

// Nuevo evento en FoodScanReviewEvent:
sealed interface FoodScanReviewEvent {
    // ... eventos existentes ...
    data class OnTargetDateChanged(val date: LocalDate) : FoodScanReviewEvent
}
```

### B. `FoodScanReviewViewModel.kt`
- Métodos `startImageAnalysis`, `initializeWithResult` y `handleAnalyzeNaturalLanguage` aceptan `targetDate: LocalDate = LocalDate.now()`.
- En `handleConfirmAndSave`:
```kotlin
val targetTime = LocalTime.now()
val mealTimestamp = currentState.targetDate
    .atTime(targetTime)
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

val result = saveMealWithHealthSyncUseCase(
    mealName = currentState.selectedCategory.displayName,
    category = currentState.selectedCategory,
    timestamp = mealTimestamp,
    items = currentState.items,
    rawDescription = currentState.lastRawDescription,
    isPendingAiRefinement = currentState.isPendingAiRefinement
)
```

### C. `EditMealContract.kt`
```kotlin
sealed interface EditMealEvent {
    // ... eventos existentes ...
    data class OnDateChanged(val newDate: LocalDate) : EditMealEvent
}
```

### D. `EditMealViewModel.kt`
```kotlin
is EditMealEvent.OnDateChanged -> {
    val zoneId = ZoneId.systemDefault()
    val originalTime = try {
        Instant.ofEpochMilli(_uiState.value.timestamp).atZone(zoneId).toLocalTime()
    } catch (_: Exception) {
        LocalTime.now()
    }
    val newTimestamp = event.newDate
        .atTime(originalTime)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()

    _uiState.update { it.copy(timestamp = newTimestamp) }
}
```

---

## 4. Diseño de Componentes de UI (Jetpack Compose)

### Visualización y Selector en `FoodScanReviewScreen`
- Chip interactivo ubicado encima de los macronutrientes:
  - Icono de calendario (`Icons.Default.CalendarToday` o `Icons.Default.Event`).
  - Texto legible en español chileno:
    - Si es hoy: *"Hoy, 8 de septiembre"*
    - Si es ayer: *"Ayer, 7 de septiembre"*
    - Otra fecha: *"Lunes, 7 de septiembre"*
  - Al pulsar, abre un `DatePickerDialog` de Material 3 para ajustar la fecha.

### Visualización y Selector en `EditMealScreen`
- Fila en la tarjeta de metadatos o bajo la categoría:
  - Muestra la fecha y hora actual de la comida.
  - Botón o chip "Cambiar fecha" que activa el `DatePickerDialog`.

---

## 5. Localización y Textos (`strings.xml`)
Se agregarán las cadenas correspondientes en español chileno sin voseo:
- `label_target_date`: *"Fecha de la comida"*
- `label_change_date`: *"Cambiar fecha"*
- `label_registering_for_date`: *"Registrando para: %1$s"*
- `label_yesterday`: *"Ayer"*
- `dialog_select_date_title`: *"Selecciona la fecha"*
