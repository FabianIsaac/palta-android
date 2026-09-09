# Propuesta: Respaldo Automático en Google Drive y Segundo Plano (WorkManager + SAF)

## 1. Problema y Justificación

Actualmente, la aplicación cuenta con un mecanismo de respaldo y restauración a través del Android Storage Access Framework (SAF). Sin embargo, este mecanismo es **exclusivamente manual**: depende de que el usuario recuerde ingresar a Ajustes, pulsar *"Crear copia de seguridad"* y seleccionar el destino en cada ocasión.

Si el usuario olvida realizar respaldos periódicos y sufre la pérdida del dispositivo, robo, daño irreparable o restablecimiento de fábrica, todo su historial de comidas registradas, alimentos desglosados, suplementos y metas nutricionales se perderá definitivamente. Almacenar copias únicamente en la memoria local del teléfono tampoco soluciona este riesgo.

El usuario necesita una solución automatizada, silenciosa y confiable que:
1. Guarde periódicamente sus datos directamente en su **Google Drive** personal sin requerir su intervención diaria.
2. Mantenga la privacidad absoluta y la arquitectura **Offline-First**, sin depender de bases de datos propietarias en la nube ni registros obligatorios con contraseñas de terceros.
3. Permita recuperar toda la información en un nuevo dispositivo en cuestión de segundos.

---

## 2. Alcance (In-Scope)

- **Vinculación de Carpeta Persistente en Google Drive (SAF `OpenDocumentTree`):**
  - Implementar el selector de directorios `ActivityResultContracts.OpenDocumentTree()`.
  - El usuario selecciona una sola vez su carpeta deseada en Google Drive (o en su almacenamiento local).
  - La aplicación solicita y adquiere permisos de acceso permanentes mediante `contentResolver.takePersistableUriPermission` con banderas de lectura y escritura (`FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION`).
  - Almacenar la URI del árbol (`backup_folder_uri`) y el nombre amigable de la carpeta en `UserPreferencesRepository` (DataStore).

- **Sincronización Automática en Segundo Plano con Android WorkManager (`AutoBackupWorker`):**
  - Diseñar e implementar `AutoBackupWorker` utilizando `CoroutineWorker`.
  - Programar un `PeriodicWorkRequest` con periodicidad de 24 horas y restricciones óptimas (`Constraints.Builder().setRequiresBatteryNotLow(true)`).
  - Disparo reactivo adicional: opción de actualizar el respaldo silencioso de forma asíncrona tras confirmar comidas si la carpeta está vinculada.
  - Serializar el `BackupDataPayload` actualizado y escribirlo atómicamente en `palta_respaldo_automatico.json` dentro del directorio de Google Drive usando `DocumentFile`.
  - Registrar la marca de tiempo del último respaldo automático exitoso (`last_auto_backup_timestamp`).

- **Reglas de Android Auto Backup Nativo:**
  - Configurar en `AndroidManifest.xml` las directivas de respaldo nativo de Google Cloud (`android:allowBackup="true"`, `dataExtractionRules` y `fullBackupContent`), asegurando que la base de datos de Room y las preferencias de DataStore sean respaldadas también por el propio sistema operativo Android.

- **Interfaz de Usuario en Pantalla de Ajustes (`SettingsScreen`):**
  - Ampliar la sección *"Copia de Seguridad y Restauración"* con controles de automatización:
    - Interruptor (Switch): *"Respaldo automático en Google Drive"*.
    - Botón para *"Vincular carpeta de Google Drive"* o *"Cambiar carpeta"*.
    - Indicador de estado claro: *"Carpeta vinculada: Google Drive / Palta"* o *"Sin carpeta vinculada"*.
    - Fecha y hora del último respaldo automático ejecutado.
    - Mensajes de confirmación y advertencia en Español Chileno (`es-CL`) sin voseo.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Implementación de un servidor remoto propietario con autenticación obligatoria (Firebase/Supabase), preservando el enfoque **Offline-First**.
- Gestión de versiones múltiples de archivos históricos dentro de Google Drive (se mantendrá un archivo maestro `palta_respaldo_automatico.json` que se sobrescribe de manera segura para no saturar el espacio de Drive del usuario).
