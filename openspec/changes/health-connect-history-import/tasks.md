# Tareas de Implementación: health-connect-history-import

## Fase 1: Dominio y Contratos
- [ ] 1.1 Crear modelo de dominio `ExternalNutritionRecord` en `domain/model/ExternalNutritionRecord.kt`.
- [ ] 1.2 Actualizar interfaz `HealthConnectRepository` con `hasReadNutritionPermission()` y `readNutritionRecords(startTime: Long, endTime: Long): Result<List<ExternalNutritionRecord>>`.
- [ ] 1.3 Crear caso de uso `ImportHealthConnectHistoryUseCase` con lógica de deduplicación y mapeo a categorías chilenas.
- [ ] 1.4 Crear pruebas unitarias para `ImportHealthConnectHistoryUseCaseTest`.

## Fase 2: Capa de Datos y Persistencia
- [ ] 2.1 Agregar método en `MealDao` para consultar `getAllHealthConnectRecordIds(): List<String>`.
- [ ] 2.2 Implementar `hasReadNutritionPermission` y `readNutritionRecords` en `AndroidHealthConnectRepository` usando `ReadRecordsRequest`.
- [ ] 2.3 Proveer pruebas unitarias de repositorio y simulación de lecturas de Health Connect.

## Fase 3: Capa de Presentación (Ajustes y UI)
- [ ] 3.1 Actualizar `SettingsContract.kt` con estados de importación (`isImportingHistory`, `selectedImportDaysRange`, `importResultCount`) y eventos correspondientes.
- [ ] 3.2 Implementar en `SettingsViewModel` la invocación del caso de uso y gestión de mensajes al usuario.
- [ ] 3.3 Diseñar e integrar la tarjeta de sincronización con Health Connect en `SettingsScreen.kt`.
- [ ] 3.4 Agregar Compose Previews para la nueva tarjeta en temas claro y oscuro.

## Fase 4: Pruebas, Verificación y Entrega
- [ ] 4.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [ ] 4.2 Compilar el proyecto Android (`./gradlew assembleDebug`).
- [ ] 4.3 Validar que todos los textos estén en Español Chileno (`es-CL`) sin voseo.
- [ ] 4.4 Compilar y enviar APK vía Telegram ejecutando `bash ./scripts/send_apk_telegram.sh`.
