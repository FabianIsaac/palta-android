# Tareas de Implementación: offline-resilient-meal-decomposition

## Fase 1: Capa de Dominio (Modelos y Casos de Uso)

- [x] 1.1 Actualizar el modelo de dominio `MealEntry` con `rawDescription: String?` e `isPendingAiRefinement: Boolean`.
- [x] 1.2 Añadir método en `MealRepository` para consultar comidas pendientes de refinamiento (`getPendingRefinementMeals()`).
- [x] 1.3 Crear el caso de uso `RefinePendingMealsUseCase` que tome comidas pendientes y las procese mediante `NaturalLanguageMealAnalyzer`.
- [x] 1.4 Escribir pruebas unitarias exhaustivas para `RefinePendingMealsUseCaseTest` (éxito de reprocesamiento, fallos parciales, persistencia).

---

## Fase 2: Capa de Datos (IA, Catálogo Local y Persistencia)

- [x] 2.1 Actualizar el system prompt y lógica en `RemoteNaturalLanguageMealAnalyzer` para exigir la descomposición atómica de ingredientes en preparaciones compuestas y el cálculo proporcional de porciones.
- [x] 2.2 Enriquecer `LocalFoodCatalogRepository` con alimentos básicos chilenos: tortillas de fajita, carne molida, choclo desgranado, lechuga picada y yogurt griego.
- [x] 2.3 Actualizar `MealEntryEntity`, DAO y Room Database con las nuevas columnas `rawDescription` e `isPendingAiRefinement`.
- [x] 2.4 Actualizar `RoomMealRepository` implementando la lectura y actualización de comidas pendientes de refinamiento.
- [x] 2.5 Escribir pruebas unitarias en `RemoteNaturalLanguageMealAnalyzerTest` validando el desglose de preparaciones complejas (ej. fajitas con múltiples ingredientes) y el fallback local enriquecido.

---

## Fase 3: Capa de Presentación (UI y Estados Compose)

- [x] 3.1 Modificar `QuickNaturalLanguageEntrySheet` para recibir `initialText: String` y evitar borrar el texto ante cancelaciones o reintentos.
- [x] 3.2 Actualizar `FoodScanReviewUiState` y `FoodScanReviewViewModel` para almacenar `lastRawDescription` y exponer acciones de reintento directo y guardado con estimado local.
- [x] 3.3 Diseñar la tarjeta visual de estado de error en `FoodScanReviewScreen` con el texto intacto, botón [Reintentar] y botón [Editar].
- [x] 3.4 Actualizar `DailySummaryScreen` para mostrar un distintivo ("Estimado local - Pendiente de IA") en las comidas marcadas con `isPendingAiRefinement`.
- [x] 3.5 Implementar observador de conectividad o gatillo de sincronización en `MainActivity` / `DailySummaryViewModel` para ejecutar `RefinePendingMealsUseCase` automáticamente cuando haya internet.

---

## Fase 4: Verificación, Pruebas y APK

- [x] 4.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 4.2 Compilar el APK debug (`./gradlew assembleDebug`).
- [x] 4.3 Enviar automáticamente el APK generado a Telegram con `./scripts/send_apk_telegram.sh` según las reglas del proyecto.
