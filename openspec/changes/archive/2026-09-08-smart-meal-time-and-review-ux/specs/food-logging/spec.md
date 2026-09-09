# Food Logging Specification Delta

## Requirements

### Requirement: Smart Time-Based Meal Category Detection
The system SHALL automatically infer the appropriate Chilean meal category (Desayuno, Almuerzo, Once / Cena, Colaciones) based on the user's current local time and their configured meal time windows, applying it as the default selection whenever a new meal is logged.

#### Scenario: Detecting lunch during midday
- **GIVEN** meal time windows where Almuerzo is configured between 11:30 and 16:00
- **WHEN** the user opens the camera, text entry sheet, or manual entry at 13:45
- **THEN** the system SHALL pre-select "Almuerzo" as the meal category and present a clear option to switch categories if desired

#### Scenario: Detecting breakfast in the morning
- **GIVEN** meal time windows where Desayuno is configured between 06:00 and 11:30
- **WHEN** the user logs food at 08:30
- **THEN** the system SHALL pre-select "Desayuno" as the meal category

#### Scenario: Detecting once / cena in the evening
- **GIVEN** meal time windows where Once / Cena is configured between 16:00 and 22:00
- **WHEN** the user logs food at 19:30
- **THEN** the system SHALL pre-select "Once / Cena" as the meal category

---

### Requirement: Configurable Meal Time Windows
The system SHALL provide settings allowing the user to customize the start and end times for Desayuno, Almuerzo, and Once / Cena, persisting these preferences locally across app restarts.

#### Scenario: Updating lunch time window
- **GIVEN** the settings screen
- **WHEN** the user changes the start time of Almuerzo to 12:30 and saves
- **THEN** the system SHALL persist the updated time window and use 12:30 for future automated meal detection

---

### Requirement: Responsive Review Screen Actions Layout
The system SHALL render the meal review screen actions (including adding food items and describing with AI) with dedicated layout spacing that prevents button compression, awkward line breaking, or visual truncation across all screen densities.

#### Scenario: Adding items without label truncation
- **GIVEN** the meal review screen with detected food items
- **WHEN** the actions section is displayed
- **THEN** the "Agregar alimento" button SHALL remain fully legible on a single line with proper padding and icon alignment without clipping or circular deformation

---

### Requirement: Immediate Feedback on Text Description Analysis
The system SHALL transition immediately to a visible loading state upon submitting a food description in natural language, displaying progress animation and clear status feedback until parsing finishes.

#### Scenario: Submitting meal text description
- **GIVEN** the quick text entry sheet
- **WHEN** the user taps the interpret button with a valid description
- **THEN** the system SHALL dismiss the sheet and immediately show the full review loading indicator displaying "Interpretando tu comida con IA..." while the analyzer processes the request
