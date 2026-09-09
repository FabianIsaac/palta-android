# Tareas de Implementación: data-persistence-and-cloud-backup

## Fase 1: Migraciones Seguras y Persistencia en Room
- [x] 1.1 Modificar `AppDatabase.kt` para remover `fallbackToDestructiveMigration()` y reemplazarlo por `fallbackToDestructiveMigrationOnDowngrade()`.
- [x] 1.2 Configurar `exportSchema = false` o carpeta de esquemas Room si se requiere `@AutoMigration`.
- [x] 1.3 Incrementar `versionCode` y crear estrategia de versionado para APKs futuros.

## Fase 2: Modelos de Dominio y Serialización de Respaldo
- [x] 2.1 Crear los modelos de dominio serializables para respaldo en `domain/model/backup/`:
  - `BackupDataPayload`
  - `BackupMealEntry` y `BackupMealItem`
  - `BackupSupplement` y `BackupSupplementLog`
  - `BackupPreferences` y `BackupMealTimeWindows`
- [x] 2.2 Definir la interfaz `BackupRepository` en `domain/repository/BackupRepository.kt` con métodos para exportar payload e importar payload atómicamente.
- [x] 2.3 Implementar `ExportBackupUseCase` y `ImportBackupUseCase` en `domain/usecase/backup/`.
- [x] 2.4 Implementar pruebas unitarias para serialización, deserialización y validación de esquemas de respaldo.

## Fase 3: Capa de Datos y Persistencia de Respaldo
- [x] 3.1 Implementar `RoomBackupRepository` en `data/repository/RoomBackupRepository.kt` interactuando con `MealDao`, `SupplementDao`, `SupplementLogDao` y `UserPreferencesRepository`.
- [x] 3.2 Implementar métodos DAO auxiliares en `MealDao`, `SupplementDao` y `SupplementLogDao` para inserción masiva transaccional en restauración.
- [x] 3.3 Agregar persistencia de `last_backup_timestamp` en `UserPreferencesRepository`.
- [x] 3.4 Implementar pruebas unitarias para `RoomBackupRepository` y casos de uso con MockK.

## Fase 4: ViewModels y Flujo SAF (Storage Access Framework)
- [x] 4.1 Actualizar `SettingsViewModel` y `SettingsUiState` para gestionar el estado de respaldo, fecha del último respaldo, estados de carga y previsualización de importación.
- [x] 4.2 Exponer eventos en `SettingsViewModel`: `onExportBackup(Uri)`, `onSelectBackupFile(Uri)`, `onConfirmRestore()`, `onDismissRestoreDialog()`.
- [x] 4.3 Implementar pruebas unitarias para `SettingsViewModel` cubriendo exportación exitosa, error de archivo corrupto y confirmación de restauración.

## Fase 5: UI Compose en Pantalla de Ajustes
- [x] 5.1 Crear componente `BackupAndRestoreSection` en `presentation/settings/components/` con diseño Material Design 3.
- [x] 5.2 Integrar los launchers `rememberLauncherForActivityResult` para `CreateDocument` y `OpenDocument` en `SettingsScreen.kt`.
- [x] 5.3 Implementar diálogo de confirmación `RestoreBackupConfirmationDialog` con resumen previo de registros a importar.
- [x] 5.4 Agregar strings localizados en Español Chileno (`es-CL`) en `strings.xml`.

## Fase 6: Verificación, Compilación y Entrega
- [x] 6.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 6.2 Compilar APK en modo debug (`./gradlew assembleDebug`).
- [x] 6.3 Ejecutar el script obligatorio de envío del APK a Telegram (`./scripts/send_apk_telegram.sh`).
