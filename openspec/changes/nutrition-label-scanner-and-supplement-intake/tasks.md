# Tareas de Implementación: Escaneo de Tablas Nutricionales y Mejoras en Suplementos

## Fase 1: Dominio y Contratos
- [x] 1.1 Crear modelo de dominio `NutritionLabelScanResult` en `com.calculadoracalorias.app.domain.model`.
- [x] 1.2 Definir interfaz `NutritionLabelAnalyzer` en `com.calculadoracalorias.app.domain.repository`.
- [x] 1.3 Implementar caso de uso `AnalyzeNutritionLabelUseCase` en `com.calculadoracalorias.app.domain.usecase` con validación de bytes de imagen.
- [x] 1.4 Escribir pruebas unitarias para `AnalyzeNutritionLabelUseCaseTest`.
- [x] 1.5 Actualizar `SupplementRepository` con método reactivo `getSupplementIntakeCounts(): Flow<Map<String, Int>>`.

## Fase 2: Capa de Datos y Persistencia
- [x] 2.1 Implementar `OpenAiCompatibleNutritionLabelAnalyzer` en `com.calculadoracalorias.app.data.vision` con prompt nutricional especializado (prioridad en porción de consumo, filtrado de vitaminas/minerales, extracción de calorías y macronutrientes).
- [x] 2.2 Agregar consulta de conteo de tomas históricas por suplemento en `SupplementLogDao` y actualizar `LocalSupplementRepository`.
- [x] 2.3 Escribir pruebas unitarias para el analizador y repositorio.

## Fase 3: Capa de Presentación en Suplementos
- [x] 3.1 Actualizar `SupplementsContract.kt` con estados `isScanningLabel`, `areSuggestionsExpanded` e `intakeCounts: Map<String, Int>`, y nuevos eventos `OnToggleSuggestionsExpanded`, `OnScanImageSelected(ByteArray)`.
- [x] 3.2 Actualizar `SupplementsViewModel` para inyectar `AnalyzeNutritionLabelUseCase`, observar el mapa de tomas históricas y manejar el escaneo de imágenes.
- [x] 3.3 Escribir pruebas unitarias en `SupplementsViewModelTest` validando el escaneo de tablas, el colapso de sugerencias y los contadores de tomas.
- [x] 3.4 Actualizar `SupplementsScreen.kt`:
  - Modificar tarjeta de sugerencias para que aparezca colapsada por defecto con botón de expandir/contraer y animación.
  - Añadir botón de "Escanear tabla" con diálogo para elegir entre tomar foto o abrir galería.
  - Mostrar en cada tarjeta de suplemento de la rutina el distintivo con las tomas registradas (ej: *"🏆 14 tomas registradas"*).

## Fase 4: Capa de Presentación en Ingredientes Personalizados
- [x] 4.1 Incorporar en `AddIngredientSheet.kt` (pestaña Personalizado) la opción de escanear tabla nutricional con cámara o galería.
- [x] 4.2 Completar automáticamente los campos de porción en gramos, calorías y macronutrientes al completar el análisis visual.

## Fase 5: Verificación y Entrega
- [x] 5.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 5.2 Verificar compilación de la aplicación (`./gradlew assembleDebug`).
- [x] 5.3 Enviar APK actualizado por Telegram ejecutando `bash ./scripts/send_apk_telegram.sh` según la regla del proyecto.
