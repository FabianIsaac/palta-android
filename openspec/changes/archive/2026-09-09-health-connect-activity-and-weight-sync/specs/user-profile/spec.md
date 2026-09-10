# User Profile Specification Delta: Smart Scale & Weight Sync

## Requirements

### Requirement: Smart Scale Weight Synchronization from Health Connect
The system SHALL read the most recent weight record available in Android Health Connect from external smart scales or health tracking apps, enabling the user to synchronize and update their profile body weight.

#### Scenario: Detecting and displaying latest smart scale weighing
- **GIVEN** that the user's smart scale logged a weight entry of 76.2 kg earlier today into Health Connect
- **WHEN** the user opens the settings and profile screen
- **THEN** the system SHALL display the detected measurement (e.g. "Último pesaje detectado: 76,2 kg") along with its recorded timestamp

#### Scenario: Applying detected weight to user profile
- **GIVEN** a newly detected weight of 76.2 kg in Health Connect and a current profile weight of 77.0 kg
- **WHEN** the user taps "Actualizar peso"
- **THEN** the system SHALL update the user's profile weight to 76.2 kg, persist it in DataStore, and show a success confirmation message in Chilean Spanish

#### Scenario: Handling missing weight permission
- **GIVEN** that the user has not granted the `READ_WEIGHT` permission for Health Connect
- **WHEN** the user attempts to synchronize their weight
- **THEN** the system SHALL launch the Health Connect permission rationale dialog explaining clearly why the permission is needed

---

### Requirement: Health Activity and Budget Settings Configuration
The system SHALL allow users to toggle activity synchronization and choose whether burned calories are added to their daily caloric budget.

#### Scenario: Toggling burned calories inclusion in energy budget
- **GIVEN** the settings screen
- **WHEN** the user toggles "Sumar calorías quemadas al presupuesto diario" to enabled
- **THEN** the system SHALL persist this setting in DataStore and notify the user with an informative tip about calorie deficit and weight loss goals
