# Delta Specification: Automatic Google Drive Cloud Backup

## Requirements

### ADDED Requirement: Persistent Cloud Folder Linking for Automatic Backup
The system SHALL allow the user to select and link a persistent folder on Google Drive or local storage via the Android Storage Access Framework (`OpenDocumentTree`), acquire permanent read and write permissions (`takePersistableUriPermission`), and store the folder URI in preferences to enable unattended background synchronization.

#### Scenario: Successfully linking a Google Drive folder
- **GIVEN** the Settings screen with automatic backup disabled or without a linked folder
- **WHEN** the user taps "Vincular carpeta de Google Drive" and selects a folder in Google Drive
- **THEN** the system SHALL acquire persistable URI permissions for the selected tree URI, persist the URI string and folder name in preferences, and update the UI to show the linked folder status

#### Scenario: Revoking or unlinking the backup folder
- **GIVEN** a previously linked Google Drive folder
- **WHEN** the user taps "Desvincular carpeta" or disables automatic backup
- **THEN** the system SHALL release persistable URI permissions if necessary, clear the stored folder URI, and cancel scheduled periodic backup workers

---

### ADDED Requirement: Background Automatic Backup Synchronization (WorkManager)
The system SHALL periodically and reliably synchronize user data (meals, food items, supplements, logs, nutrition goals, and meal time windows) into an updated JSON file named `palta_respaldo_automatico.json` inside the linked Google Drive folder using Android WorkManager, executing silently in the background without user intervention.

#### Scenario: Automatic daily backup execution in background
- **GIVEN** a linked Google Drive folder and automatic backup enabled
- **WHEN** the scheduled WorkManager worker triggers (e.g. daily when battery is not low)
- **THEN** the system SHALL export the current complete backup payload, create or overwrite `palta_respaldo_automatico.json` inside the linked Google Drive folder, update the last automatic backup timestamp, and report success

#### Scenario: Handling background backup when folder access is revoked or unavailable
- **GIVEN** a scheduled automatic backup execution where the linked Google Drive folder is inaccessible or permission was lost
- **WHEN** the background worker attempts to write the backup file
- **THEN** the system SHALL catch the security or I/O exception cleanly, record a failure status without crashing the app, and retry in the next scheduled interval

---

### MODIFIED Requirement: Settings Screen Backup & Restore Management
The system SHALL present an expanded "Copia de Seguridad y Restauración" section in the Settings screen, incorporating controls for automatic Google Drive backup, persistent folder linking, automatic sync status, and manual backup/restore actions in Chilean Spanish (`es-CL`).

#### Scenario: Viewing automatic backup status when configured
- **GIVEN** an active Google Drive folder linked and an automatic backup completed today at 04:00
- **WHEN** the user opens the Settings screen
- **THEN** the system SHALL display the automatic backup toggle enabled, the linked folder name, and "Último respaldo automático: Hoy, 04:00 hrs"

#### Scenario: Viewing automatic backup status when not configured
- **GIVEN** a fresh installation or no linked folder
- **WHEN** the user opens the Settings screen
- **THEN** the system SHALL display "Sin carpeta de Google Drive vinculada" and provide an action button to "Vincular carpeta"
