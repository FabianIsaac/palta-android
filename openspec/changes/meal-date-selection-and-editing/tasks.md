# Tareas de Implementación: meal-date-selection-and-editing

## Fase 1: Capa de Dominio y Contratos de Estado

- [x] **Tarea 1.1: Actualizar contratos de estado en FoodScanReview** <!-- id: task-1.1 -->
  - Agregar `targetDate: LocalDate = LocalDate.now()` a `FoodScanReviewUiState`.
  - Agregar evento `FoodScanReviewEvent.OnTargetDateChanged(val date: LocalDate)`.
  - Asegurar soporte de fecha en métodos de inicialización de `FoodScanReviewViewModel`.
  - Dependencias: Ninguna.

- [x] **Tarea 1.2: Actualizar contratos de estado en EditMeal** <!-- id: task-1.2 -->
  - Agregar evento `EditMealEvent.OnDateChanged(val newDate: LocalDate)` a `EditMealContract.kt`.
  - Dependencias: Ninguna.

- [x] **Tarea 1.3: Pruebas unitarias para cálculo de timestamps y cambio de fecha** <!-- id: task-1.3 -->
  - Crear/actualizar tests unitarios en `FoodScanReviewViewModelTest` para verificar que al guardar se use el timestamp derivado de `targetDate`.
  - Crear/actualizar tests unitarios en `EditMealViewModelTest` para comprobar que `OnDateChanged` conserve la hora original y actualice el timestamp adecuadamente.
  - Dependencias: Tareas 1.1, 1.2.

---

## Fase 2: ViewModels y Lógica de Negocio

- [x] **Tarea 2.1: Implementar manejo de `targetDate` en `FoodScanReviewViewModel`** <!-- id: task-2.1 -->
  - Manejar `OnTargetDateChanged` actualizando `_uiState`.
  - En `handleConfirmAndSave()`, calcular el timestamp con `targetDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()`.
  - Ajustar `checkRecentMealConsolidation` para verificar sobre la fecha objetivo en vez de estrictamente el reloj actual.
  - Dependencias: Tarea 1.1.

- [x] **Tarea 2.2: Implementar cambio de fecha en `EditMealViewModel`** <!-- id: task-2.2 -->
  - Manejar evento `EditMealEvent.OnDateChanged` calculando el nuevo timestamp manteniendo la hora previa.
  - Al ejecutar `OnSaveMealClicked`, pasar el nuevo timestamp a `updateMealWithHealthSyncUseCase`.
  - Dependencias: Tarea 1.2.

---

## Fase 3: Navegación y Propagación en la Capa de Presentación

- [x] **Tarea 3.1: Propagar fecha activa en `MainActivity.kt`** <!-- id: task-3.1 -->
  - Almacenar `pendingMealDate: LocalDate?` en `AppNavigation`.
  - Al presionar "+ Agregar comida" en `DailySummaryScreen`, capturar la categoría y `summaryUiState.selectedDate`.
  - Al presionar el botón flotante de la cámara en la barra inferior, capturar `summaryUiState.selectedDate`.
  - Al navegar a la cámara y revisión, inicializar `FoodScanReviewViewModel` con la fecha capturada (`targetDate = pendingMealDate ?: LocalDate.now()`).
  - Dependencias: Tarea 2.1.

---

## Fase 4: Componentes de Interfaz de Usuario (Compose)

- [x] **Tarea 4.1: Indicador y selector de fecha en `FoodScanReviewScreen`** <!-- id: task-4.1 -->
  - Añadir chip/tarjeta interactiva con la fecha objetivo formateada en español chileno (ej. "Ayer, 7 de septiembre" / "Hoy, 8 de septiembre").
  - Integrar `DatePickerDialog` de Material 3 para permitir al usuario cambiar la fecha de registro.
  - Añadir strings correspondientes en `strings.xml`.
  - Actualizar Compose Previews.
  - Dependencias: Tarea 2.1.

- [x] **Tarea 4.2: Selector y visualización de fecha en `EditMealScreen`** <!-- id: task-4.2 -->
  - Mostrar la fecha actual de la comida en la pantalla de edición.
  - Añadir botón interactivo "Cambiar fecha" que invoque `DatePickerDialog`.
  - Al confirmar en el diálogo, emitir `EditMealEvent.OnDateChanged`.
  - Actualizar Compose Previews.
  - Dependencias: Tarea 2.2.

---

## Fase 5: Verificación y Compilación

- [x] **Tarea 5.1: Ejecutar suite de pruebas unitarias** <!-- id: task-5.1 -->
  - Ejecutar `./gradlew testDebugUnitTest` para asegurar que todas las pruebas pasen sin regresiones.
  - Dependencias: Tareas 1.3, 2.1, 2.2, 3.1, 4.1, 4.2.

- [x] **Tarea 5.2: Generación y envío de APK por Telegram** <!-- id: task-5.2 -->
  - Ejecutar `bash ./scripts/send_apk_telegram.sh` como establece la regla obligatoria del proyecto.
  - Dependencias: Tarea 5.1.
