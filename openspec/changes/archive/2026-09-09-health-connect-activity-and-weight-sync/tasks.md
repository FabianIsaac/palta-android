# Tareas de Implementación: health-connect-activity-and-weight-sync

## Fase 1: Manifiesto Android, Dominio y Contratos
- [x] 1.1 Declarar permisos en `AndroidManifest.xml`: `READ_ACTIVE_CALORIES_BURNED`, `READ_STEPS` y `READ_WEIGHT`.
- [x] 1.2 Crear modelos de dominio `DailyHealthActivity.kt` y `HealthWeightRecord.kt` en `domain/model/`.
- [x] 1.3 Extender interfaz `HealthConnectRepository` en `domain/repository/` con consultas de permisos y métodos de lectura (`getDailyActivity`, `getLatestWeight`).
- [x] 1.4 Crear casos de uso `GetDailyHealthActivityUseCase` y `GetLatestHealthWeightUseCase` en `domain/usecase/`.
- [x] 1.5 Crear pruebas unitarias deterministas para los nuevos casos de uso.

## Fase 2: Capa de Datos y Preferencias
- [x] 2.1 Actualizar `UserPreferencesRepository.kt` con las nuevas claves de configuración en DataStore (`healthConnectActivitySyncEnabled`, `includeBurnedCaloriesInBudget`, `healthConnectWeightSyncEnabled`).
- [x] 2.2 Implementar en `AndroidHealthConnectRepository.kt` la lectura de agregados (`AggregateRequest` para calorías activas y pasos) y lectura de peso (`ReadRecordsRequest` para `WeightRecord`).
- [x] 2.3 Proveer pruebas unitarias para `AndroidHealthConnectRepositoryTest` simulando respuestas exitosas y errores controlados.

## Fase 3: Capa de Presentación (UI y ViewModels)
- [x] 3.1 Actualizar `DailySummaryContract.kt` con las nuevas propiedades en `DailySummaryUiState` (`burnedCalories`, `stepsCount`, `includeBurnedInBudget`).
- [x] 3.2 Actualizar `DailySummaryViewModel.kt` para cargar de forma reactiva la actividad física al cambiar de fecha y computar el balance calórico neto.
- [x] 3.3 Diseñar el componente Compose `DailyActivityCard.kt` en `presentation/summary/` mostrando calorías quemadas (🔥) y pasos (👣) con diseño Material You.
- [x] 3.4 Actualizar `SettingsContract.kt`, `SettingsViewModel.kt` y `SettingsScreen.kt` para incorporar los interruptores de actividad física, ajuste de presupuesto y tarjeta de sincronización de balanza inteligente.
- [x] 3.5 Crear Compose Previews para temas claro y oscuro de `DailyActivityCard` y de las nuevas tarjetas en `SettingsScreen`.

## Fase 4: Verificación y Entrega
- [x] 4.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 4.2 Compilar el proyecto Android en modo Debug (`./gradlew assembleDebug`).
- [x] 4.3 Validar que el 100% de los textos estén en Español Chileno (`es-CL`) sin voseo.
- [x] 4.4 Compilar y enviar el APK actualizado vía Telegram mediante `bash ./scripts/send_apk_telegram.sh`.
