# Diseño Técnico: Respaldo Automático en Google Drive (WorkManager + SAF OpenDocumentTree)

## 1. Arquitectura General y Flujo de Automatización

El diseño combina la capacidad de Android Storage Access Framework (SAF) para otorgar permisos persistentes sobre una carpeta (`takePersistableUriPermission`) con **Android WorkManager** para la ejecución autónoma y confiable en segundo plano.

```
+-----------------------------------------------------------------------------------------------+
|                      ARQUITECTURA DE RESPALDO AUTOMÁTICO EN GOOGLE DRIVE                      |
+-----------------------------------------------------------------------------------------------+
|                                                                                               |
|  [ SettingsScreen ]                                                                           |
|         │                                                                                     |
|         ├──────> ActivityResultContracts.OpenDocumentTree()                                   |
|         │                  │                                                                  |
|         │                  ▼                                                                  |
|         │        [ Selector de Carpetas de Android ]                                          |
|         │                  │ (Usuario elige carpeta "Palta" en Google Drive)                  |
|         │                  ▼                                                                  |
|         ├──────> contentResolver.takePersistableUriPermission(...)                            |
|         │                                                                                     |
|         ▼                                                                                     |
|  [ SettingsViewModel ] ──> [ UserPreferencesRepository ]                                      |
|         │                        │ (Guarda auto_backup_folder_uri, enabled = true)            |
|         ▼                        ▼                                                            |
|  [ AutoBackupScheduler ] ──> [ WorkManager ]                                                  |
|                                  │ (PeriodicWorkRequest cada 24 hrs + BatteryNotLow)          |
|                                  ▼                                                            |
|                         [ AutoBackupWorker ]                                                  |
|                                  │                                                            |
|                                  ├──────> [ ExportBackupUseCase ] ──> [ BackupDataPayload ]   |
|                                  │                                                            |
|                                  ▼                                                            |
|                         [ DocumentFile.fromTreeUri ] ──> [ Google Drive: palta_respaldo_auto.json ]
|                                  │                                                            |
|                                  ▼                                                            |
|                         [ last_auto_backup_timestamp ] actualizado en DataStore               |
|                                                                                               |
+-----------------------------------------------------------------------------------------------+
```

---

## 2. Flujo de Configuración y Permiso Persistente (Paso a Paso)

### A. Vinculación de Carpeta en Google Drive
1. En `SettingsScreen`, el usuario activa el interruptor *"Respaldo automático en Google Drive"* o pulsa *"Vincular carpeta"*.
2. Se lanza el contrato `ActivityResultContracts.OpenDocumentTree()`.
3. El usuario navega en el selector nativo de Android hacia su cuenta de Google Drive y selecciona una carpeta (ej. `Mi Unidad/Palta`).
4. Al recibir la `treeUri`:
   ```kotlin
   val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
   context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
   ```
5. Se extrae un nombre amigable o la ruta relativa para mostrar en la interfaz (ej. `"Google Drive (Palta)"`).
6. Se persiste en DataStore:
   - `auto_backup_enabled = true`
   - `auto_backup_folder_uri = treeUri.toString()`
7. Se programa de inmediato el worker periódico en `WorkManager` y se realiza un primer respaldo silencioso de sincronización inicial.

### B. Desvinculación de Carpeta
1. El usuario desactiva el interruptor o pulsa *"Desvincular"*.
2. Se libera el permiso persistente si está activo (`releasePersistableUriPermission`).
3. Se actualiza DataStore: `auto_backup_enabled = false`, `auto_backup_folder_uri = null`.
4. Se cancela el worker único en WorkManager: `WorkManager.cancelUniqueWork("palta_auto_backup")`.

---

## 3. Ejecución del Worker en Segundo Plano (`AutoBackupWorker`)

Se implementa `AutoBackupWorker` heredando de `CoroutineWorker`:
- **Nombre único de trabajo:** `"palta_auto_backup"`.
- **Periodicidad:** 24 horas (intervalo mínimo recomendado por Android).
- **Restricciones:**
  - `setRequiresBatteryNotLow(true)`: Solo se ejecuta si el dispositivo no está con batería crítica.
  - `setRequiresStorageNotLow(true)`: Requiere espacio de almacenamiento suficiente.
