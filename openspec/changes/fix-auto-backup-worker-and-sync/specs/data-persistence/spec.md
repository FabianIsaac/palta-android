# Data Persistence and Cloud Backup Specification Delta

## Requirements

### MODIFIED Requirement: Settings Screen Backup & Restore Management
The system SHALL present a dedicated "Copia de Seguridad y Restauración" section in the Settings screen, clearly distinguishing between automatic background synchronization with Google Drive and optional manual local file export/import, displaying accurate timestamps and providing a direct action to synchronize with Drive on demand.

#### MODIFIED Scenario: Viewing backup status with prior backup
- **GIVEN** an automatic backup previously synchronized today at 15:30 to Google Drive
- **WHEN** the user opens the Settings screen
- **THEN** the system SHALL display "Última sincronización con Drive: Hoy, 15:30 hrs" in the cloud section and any manual backup timestamp separately in the manual actions section

#### ADDED Scenario: Forcing immediate synchronization with Google Drive
- **GIVEN** a linked Google Drive folder and automatic backup enabled
- **WHEN** the user taps "Sincronizar ahora" in the Google Drive section
- **THEN** the system SHALL immediately execute the backup export into the linked folder, display a loading indicator during the process, update the last sync timestamp upon completion, and notify the user with "Respaldo sincronizado con Google Drive exitosamente"

---

### ADDED Requirement: Reliable Background Worker Instantiation and Execution
The system SHALL ensure that background backup workers (`AutoBackupWorker`) can be instantiated by Android WorkManager via default reflection using a standard two-parameter constructor `(Context, WorkerParameters)` and executed reliably without runtime reflection errors.

#### ADDED Scenario: Instantiating worker via WorkManager reflection
- **GIVEN** an scheduled periodic or one-time backup work request
- **WHEN** Android WorkManager attempts to instantiate `AutoBackupWorker` via its default `WorkerFactory`
- **THEN** the system SHALL successfully construct the worker without throwing `NoSuchMethodException` and execute `doWork()` to serialize and write `palta_respaldo_automatico.json`

#### ADDED Scenario: Triggering immediate backup upon linking or toggling on
- **GIVEN** the user links a folder or toggles on automatic backup in Settings
- **WHEN** the preference is saved
- **THEN** the system SHALL schedule an immediate one-time work execution in addition to the daily periodic request, ensuring the first backup is executed without waiting for the 24-hour cycle
