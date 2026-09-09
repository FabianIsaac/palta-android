# Documento de Diseño Técnico: Estadísticas y Calendario Mensual

## 1. Arquitectura y Flujo Unidireccional de Datos (UDF)

El diseño mantiene Clean Architecture estricta con Unidirectional Data Flow (UDF), garantizando que la lógica de cálculo y agregación resida exclusivamente en la capa de `domain` pura (sin referencias al framework `android.*`), mientras que la capa `presentation` reacciona a estados inmutables `UiState`.

```
+-----------------------------------------------------------------------------------+
|                                FLUJO UDF COMPLETO                                 |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ User Action ]                                                                  |
|        |                                                                          |
|        v                                                                          |
|  [ StatisticsEvent ]                                                              |
|   • OnSelectRange(Range)                                                          |
|   • OnOpenCalendar                                                                |
|   • OnDismissCalendar                                                             |
|   • OnCalendarDateClicked(Date)                                                   |
|   • OnNavigateToDateInDiary(Date)                                                 |
|        |                                                                          |
|        v                                                                          |
|  [ StatisticsViewModel ] <-----------------------+                                |
|        |                                         |                                |
|        | invoca                                  | emite Flow                     |
|        v                                         |                                |
|  [ Domain Use Cases ]                            |                                |
|   • GetPeriodStatisticsUseCase ------------------+                                |
|   • GetMonthlyCalendarHabitsUseCase -------------+                                |
|        |                                                                          |
|        v                                                                          |
|  [ Repositories ]                                                                 |
|   • MealRepository (Room MealDao)                                                 |
|   • SupplementRepository (Room SupplementLogDao)                                  |
|   • UserPreferencesRepository (DataStore)                                         |
|        |                                                                          |
|        v                                                                          |
|  [ StatisticsUiState ] (Inmutable)                                                |
|        |                                                                          |
|        v                                                                          |
|  [ Jetpack Compose UI ]                                                           |
|   • StatisticsScreen                                                              |
|   • MonthlyHabitsCalendarDialog                                                   |
|   • CalorieEvolutionChart (Canvas nativo)                                         |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

---

## 2. Modelos de Dominio (`domain/model`)

### 2.1. `StatisticsRange`
Enumera los rangos temporales seleccionables por el usuario para el análisis de tendencias:
```kotlin
enum class StatisticsRange(val days: Int, val label: String) {
    LAST_7_DAYS(7, "7 días"),
    LAST_14_DAYS(14, "14 días"),
    LAST_30_DAYS(30, "30 días")
}
```

### 2.2. `DayCalorieMetric`
Representa las métricas de consumo de un día específico para renderizar las barras de la gráfica:
```kotlin
data class DayCalorieMetric(
    val date: LocalDate,
    val calories: Double,
    val targetCalories: Double,
    val isToday: Boolean = false
)
```

### 2.3. `PeriodStatistics`
Consolidado completo para el periodo seleccionado:
```kotlin
data class PeriodStatistics(
    val range: StatisticsRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val averageDailyCalories: Double,
    val targetDailyCalories: Double,
    val averageProteinGrams: Double,
    val averageCarbsGrams: Double,
    val averageFatGrams: Double,
    val habitCompletedDaysCount: Int,
    val totalPeriodDays: Int,
    val currentStreakDays: Int,
    val supplementAdherencePercentage: Double,
    val dailyCalorieMetrics: List<DayCalorieMetric>
)
```

### 2.4. `DayHabitSummary`
Modela el estado de cada celda del calendario mensual:
```kotlin
data class DayHabitSummary(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasBreakfast: Boolean,
    val hasLunch: Boolean,
    val hasDinner: Boolean,
    val totalCalories: Double,
    val targetCalories: Double,
    val totalProteinGrams: Double,
    val totalCarbsGrams: Double,
    val totalFatGrams: Double,
    val supplementsTakenCount: Int,
    val totalSupplementsCount: Int
) {
    val isHabitCompleted: Boolean
        get() = hasBreakfast && hasLunch && hasDinner

    val hasSupplementsTaken: Boolean
        get() = supplementsTakenCount > 0
}
```

---

## 3. Casos de Uso (`domain/usecase`)

### 3.1. `GetPeriodStatisticsUseCase`
- **Firma:** `operator fun invoke(range: StatisticsRange, endDate: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Flow<PeriodStatistics>`
- **Responsabilidad:**
  1. Calcula la fecha de inicio según `endDate.minusDays((range.days - 1).toLong())`.
  2. Obtiene las comidas del periodo desde `MealRepository.getMealsForDay(startEpoch, endEpoch)`.
  3. Obtiene el presupuesto diario objetivo de `UserPreferencesRepository`.
  4. Obtiene los registros de suplementos del periodo de `SupplementRepository`.
  5. Agrupa comidas por fecha (`LocalDate`), calcula calorías totales y macros diarios.
  6. Determina el cumplimiento del hábito por día (presencia de Desayuno, Almuerzo y Once / Cena).
  7. Calcula los promedios aritméticos del periodo y la adherencia de suplementos.
  8. Emite de forma reactiva un objeto inmutable `PeriodStatistics`.

### 3.2. `GetMonthlyCalendarHabitsUseCase`
- **Firma:** `operator fun invoke(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<DayHabitSummary>>`
- **Responsabilidad:**
  1. Determina el rango de fechas a cubrir, incluyendo los días de relleno de la primera y última semana para completar la cuadrícula de Lunes a Domingo.
  2. Consulta comidas y registros de suplementos para dicho intervalo.
  3. Construye un `DayHabitSummary` para cada día, calculando el hábito cumplido y el consumo nutricional.

---

## 4. Capa de Presentación (`presentation`)

### 4.1. Contrato de Estadísticas (`StatisticsContract.kt`)
```kotlin
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val selectedRange: StatisticsRange = StatisticsRange.LAST_7_DAYS,
    val statistics: PeriodStatistics? = null,
    val isCalendarOpen: Boolean = false,
    val calendarYearMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<DayHabitSummary> = emptyList(),
    val selectedCalendarDate: LocalDate = LocalDate.now(),
    val selectedDayDetail: DayHabitSummary? = null,
    val errorMessage: String? = null
)

sealed interface StatisticsEvent {
    data class OnRangeSelected(val range: StatisticsRange) : StatisticsEvent
    data object OnOpenCalendarClicked : StatisticsEvent
    data object OnCloseCalendarClicked : StatisticsEvent
    data object OnPreviousMonthClicked : StatisticsEvent
    data object OnNextMonthClicked : StatisticsEvent
    data class OnCalendarDateSelected(val date: LocalDate) : StatisticsEvent
    data class OnNavigateToDiaryDate(val date: LocalDate) : StatisticsEvent
}
```

### 4.2. Componentes de UI en Compose
1. **`StatisticsScreen`:**
   - `TopAppBar` con título *"Estadísticas"* y botón de acción con icono de calendario (`Icons.Default.CalendarMonth`).
   - `SingleChoiceSegmentedButtonRow` o grupo de `FilterChip` para alternar entre *7 días*, *14 días* y *30 días*.
   - Tarjeta de Resumen con promedios diarios y racha.
   - Componente `CalorieEvolutionBarChart`:
     - Implementado con Compose `Canvas` para máxima fluidez a 60/120 fps.
     - Dibuja barras verticales redondeadas para cada día.
     - Dibuja una línea horizontal discontinua (*dashed path*) en la altura correspondiente al objetivo calórico diario.
     - Etiquetas de fecha abreviadas en la parte inferior (ej. "Lun 7", "Mar 8").
   - Tarjetas informativas de promedio de macronutrientes (con los colores del tema `MacroProtein`, `MacroCarbs`, `MacroFat`).
   - Barra de consistencia de suplementos.

2. **`MonthlyHabitsCalendarDialog`:**
   - Cuadrícula de 7 columnas (L, M, M, J, V, S, D).
   - Encabezado con selector de mes (`< Septiembre 2026 >`).
   - Celdas con número de día, círculo indicador de selección, icono de fueguito 🔥 para hábito completado y punto para suplementos.
   - Panel inferior con el detalle del día seleccionado (calorías, macros, comidas completadas) y botón *"Ir al Diario de este día"*.

3. **Integración en `DailySummaryScreen`:**
   - Se añade el icono `CalendarMonth` en el `TopAppBar` de `DailySummaryScreen` para permitir abrir el mismo selector de calendario mensual y saltar de forma inmediata a cualquier día.

4. **Integración en `MainActivity.kt`:**
   - Instanciación de `StatisticsViewModel`.
   - Navegación bidireccional: cuando desde el calendario se solicita ir a una fecha, se envía `DailySummaryEvent.OnDateSelected(date)` a `DailySummaryViewModel` y se conmuta la pantalla activa a `AppDestination.SUMMARY`.
