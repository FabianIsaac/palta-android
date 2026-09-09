# Tareas de Implementación: auto-drive-cloud-backup

## Fase 1: Dependencias y Preferencias de Respaldo Automático
- [x] 1.1 Agregar dependencias `androidx.work:work-runtime-ktx` y `androidx.documentfile:documentfile` en `gradle/libs.versions.toml` y `app/build.gradle.kts`.
- [x] 1.2 Extender `UserPreferences` y `UserPreferencesRepository` con campos `autoBackupEnabled: Boolean`, `autoBackupFolderUri: String?`, `autoBackupFolderName: String?` y `lastAutoBackupTimestamp: Long?`.
- [x] 1.3 Implementar pruebas unitarias en `UserPreferencesRepositoryTest` para las nuevas preferencias de respaldo automático.

## Fase 2: Motor de Respaldo en Segundo Plano (WorkManager)
- [x] 2.1 Implementar `AutoBackupWorker` en `data/worker/AutoBackupWorker.kt` utilizando `CoroutineWorker`, `ExportBackupUseCase` y `DocumentFile`.
- [x] 2.2 Implementar `AutoBackupScheduler` en `data/worker/AutoBackupScheduler.kt` con métodos `scheduleDailyBackup()` y `cancelAutoBackup()`.
- [x] 2.3 Implementar método para ejecución inmediata silenciosa de respaldo tras vincular carpeta o confirmar comidas.
- [x] 2.4 Implementar pruebas unitarias para `AutoBackupWorker` y `AutoBackupScheduler` con MockK.

## Fase 3: Reglas Nativas de Respaldo de Android (Cloud Auto Backup)
- [x] 3.1 Crear `app/src/main/res/xml/backup_rules.xml` con inclusión de base de datos Room y DataStore.
- [x] 3.2 Crear `app/src/main/res/xml/data_extraction_rules.xml` para compatibilidad con Android 12+ (API 31+).
- [x] 3.3 Configurar `AndroidManifest.xml` con `android:allowBackup="true"`, `dataExtractionRules` y `fullBackupContent`.

## Fase 4: Contratos de Estado y ViewModel en Ajustes
- [x] 4.1 Actualizar `SettingsUiState` en `SettingsContract.kt` con `autoBackupEnabled: Boolean`, `autoBackupFolderUri: String?`, `autoBackupFolderName: String?` y `lastAutoBackupTimestamp: Long?`.
- [x] 4.2 Agregar en `SettingsViewModel`: `onSelectAutoBackupFolder(Uri)`, `onToggleAutoBackup(Boolean)` y persistencia de permisos (`takePersistableUriPermission`).
- [x] 4.3 Actualizar pruebas unitarias en `SettingsViewModelTest` cubriendo vinculación de carpeta y activación de respaldo automático.

## Fase 5: UI Compose en Pantalla de Ajustes
- [x] 5.1 Actualizar `BackupAndRestoreSection` para incluir el selector de carpeta Google Drive, switch de activación y estado del último respaldo automático.
- [x] 5.2 Integrar el launcher `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree())` en `SettingsScreen.kt`.
- [x] 5.3 Agregar strings localizados en Español Chileno (`es-CL`) sin voseo en `strings.xml` y `values-es-rCL/strings.xml`.

## Fase 6: Verificación, Compilación y Entrega
- [x] 6.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 6.2 Compilar APK en modo debug (`./gradlew assembleDebug`).
- [x] 6.3 Ejecutar el script obligatorio de envío del APK a Telegram (`./scripts/send_apk_telegram.sh`).
