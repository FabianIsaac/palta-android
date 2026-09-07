# Tareas de Implementación: ai-food-logging-and-health-connect

## Fase 1: Capa de Dominio (Domain)

- [x] 1.1. Definir modelos inmutables de dominio: `ScannedFoodItem`, `DetectedMealResult`, `MealCategory` y `VisionSource`.
- [x] 1.2. Crear interfaz de repositorio `HealthConnectRepository` con métodos para verificar disponibilidad, permisos y escritura de `NutritionRecord`.
- [x] 1.3. Crear interfaz `FoodVisionAnalyzer` para abstracción del motor de análisis visual.
- [x] 1.4. Implementar caso de uso `RecalculatePortionUseCase` para el cálculo proporcional determinista de calorías y macronutrientes.
- [x] 1.5. Implementar caso de uso `AnalyzeFoodImageUseCase` que coordine la llamada al analizador configurado.
- [x] 1.6. Implementar caso de uso `SaveMealWithHealthSyncUseCase` para persistencia en Room y sincronización con Health Connect.
- [x] 1.7. Escribir pruebas unitarias exhaustivas con JUnit 5 y MockK para `RecalculatePortionUseCase` y `SaveMealWithHealthSyncUseCase`.

---

## Fase 2: Capa de Datos (Data) - Visión e IA

- [x] 2.1. Configurar cliente HTTP (Ktor o Retrofit) y crear DTOs de serialización JSON para la API de MiniMax Vision.
- [x] 2.2. Implementar `MiniMaxVisionAnalyzer` con manejo de errores de red, timeouts y validación de respuesta estructurada.
- [x] 2.3. Configurar dependencias de LiteRT / MediaPipe Tasks y agregar modelo de clasificación local de alimentos.
- [x] 2.4. Implementar `LocalLiteRtVisionAnalyzer` con mapeo contra el catálogo de alimentos de Room Database.
- [x] 2.5. Crear `FoodVisionAnalyzerFactory` para seleccionar dinámicamente el analizador según la preferencia y conectividad del usuario.
- [x] 2.6. Escribir pruebas unitarias para serialización, deserialización y mapeo de `MiniMaxVisionAnalyzer`.

---

## Fase 3: Capa de Datos (Data) - Health Connect y Persistencia

- [x] 3.1. Añadir dependencias de `androidx.health.connect:connect-client` y configurar permisos en `AndroidManifest.xml`.
- [x] 3.2. Implementar `AndroidHealthConnectRepository` con verificación de disponibilidad de SDK, solicitud de permisos y guardado de `NutritionRecord`.
- [x] 3.3. Añadir campo `healthConnectRecordId` en `MealEntryEntity` de Room y actualizar DAOs.
- [x] 3.4. Implementar almacenamiento de clave de API MiniMax y preferencia de motor en `UserPreferencesRepository` (DataStore).
- [x] 3.5. Escribir pruebas unitarias con MockK para `AndroidHealthConnectRepository`.

---

## Fase 4: Capa de Presentación (UI & ViewModels)

- [x] 4.1. Definir `FoodScanReviewUiState` y `FoodScanReviewEvent` con arquitectura MVI / UDF.
- [x] 4.2. Implementar `FoodScanReviewViewModel` con `StateFlow` reactivo para recálculo instantáneo al editar porciones.
- [x] 4.3. Implementar captura fotográfica con CameraX en `FoodCameraScreen` con selector de galería y control de linterna.
- [x] 4.4. Crear composable `FoodScanReviewScreen` con lista editable de alimentos, campos de texto numéricos para gramos y barra de resumen nutricional.
- [x] 4.5. Implementar selector de categorías de comida chilena (*Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*).
- [x] 4.6. Crear diálogo explicativo para solicitud de permisos de Health Connect con redacción en español chileno (`es-CL`).
- [x] 4.7. Crear pantalla o sección de configuración en `SettingsScreen` para ingresar y validar la API Key de MiniMax.
- [x] 4.8. Diseñar vistas previas con `@Preview` para `FoodScanReviewScreen` cubriendo estados de carga, lista editable y error.
- [x] 4.9. Escribir pruebas unitarias para `FoodScanReviewViewModel` utilizando Turbine y Coroutines Test Dispatcher.
