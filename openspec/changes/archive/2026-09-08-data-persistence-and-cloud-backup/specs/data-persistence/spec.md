# Data Persistence and Cloud Backup Specification

## Requirements

### Requirement: Non-Destructive Database Persistence Across Updates
The system SHALL preserve all existing records (meals, food items, supplements, supplement logs, preferences, and streaks) when updating the application version or migrating database schemas, preventing automatic data clearing.

#### Scenario: Retaining user records during application update
- **GIVEN** an installed application with logged meals and supplement entries
- **WHEN** a new version of the application is installed over the current version
- **THEN** the system SHALL retain all existing records without executing destructive table drops

#### Scenario: Database schema evolution
- **GIVEN** an existing database with user records
- **WHEN** the database version increments due to schema updates
- **THEN** the system SHALL execute deterministic migrations and keep all preexisting user data intact

---

### Requirement: Full Data Backup Generation (Google Drive and Local Storage)
The system SHALL serialize all user data (meals, food items, supplements, logs, nutrition goals, and meal time windows) into a validated JSON payload and allow exporting it directly to the user's chosen location, including Google Drive or local storage, via the Android Storage Access Framework.

#### Scenario: Successfully exporting backup to Google Drive
- **GIVEN** the Settings screen with accumulated meals and supplements
- **WHEN** the user taps "Crear copia de seguridad" and selects a Google Drive folder in the system document picker
- **THEN** the system SHALL write the complete JSON backup payload to the selected destination, record the current timestamp as the last backup date, and display a Chilean Spanish confirmation message ("Copia de seguridad guardada con éxito")

#### Scenario: Handling export cancellation
- **GIVEN** the document creation picker opened from the Settings screen
- **WHEN** the user cancels or dismisses the picker without selecting a location
- **THEN** the system SHALL abort the export operation cleanly without altering the last backup timestamp or displaying an error

---

### Requirement: Backup Restoration with Integrity Validation and User Confirmation
The system SHALL allow the user to select an existing backup file from Google Drive or local storage, validate its format and version, present a preview summary of the records to be restored, and require explicit confirmation before atomically importing the data.

#### Scenario: Previewing and confirming backup restoration from Google Drive
- **GIVEN** the Settings screen and an existing backup file on Google Drive containing 25 meals and 4 supplements
- **WHEN** the user taps "Restaurar copia de seguridad" and selects the backup file from Google Drive
- **THEN** the system SHALL parse the payload and display a confirmation dialog showing the date of the backup and record counts ("25 comidas y 4 suplementos")
- **AND WHEN** the user confirms the restoration
- **THEN** the system SHALL atomically insert the records, update preferences, and show a confirmation message ("Datos restaurados exitosamente")

#### Scenario: Attempting to restore a corrupted or invalid file
- **GIVEN** a selected file that is not a valid backup JSON payload or contains unparseable data
- **WHEN** the system attempts to parse the file for restoration
- **THEN** the system SHALL prevent any database modifications, reject the file, and display an informative error message ("El archivo seleccionado no es una copia de seguridad válida")

---

### Requirement: Settings Screen Backup & Restore Management
The system SHALL present a dedicated "Copia de Seguridad y Restauración" section in the Settings screen, displaying the timestamp of the last backup created or restored and providing clear, accessible action buttons in Chilean Spanish (`es-CL`).

#### Scenario: Viewing backup status with prior backup
- **GIVEN** a backup previously created today at 15:30
- **WHEN** the user opens the Settings screen
- **THEN** the system SHALL display "Último respaldo: Hoy, 15:30 hrs" along with options to create a new backup or restore an existing one

#### Scenario: Viewing backup status with no prior backup
- **GIVEN** a fresh installation with no prior backup created
- **WHEN** the user opens the Settings screen
- **THEN** the system SHALL display "Sin copias de seguridad registradas"