- **Lógica de `doWork()`:**
  1. Consulta `userPreferencesRepository.isAutoBackupEnabled()` y `autoBackupFolderUri`.
  2. Si no está habilitado o la URI es nula, retorna `Result.success()`.
  3. Obtiene el árbol mediante `DocumentFile.fromTreeUri(applicationContext, Uri.parse(folderUri))`.
  4. Si el directorio no existe o no tiene permisos de escritura, registra advertencia y retorna `Result.retry()`.
  5. Localiza si ya existe el archivo `palta_respaldo_automatico.json` dentro del directorio:
     - Si existe, utiliza su URI.
     - Si no existe, invoca `folder.createFile("application/json", "palta_respaldo_automatico.json")`.
  6. Invoca `exportBackupUseCase()` para compilar el `BackupDataPayload` actualizado (comidas, alimentos, suplementos, bitácora y preferencias).
  7. Abre `applicationContext.contentResolver.openOutputStream(file.uri, "wt")` (modo truncado/escritura limpia).
  8. Escribe el JSON formateado y hace `flush()`.
  9. Actualiza `last_auto_backup_timestamp` con la fecha y hora actual.
  10. Retorna `Result.success()`.

---

## 4. Respaldo Nativo de Android (`android:allowBackup`)

En paralelo al archivo visible en Google Drive:
1. En `AndroidManifest.xml` se configura:
   ```xml
   android:allowBackup="true"
   android:dataExtractionRules="@xml/data_extraction_rules"
   android:fullBackupContent="@xml/backup_rules"
   ```
2. En `res/xml/data_extraction_rules.xml` y `backup_rules.xml`:
   - Se incluye la base de datos Room: `database/calculadora_calorias_database`.
   - Se incluyen las preferencias DataStore.
   - Esto permite que el servicio del sistema Android efectúe una copia encriptada en la partición de respaldo de la cuenta de Google del usuario.

---

## 5. Diseño de Interfaz en `SettingsScreen`

Se amplía la tarjeta `BackupAndRestoreSection`:

```
+------------------------------------------------------------------+
|  [Icono CloudSync] Copia de Seguridad y Restauración             |
|                                                                  |
|  Guarda tus comidas, metas y suplementos en Google Drive         |
|  o en tu teléfono para no perder nada al actualizar la app.      |
|                                                                  |
|  --------------------------------------------------------------  |
|  RESPALDO AUTOMÁTICO EN GOOGLE DRIVE                             |
|                                                                  |
|  [Switch ON]  Sincronización automática diaria                   |
|                                                                  |
|  Carpeta: 📁 Google Drive / Palta                                |
|  Último respaldo automático: Hoy, 03:15 hrs                      |
|                                                                  |
|  [ Botón Tonal: Cambiar carpeta de Drive ]                       |
|                                                                  |
|  --------------------------------------------------------------  |
|  ACCIONES MANUALES                                               |
|                                                                  |
|  Último respaldo manual: 08/09/2026, 17:15 hrs                   |
|                                                                  |
|  [ Botón Outlined: Exportar archivo ] [ Botón Filled: Restaurar ]|
+------------------------------------------------------------------+
```

---

## 6. Pruebas y Validación

1. **Pruebas Unitarias:**
   - `UserPreferencesRepositoryTest`: almacenamiento y recuperación de `auto_backup_folder_uri`, `auto_backup_enabled` y `last_auto_backup_timestamp`.
   - `AutoBackupWorkerTest`: ejecución de `doWork()`, verificación de apertura de streams y manejo de carpetas inaccesibles.
   - `SettingsViewModelTest`: eventos `onSelectAutoBackupFolder(Uri)`, `onToggleAutoBackup(Boolean)` y actualización de estado.
2. **Pruebas de Integración y Regresión:**
   - Verificación de que los respaldos manuales continúen operando con normalidad.
   - Verificación de compilación de APK y suite completa de tests pasando.
