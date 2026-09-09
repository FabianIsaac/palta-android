# Propuesta: Persistencia de Datos Robusta y Respaldo en la Nube (Google Drive / Local)

## 1. Problema y Justificación

Al utilizar la aplicación de manera cotidiana e instalar nuevas versiones compiladas enviadas a través de Telegram, el usuario experimenta la pérdida total de los datos registrados (comidas históricas, alimentos desglosados, registros de suplementos y configuraciones personalizadas).

Tras el análisis exhaustivo del código fuente, se identificaron las causas raíz y oportunidades de mejora:

1. **Migraciones destructivas en la base de datos Room (`fallbackToDestructiveMigration`):**
   En `AppDatabase.kt`, la base de datos está configurada con `.fallbackToDestructiveMigration()`. Cada vez que el esquema de la base de datos se modifica y se incrementa el número de versión (por ejemplo, pasando de versión 1 a 4 para dar soporte a nuevas características como suplementos o personalización), Room borra por completo todas las tablas de SQLite y las recrea vacías, descartando todo el historial del usuario.

2. **Falta de un mecanismo de respaldo y restauración (Backup & Restore):**
   La aplicación no dispone actualmente de ninguna función para que el usuario pueda exportar una copia de seguridad de sus datos ni restaurarla ante imprevistos, cambios de dispositivo o reinstalaciones completas.

3. **Inexistencia de respaldo en Google Drive:**
   El usuario necesita una experiencia de respaldo accesible, confiable y privada similar a la de aplicaciones masivas como WhatsApp, donde pueda salvaguardar su información en su Google Drive personal sin depender de servidores propietarios de terceros.

4. **Falta de incremento automático de `versionCode` en compilación de APK:**
   `app/build.gradle.kts` mantiene `versionCode = 1` estático. Esto puede inducir problemas de conflicto de paquetes en algunos instaladores de Android al sobrescribir versiones sucesivas.

---

## 2. Alcance (In-Scope)

- **Eliminación de migraciones destructivas en Room:**
  - Desactivar `fallbackToDestructiveMigration()` en `AppDatabase`.
  - Implementar migraciones automáticas (`@AutoMigration`) o migraciones manuales deterministas de Room entre versiones sucesivas para preservar intacta la información del usuario al actualizar el APK.

- **Modelo de Respaldo Completo y Versionado (`BackupPayload`):**
  - Diseñar una estructura de datos inmutable y serializable (JSON) que empaquete:
    - Versión del formato de respaldo y marca de tiempo (`timestamp`).
    - Catálogo de comidas registradas (`MealEntry`) con sus alimentos desglosados (`MealFoodItem`).
    - Catálogo y bitácora de suplementos consumidos (`Supplement` y `SupplementLog`).
    - Metas y preferencias nutricionales (calorías diarias, macronutrientes y ventanas horarias de comidas).

- **Exportación de Copia de Seguridad a Google Drive o Almacenamiento Local:**
  - Integrar Android Storage Access Framework (`ActivityResultContracts.CreateDocument`) para que el usuario elija dónde guardar su respaldo.
  - El selector nativo de Android permite guardar directamente en **Google Drive** (carpeta personal del usuario) o en el almacenamiento local (Descargas), con nombre sugerido claro (ej. `palta_respaldo_2026-09-08.json`).
  - Serialización asíncrona segura con Kotlinx Serialization y feedback visual de progreso.

- **Restauración de Copia de Seguridad desde Google Drive o Archivo:**
  - Integrar `ActivityResultContracts.OpenDocument` para seleccionar un archivo de respaldo desde Google Drive o almacenamiento local.
  - Validación rigurosa de integridad y esquema antes de aplicar los datos.
  - Restauración transaccional: inserción de datos en Room y DataStore respetando identificadores y relaciones.
  - Diálogo de confirmación antes de restaurar, advirtiendo al usuario y mostrando un resumen previo (cantidad de comidas y registros encontrados en la copia).

- **Gestión visual en Pantalla de Ajustes (`SettingsScreen`):**
  - Nueva sección dedicada: *"Copia de Seguridad y Restauración"*.
  - Indicador de estado y fecha/hora del último respaldo generado o restaurado.
  - Acciones claras con botones accesibles: *"Crear copia de seguridad"* y *"Restaurar copia de seguridad"*.
  - Avisos (Snackbar / Diálogo) con mensajes en Español Chileno (`es-CL`).

- **Versionado dinámico en compilación:**
  - Asegurar que el script de envío a Telegram (`scripts/send_apk_telegram.sh`) y `build.gradle.kts` gestionen o permitan un `versionCode` incremental.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Sincronización continua en tiempo real mediante base de datos remota propietaria (Supabase/Firebase con autenticación obligatoria), ya que el proyecto prioriza la arquitectura **100% Offline-First** y privacidad absoluta del usuario.
- Cifrado con contraseña personalizada del archivo de respaldo (el almacenamiento en Google Drive ya está protegido por la cuenta de Google del usuario; se podrá evaluar como mejora futura).
