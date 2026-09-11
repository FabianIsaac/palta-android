# Propuesta: Corrección del Worker de Respaldo Automático y Sincronización Inmediata con Google Drive

## 1. Problema y Justificación

Tras vincular una carpeta de Google Drive (SAF `OpenDocumentTree`) y activar el interruptor de sincronización diaria, el respaldo automático nunca llega a ejecutarse en los dispositivos de los usuarios. En la pantalla de Ajustes, el estado permanece indefinidamente en:
> *"Aún no se ha ejecutado el primer respaldo automático"*

Al mismo tiempo, la subsección de *"Acciones manuales"* muestra la fecha de la última exportación manual realizada por el usuario (ej. *"Último respaldo manual: 09/09/2026 a las 21:04 hrs"*). Esto genera una severa confusión en el usuario, haciéndole creer que la aplicación no realiza copias automáticas y que requiere que él guarde manualmente los archivos en Google Drive para no perder sus datos.

### Causa Raíz Técnica
1. **Fallo de instanciación por reflexión en WorkManager:**  
   En `AutoBackupWorker.kt`, la clase se declaró con 7 parámetros con valores por defecto en su constructor primario. En Kotlin, esto compila únicamente el constructor completo de 7 parámetros y un constructor sintético con máscara de bits. Android WorkManager utiliza la fábrica por defecto `WorkerFactory`, la cual busca estrictamente por reflexión Java el constructor `(Context.class, WorkerParameters.class)`. Al no existir en el bytecode, WorkManager arroja internamente un `NoSuchMethodException`, abortando la ejecución antes de invocar `doWork()`.
2. **Falta de disparo inmediato garantizado:**  
   Cuando el usuario activa el interruptor de respaldo automático, `onToggleAutoBackup(true)` solo encola un `PeriodicWorkRequest`, el cual en Android no se ejecuta al instante sino tras el primer intervalo (24 horas) o según la optimización de batería del sistema.
3. **Ausencia de botón de sincronización bajo demanda:**  
   No existe una forma sencilla para que el usuario fuerce una sincronización inmediata a la carpeta de Drive vinculada y compruebe que el archivo `palta_respaldo_automatico.json` se creó correctamente sin tener que esperar la ventana nocturna del sistema.

---

## 2. Alcance (In-Scope)

- **Corrección del constructor de `AutoBackupWorker`:**
  - Agregar constructor secundario estándar `constructor(context: Context, workerParams: WorkerParameters)` o `@JvmOverloads` garantizando la presencia del constructor binario `(Context, WorkerParameters)` en el bytecode de Java para su instanciación transparente por parte de WorkManager.
- **Disparo inmediato de respaldo:**
  - Asegurar que tanto al vincular la carpeta como al activar el interruptor de sincronización automática se despache de inmediato una tarea de respaldo en segundo plano (`OneTimeWorkRequest`), actualizando la marca de tiempo `last_auto_backup_timestamp` en cuanto finalice.
- **Botón «Sincronizar ahora» en Google Drive:**
  - Incorporar en la tarjeta de *"Respaldo automático en Google Drive"* un botón de acción rápida para sincronizar en el momento hacia la carpeta vinculada, con indicador de carga y mensaje de confirmación en pantalla.
- **Clarificación de la interfaz de usuario:**
  - Separar conceptualmente la sincronización automática con Google Drive respecto a la exportación/importación manual de archivos, dejando claro que las acciones manuales son solo una opción offline alternativa.
- **Pruebas y Verificación:**
  - Pruebas unitarias que validen la instanciación de `AutoBackupWorker` mediante reflexión con `(Context, WorkerParameters)`.
  - Pruebas unitarias de los nuevos flujos en `SettingsViewModelTest` y `AutoBackupSchedulerTest`.
  - Compilación de APK y envío a Telegram (`./scripts/send_apk_telegram.sh`).

---

## 3. Fuera de Alcance (Out-of-Scope)

- Modificación del mecanismo de almacenamiento SAF (se mantiene la carpeta vinculada vía `OpenDocumentTree`, garantizando privacidad total y arquitectura offline-first sin requerir credenciales de Google ni servicios remotos propietarios).
- Versionado múltiple de copias dentro de Drive (se mantiene el archivo maestro `palta_respaldo_automatico.json` para evitar saturación de espacio).
