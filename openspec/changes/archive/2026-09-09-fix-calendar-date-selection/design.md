# Diseño Técnico: fix-calendar-date-selection

## 1. Arquitectura y Flujo Unidireccional (UDF)

El diseño mantiene estrictamente la separación de responsabilidades y la inmutabilidad de los estados en Jetpack Compose:
1. `StatisticsViewModel` es el único responsable de gestionar el estado del diálogo de calendario (`StatisticsUiState.isCalendarOpen`, `calendarYearMonth`, `calendarDays`, `selectedCalendarDate`).
2. `MainActivity` actúa como orquestador de navegación entre pantallas y diálogos a nivel raíz.
3. Al abrir el diálogo desde el Resumen Diario (`DailySummaryScreen`), se sincroniza el estado inicial del calendario con la fecha activa observada en `DailySummaryUiState.selectedDate`.
4. Los toques en cualquier día del diálogo actualizan reactivamente `selectedCalendarDate` en `StatisticsUiState`, permitiendo que la celda se resalte y que `SelectedDayDetailCard` muestre la información correcta al instante.
5. Al accionar *"Ir al Diario de este día"*, se propaga la fecha hacia `DailySummaryViewModel` mediante `DailySummaryEvent.OnDateSelected(date)` y se cierra el diálogo.

---

## 2. Diagrama de Flujo de Interacción

```
+-----------------------------------------------------------------------------------+
|                           1. Apertura del Calendario                              |
|                                                                                   |
|  [DailySummaryScreen]                                                             |
|         │                                                                         |
|         ▼ onOpenCalendar(summaryUiState.selectedDate)                             |
|  [MainActivity]                                                                   |
|         │                                                                         |
|         ▼ onEvent(StatisticsEvent.OnOpenCalendar(selectedDate))                   |
|  [StatisticsViewModel]                                                            |
|         │  • isCalendarOpen = true                                                |
|         │  • selectedCalendarDate = selectedDate                                  |
|         │  • calendarYearMonth = YearMonth.from(selectedDate)                     |
|         ▼ (carga hábitos del mes vía getMonthlyCalendarHabitsUseCase)             |
+-----------------------------------------------------------------------------------+
                                          │
                                          ▼
+-----------------------------------------------------------------------------------+
|                        2. Selección Interactiva de Días                           |
|                                                                                   |
|  [MonthlyHabitsCalendarDialog]                                                    |
|         │  (selectedDate = statisticsUiState.selectedCalendarDate)                |
|         │                                                                         |
|         ▼ Usuario toca el día 14                                                  |
|  onDateSelected(14) ──> StatisticsEvent.OnCalendarDateSelected(14)                 |
|         │                                                                         |
|         ▼                                                                         |
|  StatisticsViewModel actualiza selectedCalendarDate = 14                          |
|         │                                                                         |
|         ▼ Recomposición Compose                                                   |
|  • Celda 14 seleccionada visualmente con borde y color primario                   |
|  • SelectedDayDetailCard muestra macros y estado del día 14                       |
+-----------------------------------------------------------------------------------+
                                          │
                                          ▼
+-----------------------------------------------------------------------------------+
|                         3. Salto al Diario del Día                                |
|                                                                                   |
|  Usuario toca "Ir al Diario de este día" en SelectedDayDetailCard                 |
|         │                                                                         |
|         ▼ onNavigateToDiaryDate(selectedDate [14])                                |
|  [MainActivity]                                                                   |
|         ├──> dailySummaryViewModel.onEvent(DailySummaryEvent.OnDateSelected(14))   |
|         └──> statisticsViewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked)   |
|                                                                                   |
|  Resultado: Diálogo se cierra y el diario muestra el día 14.                      |
+-----------------------------------------------------------------------------------+
```

---

## 3. Contratos de Eventos y Cambios en Código

### A. `StatisticsContract.kt`
Mejorar el evento `OnOpenCalendarClicked` o ampliarlo para aceptar opcionalmente la fecha inicial:
```kotlin
sealed interface StatisticsEvent {
    // ...
    data class OnOpenCalendarClicked(val initialDate: LocalDate? = null) : StatisticsEvent
    // ...
}
```

### B. `StatisticsViewModel.kt`
Al procesar `OnOpenCalendarClicked(initialDate)`:
```kotlin
is StatisticsEvent.OnOpenCalendarClicked -> {
    val targetDate = event.initialDate ?: _uiState.value.selectedCalendarDate
    val targetYearMonth = YearMonth.from(targetDate)
    
    _calendarYearMonth.value = targetYearMonth
    _uiState.update {
        it.copy(
            isCalendarOpen = true,
            calendarYearMonth = targetYearMonth,
            selectedCalendarDate = targetDate
        )
    }
}
```

### C. `MainActivity.kt`
En la invocación del diálogo cuando `currentScreen == AppDestination.SUMMARY`:
```kotlin
if (statisticsUiState.isCalendarOpen && currentScreen == AppDestination.SUMMARY) {
    MonthlyHabitsCalendarDialog(
        isOpen = true,
        currentYearMonth = statisticsUiState.calendarYearMonth,
        calendarDays = statisticsUiState.calendarDays,
        selectedDate = statisticsUiState.selectedCalendarDate, // <-- Corregido: enlaza al estado de estadísticas
        onDateSelected = { date ->
            statisticsViewModel.onEvent(StatisticsEvent.OnCalendarDateSelected(date))
        },
        onPreviousMonth = { statisticsViewModel.onEvent(StatisticsEvent.OnPreviousMonthClicked) },
        onNextMonth = { statisticsViewModel.onEvent(StatisticsEvent.OnNextMonthClicked) },
        onNavigateToDiaryDate = { date ->
            dailySummaryViewModel.onEvent(DailySummaryEvent.OnDateSelected(date))
            statisticsViewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked)
            currentScreen = AppDestination.SUMMARY
        },
        onDismiss = { statisticsViewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked) }
    )
}
```
Y al pasar el callback `onOpenCalendar` a `DailySummaryScreen`:
```kotlin
onOpenCalendar = {
    statisticsViewModel.onEvent(StatisticsEvent.OnOpenCalendarClicked(summaryUiState.selectedDate))
}
```
