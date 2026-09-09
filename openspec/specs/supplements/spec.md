# Supplements Management Specification

## Requirements

### Requirement: Supplements Catalog Management
The system SHALL provide a dedicated full-screen interface ("Mis Suplementos") allowing users to create, activate, edit, and delete nutritional supplements that appear in their daily tracking routine.

#### Scenario: Creating a new supplement with custom dosage
- **GIVEN** the Supplements screen
- **WHEN** the user inputs "Proteína Whey", dosage "1 scoop (30g)", 120 kcal, 24g protein, 2g carbs, 1.5g fat and taps "Guardar suplemento"
- **THEN** the system SHALL persist the supplement in the local database and make it available in the active routine

#### Scenario: Activating a preconfigured supplement
- **GIVEN** the preconfigured suggestions list with "Citrato de Magnesio"
- **WHEN** the user taps the suggestion chip
- **THEN** the system SHALL activate the supplement and immediately include it in the active daily checklist

#### Scenario: Deleting a custom supplement
- **GIVEN** a custom supplement in the active routine
- **WHEN** the user taps the delete icon and confirms
- **THEN** the system SHALL remove the supplement from the catalog and no longer display it on subsequent daily summaries

---

### Requirement: AI-Assisted Supplement Nutrition Estimation
The system SHALL interpret a natural language supplement name or description using AI, automatically inferring realistic dosage, calories, and macronutrient breakdown (protein, carbs, fat in grams), presenting the results in editable fields.

#### Scenario: Estimating a calorie-free supplement
- **GIVEN** the new supplement form with "Creatina monohidrato" entered
- **WHEN** the user taps "Estimar macros con IA"
- **THEN** the system SHALL populate the dosage with "5g", calories with 0.0 kcal, and 0.0g for protein, carbs, and fat

#### Scenario: Estimating protein powder nutritional breakdown
- **GIVEN** the new supplement form with "Proteína isolate vainilla" entered
- **WHEN** the user taps "Estimar macros con IA"
- **THEN** the system SHALL populate typical values of ~1 scoop (30g), ~110-130 kcal, ~24-27g protein, and minimal carbs/fat

#### Scenario: Offline fallback estimation
- **GIVEN** a device without internet connection or without an AI API key
- **WHEN** the user requests AI estimation for a recognized supplement like "Omega 3"
- **THEN** the system SHALL resolve the nutritional values from the local offline fallback catalog (2 cápsulas, 18 kcal, 2g grasa)
