# Propuesta: meal-date-selection-and-editing

## 1. Contexto y Justificación

Actualmente, cuando el usuario navega en el resumen diario (`DailySummaryScreen`) a un día previo (por ejemplo, "Ayer") e intenta registrar una comida —ya sea pulsando "+ Agregar a [Categoría]" o el botón flotante de la cámara—, la comida termina guardándose en el día actual ("Hoy").

### Causa Raíz
1. **Pérdida de la fecha en la navegación:** En `MainActivity.kt`, al transicionar a `AppDestination.CAMERA`, únicamente se almacena `pendingMealCategory`. La fecha observada en el diario (`summaryUiState.selectedDate`) se descarta.
2. **Timestamp fijado a tiempo real:** En `FoodScanReviewViewModel.kt`, el método `handleConfirmAndSave()` invoca `saveMealWithHealthSyncUseCase` utilizando directamente `timestamp = System.currentTimeMillis()`. Como consecuencia, la comida se guarda con la fecha y hora exacta del momento actual, apareciendo en el resumen de hoy y dejando el día anterior sin el registro esperado.
3. **Falta de visibilidad y control de fecha:** En `FoodScanReviewScreen`, no se indica la fecha para la cual se está guardando la comida, impidiendo al usuario verificar o modificar el día.
4. **Imposibilidad de corregir la fecha en comidas existentes:** En `EditMealScreen`, el usuario puede modificar porciones y categoría, pero no puede mover la comida a otra fecha si fue registrada accidentalmente en un día erróneo.

---

## 2. Alcance (Scope)

### Dentro del Alcance (In-Scope)
- **Propagación de fecha activa:** Capturar la fecha actualmente seleccionada en `DailySummaryScreen` (`summaryUiState.selectedDate`) y transferirla al flujo de registro de comida (`MainActivity -> FoodCameraScreen -> FoodScanReviewViewModel`).
- **Fecha objetivo en revisión de comida (`FoodScanReviewUiState`):** 
  - Almacenar `targetDate: LocalDate` en el estado de revisión.
  - Al guardar la comida, calcular el `timestamp` combinando `targetDate` con la hora local actual (`LocalTime.now()`).
- **Indicador y selector de fecha en `FoodScanReviewScreen`:**
  - Mostrar visualmente la fecha en la que se registrará la comida (ej. *"Registrando para: Ayer, 7 de septiembre"* o *"Hoy, 8 de septiembre"*).
  - Permitir al usuario cambiar la fecha mediante un diálogo selector de fecha (`DatePickerDialog` de Material 3).
- **Edición de fecha en `EditMealScreen`:**
  - Mostrar la fecha actual del registro de comida.
  - Proveer acción y diálogo para cambiar la fecha de la comida.
  - Actualizar el `timestamp` en Room y sincronizar con Health Connect para que la comida se traslade automáticamente al resumen del día correspondiente.
- **Idioma y localización:** Todos los textos y diálogos en Español Chileno (`es-CL`) estricto sin voseo.

### Fuera del Alcance (Out-of-Scope)
- Modificación de las fórmulas de presupuesto calórico o cálculo de macronutrientes.
- Migraciones de esquema en la base de datos Room (el campo `timestamp: Long` ya soporta cualquier instante de tiempo).
- Edición de la hora exacta minuto a minuto (la edición se enfoca en el cambio de día/fecha calendario, manteniendo la hora del registro o la hora actual).

---

## 3. Impacto en Base de Datos y Persistencia

- **Esquema Room:** No requiere cambios ni migraciones de versión en Room. `MealEntryEntity` ya cuenta con el campo `timestamp: Long` (milisegundos epoch), el cual se utiliza para delimitar las comidas entre el inicio (`startOfDay`) y el fin (`endOfDay`) del día consultado.
- **Consultas DAOs existentes:** Compatibilidad del 100%. `getMealsWithItemsBetween(startTime, endTime)` responderá de forma reactiva al cambio de fecha de cualquier registro.

---

## 4. Componentes de UI y Estados de Compose Afectados

- **`MainActivity.kt` (`AppNavigation`):**
  - Nuevo estado: `pendingMealDate: LocalDate?`.
  - Conexión de `summaryUiState.selectedDate` hacia `FoodCameraScreen` y `FoodScanReviewViewModel`.
- **`DailySummaryScreen.kt`:**
  - `ChileanMealCategoryCard` propaga la fecha actual del resumen junto a la categoría.
- **`FoodScanReviewContract.kt` y `FoodScanReviewViewModel.kt`:**
  - `FoodScanReviewUiState`: nuevo campo `targetDate: LocalDate = LocalDate.now()`.
  - Nuevo evento: `FoodScanReviewEvent.OnTargetDateChanged(val date: LocalDate)`.
  - `handleConfirmAndSave`: cálculo de timestamp usando `targetDate`.
- **`FoodScanReviewScreen.kt`:**
  - Chip / tarjeta informativa de fecha con acción para abrir `DatePickerDialog`.
- **`EditMealContract.kt` y `EditMealViewModel.kt`:**
  - Nuevo evento: `EditMealEvent.OnDateChanged(val newDate: LocalDate)`.
  - Recálculo del `timestamp` en `EditMealUiState`.
- **`EditMealScreen.kt`:**
  - Sección visual para ver la fecha asignada y botón "Cambiar fecha" con `DatePickerDialog`.

---

## 5. Impacto en Fórmulas y Métricas Nutricionales

- Ninguno. Las fórmulas de Mifflin-St Jeor, factores de actividad y metas de macronutrientes se mantienen intactas.
- El resumen diario consolidado (`GetDailyMealSummaryUseCase`) reflejará de forma precisa las comidas en la fecha correspondiente según el filtro por timestamps.
