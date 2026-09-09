# Tareas de Implementación: fix-calendar-date-selection

## Fase 1: Contratos y ViewModel de Estadísticas
- [x] 1.1 Modificar `StatisticsEvent.OnOpenCalendarClicked` en `StatisticsContract.kt` para recibir opcionalmente `initialDate: LocalDate? = null`.
- [x] 1.2 Actualizar `StatisticsViewModel.kt` para sincronizar `selectedCalendarDate` y `calendarYearMonth` con `initialDate` al procesar `OnOpenCalendarClicked`.
- [x] 1.3 Añadir o actualizar pruebas unitarias en `StatisticsViewModelTest` para verificar la inicialización y cambio interactivo de fecha.

## Fase 2: Integración en Pantalla Raíz (MainActivity)
- [x] 2.1 En `MainActivity.kt`, corregir la invocación de `MonthlyHabitsCalendarDialog` bajo `AppDestination.SUMMARY` pasando `selectedDate = statisticsUiState.selectedCalendarDate` en lugar de `summaryUiState.selectedDate`.
- [x] 2.2 En `MainActivity.kt`, enviar `summaryUiState.selectedDate` en el callback `onOpenCalendar` del `DailySummaryScreen`.

## Fase 3: Pruebas y Verificación
- [x] 3.1 Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 3.2 Verificar compilación del proyecto Android (`./gradlew assembleDebug`).
- [x] 3.3 Validar que todos los textos se mantengan en Español Chileno (`es-CL`) sin voseo.
- [x] 3.4 Compilar y enviar APK vía Telegram ejecutando `bash ./scripts/send_apk_telegram.sh`.
