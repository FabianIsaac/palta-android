# Propuesta: fix-calendar-date-selection

## 1. Contexto y Justificación

Actualmente, cuando el usuario abre el diálogo del calendario mensual de hábitos (`MonthlyHabitsCalendarDialog`) desde la pantalla de Resumen Diario (`DailySummaryScreen`), al tocar un día diferente en la cuadrícula del calendario no se actualiza la selección visual ni el detalle del día seleccionado (calorías, macros y hábitos). Además, el diálogo parece no responder o quedar congelado en el día original.

### Causa Raíz
1. **Desconexión de estado en `MainActivity.kt`:** 
   En `MainActivity.kt` (línea 532), al invocar `MonthlyHabitsCalendarDialog` cuando `currentScreen == AppDestination.SUMMARY`, se pasa:
   ```kotlin
   selectedDate = summaryUiState.selectedDate
   ```
   Cuando el usuario presiona cualquier día en la cuadrícula, `onDateSelected` emite `StatisticsEvent.OnCalendarDateSelected(date)`, el cual actualiza `statisticsUiState.selectedCalendarDate`. Sin embargo, `MainActivity` sigue proveyendo `summaryUiState.selectedDate`, la cual nunca cambia con ese evento. Por ende, el diálogo se recompone manteniendo la fecha antigua.
2. **Pérdida de la fecha seleccionada al navegar al diario:**
   En `MonthlyHabitsCalendarDialog`, la tarjeta de detalle `SelectedDayDetailCard` ejecuta:
   ```kotlin
   onNavigateToDiary = {
       onNavigateToDiaryDate(selectedDate)
       onDismiss()
   }
   ```
   Al estar `selectedDate` congelado en el valor de `summaryUiState.selectedDate`, pulsar *"Ir al Diario de este día"* vuelve a cargar la misma fecha donde ya estaba el usuario.
3. **Falta de sincronización inicial al abrir el calendario:**
   Al abrir el calendario desde el Resumen Diario (`onOpenCalendar`), no se inicializa el mes (`calendarYearMonth`) ni la fecha seleccionada (`selectedCalendarDate`) en `StatisticsViewModel` con la fecha que el usuario estaba visualizando (`summaryUiState.selectedDate`). Si el usuario estaba revisando un día de un mes previo o futuro, el calendario se abre desfasado.

---

## 2. Alcance (Scope)

### Dentro del Alcance (In-Scope)
- **Conexión reactiva en `MainActivity.kt`:**
  - Vincular `selectedDate = statisticsUiState.selectedCalendarDate` en el diálogo del calendario cuando se muestra sobre `AppDestination.SUMMARY`.
- **Sincronización al abrir el calendario:**
  - Permitir que el evento de apertura del calendario (o una función dedicada en `StatisticsViewModel`) reciba la fecha activa del diario (`summaryUiState.selectedDate`) para inicializar tanto `selectedCalendarDate` como `calendarYearMonth` con dicha fecha.
- **Interacción fluida en `MonthlyHabitsCalendarDialog`:**
  - Al pulsar cualquier día del mes visible, se resalta inmediatamente la celda seleccionada y se actualiza la tarjeta inferior con los datos nutricionales y de hábitos de ese día.
  - Al presionar *"Ir al Diario de este día"*, se envía la fecha efectivamente seleccionada (`statisticsUiState.selectedCalendarDate`) a `dailySummaryViewModel.onEvent(DailySummaryEvent.OnDateSelected(date))` y se cierra el diálogo, posicionando al usuario en ese día.
- **Idioma y localización:**
  - Mantener todos los textos y descripciones en Español Chileno (`es-CL`) estricto sin voseo.

### Fuera del Alcance (Out-of-Scope)
- Modificación en el cálculo de rachas o hábitos en la capa de dominio.
- Cambios en el diseño visual o estilos de las celdas del calendario.
- Alteraciones en la base de datos Room o persistencia de comidas.

---

## 3. Componentes de UI y Estados Afectados

- **`MainActivity.kt`:**
  - Corrección de los parámetros pasados a `MonthlyHabitsCalendarDialog` en la capa de diálogo de resumen.
  - Pasar la fecha actual de `summaryUiState.selectedDate` al abrir el calendario desde la cabecera o botón del diario.
- **`StatisticsContract.kt` y `StatisticsViewModel.kt`:**
  - Adaptar `StatisticsEvent.OnOpenCalendarClicked` (o crear `OnOpenCalendarWithDate(date: LocalDate)`) para inicializar `selectedCalendarDate` y `calendarYearMonth` con la fecha provista.
- **`DailySummaryScreen.kt`:**
  - Asegurar que el callback `onOpenCalendar` transmita la fecha activa o invoque la sincronización adecuada.
