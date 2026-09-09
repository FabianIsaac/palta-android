# Diseño Técnico: Persistencia Robusta, Migraciones y Respaldo en la Nube (Google Drive)

## 1. Arquitectura General y Flujo de Datos

El diseño sigue estrictamente **Clean Architecture**, **UDF** y la premisa **Offline-First**. Para el respaldo hacia la nube (Google Drive), se aprovecha el **Android Storage Access Framework (SAF)**, permitiendo que el usuario guarde o recupere sus datos directamente en su unidad personal de Google Drive o en el almacenamiento local sin requerir credenciales complejas de Google Cloud en el cliente ni violar la privacidad del usuario.

```
+-----------------------------------------------------------------------------------------------+
|                                ARQUITECTURA DE RESPALDO Y PERSISTENCIA                        |
+-----------------------------------------------------------------------------------------------+
|                                                                                               |
|  [ SettingsScreen ]                                                                           |
|         │                                                                                     |
|         ├──────> ActivityResultContracts.CreateDocument("application/json")                   |
|         │                  │                                                                  |
|         │                  ▼                                                                  |
|         │        [ Selector de Android SAF ] ──> [ Google Drive ] / [ Almacenamiento Local ]  |
|         │                  │ (Devuelve Uri)                                                   |
|         ▼                  ▼                                                                  |
|  [ SettingsViewModel ] ──> [ ExportBackupUseCase ]                                            |
|                                    │                                                          |
|                                    ▼                                                          |
|                         [ BackupRepository ]                                                  |
|                                    │                                                          |
|             ┌──────────────────────┴──────────────────────┐                                   |
|             ▼                                             ▼                                   |
|    [ Room Database ]                             [ UserPreferences ]                          |
|    - MealDao (Comidas e ítems)                   - Presupuesto calórico / macros              |
|    - SupplementDao (Suplementos y catálogo)       - Ventanas horarias de comidas              |
|                                                                                               |
+-----------------------------------------------------------------------------------------------+
```

---

## 2. Modelo de Datos del Respaldo (`BackupDataPayload`)

Se define un modelo serializable inmutable en la capa de dominio (`domain/model/backup/`):

```kotlin
@Serializable
data class BackupDataPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val appVersionName: String,
    val meals: List<BackupMealEntry>,
    val supplements: List<BackupSupplement>,
    val supplementLogs: List<BackupSupplementLog>,
    val preferences: BackupPreferences
)

@Serializable
data class BackupMealEntry(
    val id: Long,
    val category: String,
    val timestamp: Long,
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalCarbsGrams: Double,
    val totalFatGrams: Double,
    val notes: String? = null,
    val items: List<BackupMealItem>
)

@Serializable
data class BackupMealItem(
    val id: Long,
    val name: String,
    val portionGrams: Double,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val householdMeasure: String? = null
)

@Serializable
data class BackupSupplement(
    val id: Long,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: String,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val category: String
)

@Serializable
data class BackupSupplementLog(
    val id: Long,
    val supplementId: Long,
    val supplementName: String,
    val timestamp: Long,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val dosageText: String
)

@Serializable
data class BackupPreferences(
    val targetCalories: Double,
    val targetProteinGrams: Double,
    val targetCarbsGrams: Double,
    val targetFatGrams: Double,
    val mealTimeWindows: BackupMealTimeWindows
)

@Serializable
data class BackupMealTimeWindows(
    val breakfastStartMinute: Int,
    val breakfastEndMinute: Int,
    val lunchStartMinute: Int,
    val lunchEndMinute: Int,
    val dinnerStartMinute: Int,
    val dinnerEndMinute: Int
)
```

---

## 3. Estrategia de Migraciones en Room Database

Para erradicar la pérdida de datos en actualizaciones de APK:

1. **Remover `fallbackToDestructiveMigration()`** en el `databaseBuilder` de `AppDatabase`.
2. **Definir migraciones deterministas:**
   - La base de datos actual se encuentra en `version = 4`.
   - Se crea un archivo de migraciones `AppDatabaseMigrations.kt`.
   - En caso de requerir futuros cambios de esquema, se utilizarán objetos `Migration(start, end)` o `@AutoMigration` con validación de exportSchema activada (`exportSchema = true` con carpeta `schemas` en el módulo app) para garantizar integridad referencial.
