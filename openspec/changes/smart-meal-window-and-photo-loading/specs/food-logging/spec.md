# Food Logging Specification Delta

## Requirements

### Requirement: Meal Entry Logging
The system SHALL allow users to log food entries categorized under meal slots adapted to Chilean conventions (Desayuno, Almuerzo, Once / Cena, Colaciones) for any selected date, and SHALL automatically consolidate new entries into an existing meal if logged within a 30-minute window of a previous meal in the same category.

#### Scenario: Consolidating items within the 30-minute window
- **GIVEN** an existing meal in the "Almuerzo" category logged at 13:15 containing "Pechuga de pollo" and "Arroz blanco"
- **WHEN** the user logs 6 "Wantanes fritos" under "Almuerzo" at 13:30 (15 minutes later)
- **THEN** the system SHALL append the "Wantanes fritos" to the existing 13:15 "Almuerzo" entry, recalculate the consolidated nutritional totals, and update any associated Health Connect record without creating a duplicate meal entry

#### Scenario: Creating a separate meal entry after 30 minutes
- **GIVEN** an existing meal in the "Almuerzo" category logged at 13:15
- **WHEN** the user logs food under "Almuerzo" at 14:00 (45 minutes later)
- **THEN** the system SHALL create a new, separate meal entry under "Almuerzo" with its own timestamp and nutritional totals

---

### Requirement: Interactive Portion and Macro Review
The system SHALL present an interactive review screen immediately upon capturing an image or submitting a text description, displaying a clear loading indicator during AI analysis, and showing a consolidation notice whenever the items will be merged into a recent meal.

#### Scenario: Immediate loading state on image capture
- **GIVEN** the food camera screen
- **WHEN** the user taps the capture button or selects an image from the gallery
- **THEN** the system SHALL immediately disable the capture controls and navigate to the review screen displaying an animated loading state with helpful nutritional guidance until AI analysis completes

#### Scenario: Reviewing additions with consolidation notice
- **GIVEN** a meal was logged under "Almuerzo" 10 minutes ago
- **WHEN** the user reviews newly detected or entered food items for "Almuerzo"
- **THEN** the system SHALL display only the newly added items in the editable list and show a prominent notice stating that these foods will be consolidated into the recent "Almuerzo"

#### Scenario: State reset after saving
- **GIVEN** a user has reviewed and confirmed a meal entry
- **WHEN** the meal is saved successfully and the user navigates away from the review screen
- **THEN** the review state SHALL be completely reset so that subsequent entries start with an empty item list
