# Diseño Técnico: Corrección del Worker de Respaldo Automático y Sincronización Inmediata

## 1. Corrección del Constructor de `AutoBackupWorker`

### Diagnóstico de Bytecode
El constructor primario actual utiliza argumentos por defecto en Kotlin:
```kotlin
class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val exportBackupUseCaseProvider: () -> ExportBackupUseCase = { ... },
    ...
)
```
Sin constructores sobrecargados, el compilador Kotlin no emite el método `AutoBackupWorker(Context, WorkerParameters)`. Al ejecutarse en Android, `androidx.work.WorkerFactory` ejecuta:
```java
Constructor<? extends ListenableWorker> constructor =
    clazz.getDeclaredConstructor(Context.class, WorkerParameters.class);
return constructor.newInstance(appContext, workerParameters);
```
lanzando `NoSuchMethodException`.

### Solución Arquitectónica
Se añade un constructor secundario explícito de 2 parámetros, manteniendo la capacidad de inyección de dependencias para pruebas unitarias:
```kotlin
class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val exportBackupUseCaseProvider: () -> ExportBackupUseCase,
    private val userPreferencesRepositoryProvider: () -> UserPreferencesRepository,
    private val documentFileTreeProvider: (Context, Uri) -> DocumentFile?,
    private val openOutputStreamProvider: (Uri) -> OutputStream?,
    private val uriParser: (String) -> Uri
) : CoroutineWorker(context, workerParams) {

    // Constructor estándar requerido por androidx.work.WorkerFactory
    constructor(context: Context, workerParams: WorkerParameters) : this(
        context = context,
        workerParams = workerParams,
        exportBackupUseCaseProvider = {
            val database = AppDatabase.getDatabase(context)
            val userPrefs = UserPreferencesRepository(context.userDataStore)
            val backupRepo = RoomBackupRepository(
                appDatabase = database,
                mealDao = database.mealDao(),
                supplementDao = database.supplementDao(),
                supplementLogDao = database.supplementLogDao(),
                userPreferencesRepository = userPrefs
            )
            ExportBackupUseCase(backupRepo)
        },
        userPreferencesRepositoryProvider = { UserPreferencesRepository(context.userDataStore) },
        documentFileTreeProvider = { ctx, uri -> DocumentFile.fromTreeUri(ctx, uri) },
        openOutputStreamProvider = { uri -> context.contentResolver.openOutputStream(uri, "wt") },
        uriParser = { Uri.parse(it) }
    )
    ...
}
```

---

## 2. Flujo de Sincronización Inmediata y Bajo Demanda

```
+-----------------------------------------------------------------------------------------+
|                    FLUJO DE DISPARO Y SINCRONIZACIÓN INMEDIATA                          |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|  [ Usuario presiona "Sincronizar ahora" / Vincula carpeta / Activa Switch ]            |
|                                    │                                                    |
|                                    ▼                                                    |
|                      [ SettingsViewModel.onSyncDriveNow() ]                             |
|                                    │                                                    |
|                  ┌─────────────────┴─────────────────┐                                  |
|                  ▼                                   ▼                                  |
|     (UI: isSyncingDrive = true)           [ AutoBackupScheduler ]                       |
|                                                      │                                  |
|                                                      ▼                                  |
|                                           [ triggerImmediateBackup() ]                  |
|                                           - OneTimeWorkRequest                          |
|                                           - ExistingWorkPolicy.REPLACE                  |
|                                                      │                                  |
|                                                      ▼                                  |
|                                           [ AutoBackupWorker ]                          |
|                                           - Serializa datos Room/Prefs                  |
|                                           - Escribe palta_respaldo_automatico.json      |
|                                           - Actualiza last_auto_backup_timestamp        |
|                                                      │                                  |
|                                                      ▼                                  |
|                                        (UI: Fecha actualizada + Mensaje de éxito)       |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

### Casos de Uso del Disparo Inmediato:
1. **Al vincular carpeta por primera vez:** Se programa el trabajo periódico y se ejecuta de inmediato el respaldo para que la fecha quede registrada al segundo de configurarse.
2. **Al reactivar el interruptor diario:** Si ya había una carpeta vinculada y el usuario reactiva el switch, además de programar el trabajo de 24 horas se dispara una sincronización inmediata.
3. **Botón manual "Sincronizar ahora" en Google Drive:** Permite forzar la actualización del archivo en Drive en cualquier instante sin abrir selectores de archivos, mostrando indicador de carga.

---

## 3. Modificaciones en UI (`BackupAndRestoreSection.kt`)

En la sección de Google Drive:
1. **Indicador de estado claro:**
   - Si nunca se ha ejecutado: *"Aún no se ha sincronizado"*.
   - Si ya se ejecutó: *"Última sincronización con Drive: Hoy, 11:42 hrs"*.
2. **Fila de acciones de Google Drive:**
   - Botón de sincronización bajo demanda:
     - `OutlinedButton` o `FilledTonalButton` con ícono `Icons.Default.Refresh` / `CloudUpload` y texto *"Sincronizar ahora"*.
     - Estado deshabilitado con `CircularProgressIndicator` si una sincronización está en curso.
   - Botón *"Cambiar carpeta"*.
   - Botón de texto *"Desvincular"*.
3. **Separación semántica con Acciones Manuales:**
   - Se mantiene la sección de abajo pero con descripción nítida: *"Copia local en archivo JSON (Opcional)"*.

---

## 4. Pruebas y Validación

1. **Prueba de Instanciación por Reflexión:**
   - Nuevo test unitario en `AutoBackupWorkerTest` que instancie la clase mediante `AutoBackupWorker::class.java.getConstructor(Context::class.java, WorkerParameters::class.java).newInstance(context, workerParams)` comprobando que no lanza excepción.
2. **Prueba de ViewModel:**
   - Verificar en `SettingsViewModelTest` que `onSyncDriveNow()` invoca el scheduler y actualiza el estado.
   - Verificar que al activar `onToggleAutoBackup(true)` se llama a `triggerImmediateBackup()`.
3. **Prueba de Scheduler:**
   - Verificar que `triggerImmediateBackup()` encola `OneTimeWorkRequest` con política `REPLACE`.
