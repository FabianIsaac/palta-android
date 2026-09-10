## 1. Dominio y Modelos de Diagnóstico

- [x] 1.1 Crear modelo `AiCallLogEntry`, enum `AiCallType` y el gestor en memoria `AiDebugLogManager` en `domain/model/` y `data/remote/` con buffer circular de hasta 50 registros en `StateFlow` y verificación de ofuscación de credenciales.
- [x] 1.2 Escribir pruebas unitarias en `AiDebugLogManagerTest` verificando la retención de llamadas, rotación FIFO al superar el límite y emisión reactiva en el flujo.

## 2. Refuerzo del Analizador Remoto y System Prompt

- [x] 2.1 Instrumentar `OpenAiCompatibleMealAnalyzer` (y analizadores asociados) para registrar cada llamada en `AiDebugLogManager` y emitir trazas en Logcat bajo el tag `AiMealAnalyzer`.
- [x] 2.2 Actualizar el system prompt en `OpenAiCompatibleMealAnalyzer` con la regla de oro para la descomposición atómica de pastas (ave mayo, atún mayo, huevo mayo) y separación obligatoria de salsas/grasas calóricas (mayonesa).
- [x] 2.3 Implementar función de prueba de conectividad (*Ping*) con el proveedor activo en `OpenAiCompatibleMealAnalyzer` y propagar excepciones estructuradas ante fallos.
- [x] 2.4 Escribir pruebas unitarias en `OpenAiCompatibleMealAnalyzerTest` comprobando el registro de diagnósticos en fallos y el parseo de respuestas con mayonesa y aderezos independientes.

## 3. Integración en ViewModel y UI de Revisión

- [x] 3.1 Actualizar `FoodScanReviewContract` y `FoodScanReviewViewModel` para retener y exponer el detalle técnico del último fallo de IA (`lastTechnicalError`) en `FoodScanReviewUiState`.
- [x] 3.2 Crear diálogo `AiDiagnosticDetailsDialog` y añadir la acción *"Ver detalle técnico"* en la tarjeta roja de error de `FoodScanReviewScreen` con botón para copiar al portapapeles.
- [x] 3.3 Escribir pruebas unitarias en `FoodScanReviewViewModelTest` asegurando que el detalle técnico del error esté presente al ocurrir un fallback local.

## 4. Diagnóstico y Prueba de Conexión en Ajustes

- [x] 4.1 Actualizar `SettingsContract` y `SettingsViewModel` para soportar la ejecución del test de conectividad en vivo y la observación de los logs de `AiDebugLogManager`.
- [x] 4.2 Diseñar e integrar la tarjeta *"Diagnóstico y Conexión de IA"* en `SettingsScreen`, incorporando el botón *"Probar conexión"* e historial de llamadas recientes desplegable.
- [x] 4.3 Ejecutar `./gradlew testDebugUnitTest` para comprobar que todas las pruebas unitarias pasen exitosamente sin regresiones.
