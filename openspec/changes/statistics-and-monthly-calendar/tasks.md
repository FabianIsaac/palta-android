# Tareas de Implementación: Estadísticas y Calendario Mensual

## Fase 1: Capa de Dominio (Modelos, Casos de Uso y Pruebas Unitarias)
- [x] 1.1 Crear enum `StatisticsRange` (`domain/model/StatisticsRange.kt`) con opciones de 7, 14 y 30 días.
- [x] 1.2 Crear modelos de dominio nutricionales: `DayCalorieMetric`, `PeriodStatistics` y `DayHabitSummary` en `domain/model/`.
- [x] 1.3 Implementar caso de uso `GetPeriodStatisticsUseCase` (`domain/usecase/GetPeriodStatisticsUseCase.kt`) para calcular promedios, agregación de comidas y suplementos para el rango dado.
- [x] 1.4 Crear pruebas unitarias para `GetPeriodStatisticsUseCaseTest` verificando cálculos aritméticos, adherencia de suplementos y métricas de racha.
- [x] 1.5 Implementar caso de uso `GetMonthlyCalendarHabitsUseCase` (`domain/usecase/GetMonthlyCalendarHabitsUseCase.kt`) para generar la grilla del mes con días cumplidos e ingesta.
- [x] 1.6 Crear pruebas unitarias para `GetMonthlyCalendarHabitsUseCaseTest` verificando la generación de celdas y lógica de días cumplidos.

## Fase 2: Componente de Calendario Mensual Interactivo
- [x] 2.1 Crear componente `MonthlyHabitsCalendarDialog` (`presentation/calendar/MonthlyHabitsCalendarDialog.kt`) con grilla Lunes-Domingo, selector de mes y badges de hábito (🔥) y suplementos.
- [x] 2.2 Diseñar la tarjeta inferior de detalle del día seleccionado dentro del diálogo con desglose calórico/macros y botón "Ir al Diario de este día".
- [x] 2.3 Crear Compose Previews para `MonthlyHabitsCalendarDialog` en modo claro y modo oscuro con datos de prueba realistas.

## Fase 3: Pantalla de Estadísticas y Gráfica de Calorías
- [x] 3.1 Definir contrato `StatisticsContract` (`presentation/statistics/StatisticsContract.kt`) con `StatisticsUiState` y `StatisticsEvent`.
- [x] 3.2 Implementar `StatisticsViewModel` (`presentation/statistics/StatisticsViewModel.kt`) integrando `GetPeriodStatisticsUseCase` y `GetMonthlyCalendarHabitsUseCase`.
- [x] 3.3 Crear componente gráfico `CalorieEvolutionBarChart` (`presentation/statistics/components/CalorieEvolutionBarChart.kt`) en Canvas con barras de energía y línea de referencia de meta calórica.
- [x] 3.4 Implementar `StatisticsScreen` (`presentation/statistics/StatisticsScreen.kt`) reemplazando el placeholder por selector de rango (7, 14, 30 días), resumen de promedios, gráfico de calorías, macros y consistencia de suplementos.
- [x] 3.5 Añadir Compose Previews para `StatisticsScreen` en modo claro y oscuro con datos simulados.

## Fase 4: Integración en la App y Navegación
- [x] 4.1 Añadir botón de acción con icono de calendario (`Icons.Default.CalendarMonth`) en el `TopAppBar` de `DailySummaryScreen.kt`.
- [x] 4.2 Conectar en `DailySummaryScreen` el diálogo de calendario para permitir saltar a cualquier fecha en el diario.
- [x] 4.3 Conectar `StatisticsViewModel` en `MainActivity.kt` y gestionar el evento de navegación hacia `AppDestination.SUMMARY` al pulsar "Ir al Diario de este día".
- [x] 4.4 Verificar compilación completa y ejecutar toda la suite de pruebas unitarias (`./gradlew testDebugUnitTest`).

## Fase 5: Compilación y Envío de APK
- [x] 5.1 Ejecutar `bash ./scripts/send_apk_telegram.sh` para compilar y enviar el APK actualizado a Telegram según la regla obligatoria del proyecto.