3. **Manejo controlado de fallback:**
   - Reemplazar el fallback destructivo general por `fallbackToDestructiveMigrationOnDowngrade()`, asegurando que al actualizar la app hacia adelante jamás se borren los datos.

---

## 4. Flujo de Exportación e Importación con Google Drive y SAF

### A. Exportar Copia de Seguridad
1. El usuario presiona *"Crear copia de seguridad"* en `SettingsScreen`.
2. El Composable invoca un `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"))`.
3. El sistema Android despliega el selector de almacenamiento con el nombre sugerido:  
   `palta_respaldo_YYYY-MM-DD.json`.
4. El usuario puede seleccionar **Google Drive** (carpeta personal o compartida) o la carpeta **Descargas** de su teléfono.
5. El launcher devuelve un `Uri`.
6. El ViewModel invoca `ExportBackupUseCase(uri)` en una corrutina en `Dispatchers.IO`:
   - Lee todos los registros de `MealDao`, `SupplementDao`, `SupplementLogDao` y `UserPreferencesRepository`.
   - Compila el objeto `BackupDataPayload`.
   - Serializa a JSON formateado con `Json { prettyPrint = true }`.
   - Escribe el flujo de bytes en el `OutputStream` del `ContentResolver`.
7. Registra en DataStore la marca temporal del último respaldo exitoso (`last_backup_timestamp`).
8. Muestra un Snackbar de éxito: *"Copia de seguridad guardada con éxito"*.

### B. Restaurar Copia de Seguridad
1. El usuario presiona *"Restaurar copia de seguridad"*.
2. El Composable invoca `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` filtrando por tipos MIME (`application/json`, `text/*`, `*/*`).
3. El usuario localiza su archivo en **Google Drive** o en sus archivos locales.
4. El launcher devuelve el `Uri`.
5. El ViewModel lee el archivo, valida su esquema e integridad y muestra un **diálogo modal de confirmación**:
   - Resumen del contenido: *"Este archivo contiene X comidas, Y suplementos y tus metas nutricionales del DD/MM/AAAA. ¿Deseas restaurar estos datos?"*.
   - Opciones: *"Cancelar"* y *"Restaurar"*.
6. Al confirmar, `ImportBackupUseCase`:
   - Ejecuta una transacción atómica en Room (`AppDatabase.runInTransaction`).
   - Inserta o reemplaza registros respetando la integridad de IDs foráneos.
   - Actualiza las metas nutricionales y ventanas horarias en `UserPreferencesRepository`.
7. Muestra mensaje de confirmación: *"Datos restaurados exitosamente"*.

---

## 5. Diseño de Interfaz en `SettingsScreen`

Se incorpora la tarjeta *"Copia de Seguridad y Restauración"* con Material Design 3:

```
+-------------------------------------------------------------+
|  [Icono Cloud/Storage]  Copia de Seguridad y Restauración   |
|                                                             |
|  Guarda tus comidas, metas y suplementos en Google Drive    |
|  o en tu teléfono para no perder nada al actualizar.        |
|                                                             |
|  Último respaldo: Hoy, 17:15 hrs                            |
|                                                             |
|  [ Botón Outlined: Crear copia de seguridad ]               |
|  [ Botón FilledTonal: Restaurar copia de seguridad ]        |
+-------------------------------------------------------------+
```

---

## 6. Pruebas y Validación

- **Pruebas Unitarias:**
  - Serialización y deserialización idéntica de `BackupDataPayloadTest`.
  - Validación de respaldos con versiones superiores o esquemas alterados (manejo robusto de excepciones).
  - Casos de uso `ExportBackupUseCaseTest` e `ImportBackupUseCaseTest` con mocks de repositorios y transacciones.
- **Pruebas de Integración y Migración:**
  - Verificación de que la instancia de `AppDatabase` mantenga los datos al instanciarla y cerrarla sin invocaciones destructivas.
