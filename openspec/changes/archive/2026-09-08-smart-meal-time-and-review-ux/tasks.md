# Tareas de Implementación: smart-meal-time-and-review-ux

## Fase 1: Dominio y Pruebas Unitarias
- [x] 1.1 Crear el modelo de dominio `MealTimeWindows` en `com.calculadoracalorias.app.domain.model` con valores por defecto chilenos y método `detectCategory(LocalTime)`.
- [x] 1.2 Implementar pruebas unitarias completas para `MealTimeWindows` cubriendo mañanas, tardes, noches y límites entre horarios.

## Fase 2: Persistencia en DataStore
- [x] 2.1 Agregar claves de persistencia para minutos de inicio/fin en `UserPreferencesRepository`.
- [x] 2.2 Exponer `mealTimeWindows: MealTimeWindows` en `UserPreferences` y método `updateMealTimeWindows(windows: MealTimeWindows)`.
- [x] 2.3 Verificar la reactividad del `userPreferencesFlow` con los horarios configurados.

## Fase 3: ViewModels y Lógica de Selección
- [x] 3.1 Actualizar `FoodScanReviewViewModel` para activar `isLoading = true` durante el análisis de texto en lenguaje natural.
- [x] 3.2 Modificar `MainActivity` para inicializar el tipo de comida sugerido usando la hora actual (`LocalTime.now()`) y las ventanas configuradas en lugar del default fijo `ALMUERZO`.

## Fase 4: Componentes de UI y Rediseño de Pantallas
- [x] 4.1 Retirar el botón de Ajustes de la barra superior en `FoodCameraScreen.kt`.
- [x] 4.2 Crear el componente seleccionable compacto `MealCategorySelector` con iconos chilenos y diseño responsivo.
- [x] 4.3 Integrar `MealCategorySelector` en `FoodScanReviewScreen.kt` reemplazando los chips dispersos.
- [x] 4.4 Rediseñar la sección de acciones de *Alimentos detectados* en `FoodScanReviewScreen.kt` para garantizar que `+ Agregar alimento` no se deforme ni comprima en ninguna pantalla.
- [x] 4.5 Actualizar el estado de carga en `FoodScanReviewScreen.kt` para mostrar mensajes y animaciones claras al procesar texto.
- [x] 4.6 Incorporar la sección interactiva de *Horarios de Comidas* en `SettingsScreen.kt` con diálogos de selección de hora (TimePicker Material 3).

## Fase 5: Verificación y Compilación
- [x] 5.1 Ejecutar suite de pruebas unitarias `./gradlew testDebugUnitTest`.
- [x] 5.2 Compilar el proyecto en modo depuración y verificar que no existan advertencias ni errores de Compose.
- [x] 5.3 Enviar APK generado a Telegram con el script `./scripts/send_apk_telegram.sh`.
