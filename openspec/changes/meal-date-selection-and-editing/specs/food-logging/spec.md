# Food Logging Specification (Delta)

## MODIFIED Requirements

### Requirement: Meal Entry Logging
The system SHALL allow users to log food entries categorized under meal slots adapted to Chilean conventions (Desayuno, Almuerzo, Once / Cena, Colaciones) associated with a user-selected target calendar date (inheriting by default the date active in the daily summary view), calculating the persisted epoch timestamp based on that target date and the current local time.

#### Scenario: Logging food for yesterday from daily summary view
- **GIVEN** the user is viewing yesterday's date in the daily summary screen
- **WHEN** the user initiates meal logging for "Almuerzo" and confirms the review
- **THEN** the system SHALL persist the meal entry with an epoch timestamp corresponding to yesterday's date, reflecting the meal immediately in yesterday's daily summary

#### Scenario: Logging food for today when viewing today's summary
- **GIVEN** the user is viewing the current day in the daily summary screen
- **WHEN** the user logs and confirms a meal
- **THEN** the system SHALL persist the meal entry with an epoch timestamp corresponding to the current day and time

---

## ADDED Requirements

### Requirement: Target Date Selection and Adjustment in Review
The system SHALL display the target registration date clearly on the food scan review screen and allow the user to modify the target date before confirming and saving the meal.

#### Scenario: User changes meal target date in review screen
- **GIVEN** the user is reviewing scanned food items with the default target date set to today
- **WHEN** the user taps the date selector and chooses yesterday's date
- **THEN** the system SHALL update the review state with the selected date and persist the meal with yesterday's timestamp upon tapping "Confirmar y Guardar"

---

### Requirement: Meal Date Modification in Existing Entries
The system SHALL allow users to modify the calendar date of any existing logged meal entry from the meal edit screen, preserving the original time-of-day and updating the database so that the meal shifts to the newly selected date.

#### Scenario: Moving a meal from today to yesterday
- **GIVEN** a meal registered today at 13:30 with an id of 42
- **WHEN** the user opens the meal editor, selects yesterday's date, and taps "Guardar Cambios"
- **THEN** the system SHALL update the meal's timestamp to yesterday at 13:30, removing it from today's summary and displaying it in yesterday's summary
