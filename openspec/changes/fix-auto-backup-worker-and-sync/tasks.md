# Tareas de Implementación: fix-auto-backup-worker-and-sync

## Fase 1: Corrección de AutoBackupWorker y Pruebas de Reflexión
- [x] 1.1 Añadir constructor secundario estándar `constructor(context: Context, workerParams: WorkerParameters)` en `AutoBackupWorker.kt` para garantizar compatibilidad con `WorkerFactory` de WorkManager en Android.
- [x] 1.2 Implementar prueba unitaria en `AutoBackupWorkerTest.kt` que verifique la instanciación de `AutoBackupWorker` por reflexión Java mediante `(Context, WorkerParameters)` sin excepciones.

## Fase 2: Sincronización Inmediata y Disparo On-Demand en ViewModel
- [x] 2.1 Actualizar `SettingsContract.kt` agregando `isSyncingDrive: Boolean = false` en `SettingsUiState`.
- [x] 2.2 Modificar `SettingsViewModel.kt`:
  - En `onToggleAutoBackup(true)`, invocar `autoBackupScheduler?.triggerImmediateBackup()` además del periódico.
  - Implementar método `onSyncDriveNow()` para forzar un respaldo inmediato hacia la carpeta vinculada con actualización de estado y mensajes de feedback.
- [x] 2.3 Actualizar y ampliar pruebas unitarias en `SettingsViewModelTest.kt` y `AutoBackupSchedulerTest.kt`.

## Fase 3: Interfaz de Usuario en Pantalla de Ajustes
- [x] 3.1 Actualizar `BackupAndRestoreSection.kt`:
  - Añadir botón *"Sincronizar ahora"* en la fila de acciones de Google Drive con ícono y estado de carga (`CircularProgressIndicator`).
  - Mejorar los textos de estado: *"Última sincronización con Drive: ..."* y clarificar la sección de exportación local manual.
- [x] 3.2 Conectar `onSyncDriveNow` en `SettingsScreen.kt` y verificar cableado en `MainActivity.kt`.
- [x] 3.3 Asegurar que todos los nuevos textos y diálogos utilicen Español Chileno (`es-CL`) sin voseo.

## Fase 4: Verificación, Compilación y Entrega
- [x] 4.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 4.2 Compilar APK en modo debug (`./gradlew assembleDebug`).
- [x] 4.3 Ejecutar el script obligatorio de envío del APK a Telegram (`./scripts/send_apk_telegram.sh`).
