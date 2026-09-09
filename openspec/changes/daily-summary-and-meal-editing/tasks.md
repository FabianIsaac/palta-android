# Tareas de Implementación: Resumen Diario y Edición Sincronizada de Comidas

## Fase 1: Capa de Dominio (Modelos, Contratos y Casos de Uso)
- [x] 1.1 Definir modelos de dominio `MealEntry`, `DailyMacroBudget` y `DailySummary` en `com.calculadoracalorias.app.domain.model`.
- [x] 1.2 Ampliar el contrato `MealRepository` con métodos para consultar por día (`getMealsForDay`), consultar por ID (`getMealById`), actualizar (`updateMeal`) y eliminar (`deleteMeal`).
- [x] 1.3 Ampliar el contrato `HealthConnectRepository` con `updateNutritionRecord` y `deleteNutritionRecord`.
- [x] 1.4 Implementar `GetDailyMealSummaryUseCase` que consolida presupuesto calórico y comidas registradas por fecha.
- [x] 1.5 Implementar `UpdateMealWithHealthSyncUseCase` con recálculo nutricional y sincronización a Health Connect.
- [x] 1.6 Implementar `DeleteMealWithHealthSyncUseCase` con eliminación local y sincronización de borrado en Health Connect.
- [x] 1.7 Escribir pruebas unitarias exhaustivas con JUnit 5 y MockK para:
  - `GetDailyMealSummaryUseCaseTest`
  - `UpdateMealWithHealthSyncUseCaseTest`
  - `DeleteMealWithHealthSyncUseCaseTest`

## Fase 2: Capa de Datos y Persistencia
- [x] 2.1 Crear clase de relación Room `MealWithItems` en `com.calculadoracalorias.app.data.local.relation` o `entity`.
- [x] 2.2 Actualizar `MealDao` con consultas `getMealsWithItemsBetween`, `getMealWithItemsById` y método transaccional `updateMealWithItems`.
- [x] 2.3 Implementar los nuevos métodos en `RoomMealRepository` garantizando mapeos deterministas entre entidades Room y modelos de dominio.
- [x] 2.4 Actualizar `AndroidHealthConnectRepository` para soportar `updateNutritionRecord` y `deleteNutritionRecord` con manejo seguro de excepciones.
- [x] 2.5 Actualizar y agregar pruebas unitarias para `RoomMealRepository` y `AndroidHealthConnectRepositoryTest`.

## Fase 3: Capa de Presentación (ViewModels, UI Compose y Strings)
- [x] 3.1 Agregar recursos de texto en español chileno (`strings.xml`) sin voseo para la pantalla de resumen, edición, confirmaciones y mensajes.
- [x] 3.2 Implementar `DailySummaryContract` y `DailySummaryViewModel` gestionando fecha activa y flujo de datos de resumen.
- [x] 3.3 Diseñar componentes visuales Compose para `DailySummaryScreen`:
  - Anillo de progreso calórico (consumidas vs restantes).
  - Barras horizontales para proteínas, carbohidratos y grasas con indicadores de porcentaje.
  - Tarjetas de categorías chilenas (*Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*) con lista de alimentos e indicador de calorías.
  - Selector de fecha con controles de día previo / siguiente.
- [x] 3.4 Implementar `EditMealContract` y `EditMealViewModel` para carga de comida, ajuste reactivo de gramos, eliminación de ítems y guardado.
- [x] 3.5 Diseñar composable `EditMealScreen` con edición numérica directa de porciones, recálculo en tiempo real y diálogo de confirmación de borrado.
- [x] 3.6 Escribir pruebas unitarias con Turbine para `DailySummaryViewModelTest` y `EditMealViewModelTest`.
- [x] 3.7 Agregar `@Preview` para Compose en `DailySummaryScreen` y `EditMealScreen` con datos de prueba locales.

## Fase 4: Integración, Navegación y Verificación
- [x] 4.1 Actualizar navegación en `MainActivity.kt` para establecer `DailySummaryScreen` como pantalla principal de entrada, permitiendo navegar a `FoodCameraScreen` y `EditMealScreen`.
- [x] 4.2 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 4.3 Compilar y empaquetar APK (`./gradlew assembleDebug`).
- [x] 4.4 Verificar cumplimiento estricto de localización `es-CL` sin voseo en todos los componentes.
