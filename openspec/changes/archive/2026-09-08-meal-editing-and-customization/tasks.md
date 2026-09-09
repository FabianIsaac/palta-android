# Tareas de Implementación: meal-editing-and-customization

## Fase 1: Contratos, ViewModels y Pruebas Unitarias
- [x] 1.1 Agregar eventos `OnItemNameChanged` y `OnItemAdded` a `EditMealContract.kt`.
- [x] 1.2 Agregar evento `OnItemNameChanged` a `FoodScanReviewContract.kt`.
- [x] 1.3 Inyectar o proveer `FoodCatalogRepository` en `EditMealViewModel` y manejar búsqueda rápida de alimentos.
- [x] 1.4 Implementar manejo de `OnItemNameChanged` y `OnItemAdded` en `EditMealViewModel` recalculando `NutritionSummary`.
- [x] 1.5 Implementar manejo de `OnItemNameChanged` en `FoodScanReviewViewModel`.
- [x] 1.6 Escribir pruebas unitarias en `EditMealViewModelTest` verificando renombrado, adición de ingredientes y recálculo de macronutrientes.

## Fase 2: Componentes de UI
- [x] 2.1 Crear el componente `EditItemNameDialog` en Compose con validación de nombre no vacío.
- [x] 2.2 Crear el modal `AddIngredientSheet` con buscador reactivo sobre `FoodCatalogRepository`, sugerencias populares chilenas y selector de porción en gramos.
- [x] 2.3 Integrar `EditItemNameDialog` en `EditableFoodItemRow` (`EditMealScreen.kt`) para permitir editar el nombre con un toque o icono.
- [x] 2.4 Integrar `EditItemNameDialog` en `ScannedFoodItemCard` (`FoodScanReviewScreen.kt`) para editar nombres antes de guardar.
- [x] 2.5 Añadir botón `+ Agregar ingrediente` e integrar `AddIngredientSheet` en `EditMealScreen.kt`.

## Fase 3: Verificación y Compilación
- [x] 3.1 Ejecutar suite de pruebas unitarias `./gradlew testDebugUnitTest`.
- [x] 3.2 Compilar el proyecto en modo depuración y verificar que no existan errores de compilación ni lints.
- [x] 3.3 Compilar y enviar el APK generado a Telegram con `./scripts/send_apk_telegram.sh`.
