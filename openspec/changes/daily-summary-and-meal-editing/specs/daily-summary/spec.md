# Daily Summary Specification (Delta)

## ADDED Requirements

### Requirement: Meal Categorization and Quick Access
The system SHALL group and display all logged meals for the selected date under Chilean meal categories (*Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*), showing category-specific calorie totals, macro breakdown, and an entry point to edit or add items.

#### Scenario: Viewing meals grouped by Chilean categories
- **GIVEN** a user with a logged Breakfast of 450 kcal and a Lunch of 650 kcal for today
- **WHEN** the user opens the Daily Summary screen
- **THEN** the system SHALL display a card for "Desayuno" showing 450 kcal and a card for "Almuerzo" showing 650 kcal, while showing empty states with an "Agregar" action for "Once / Cena" and "Colaciones"

#### Scenario: Opening a meal for editing
- **GIVEN** an existing logged meal in the "Desayuno" category
- **WHEN** the user taps on the breakfast card or one of its items
- **THEN** the system SHALL navigate to the Edit Meal screen loaded with the breakfast details and food items

---

### Requirement: Real-time Synchronized Recalculation
The system SHALL reactively recalculate total daily calories, remaining calories, and macronutrient percentages whenever any meal entry for the active date is updated or removed.

#### Scenario: Immediate daily update after editing a previous meal
- **GIVEN** a daily calorie budget of 2000 kcal and logged meals totaling 1800 kcal (including 400 kcal from breakfast)
- **WHEN** the user edits the breakfast entry reducing its portion to 250 kcal
- **THEN** the system SHALL update the daily consumed calories to 1650 kcal and remaining calories to 350 kcal immediately upon returning to the summary screen
