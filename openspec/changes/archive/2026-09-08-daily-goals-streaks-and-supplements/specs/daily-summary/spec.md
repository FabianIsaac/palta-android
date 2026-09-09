# Daily Summary Specification Delta

## Requirements

### Requirement: Custom Daily Nutritional Target Configuration
The system SHALL allow users to manually configure their daily calorie budget and individual macronutrient targets (Protein, Carbohydrates, Fat) in grams within the settings screen, immediately applying the updated budget to the daily nutritional balance calculations.

#### Scenario: Setting nutritionist-recommended calorie target
- **GIVEN** the settings screen
- **WHEN** the user inputs a daily calorie target of 2300 kcal, with 160g Protein, 220g Carbohydrates, and 70g Fat, and saves
- **THEN** the system SHALL persist the updated budget and reflect 2300 kcal as the daily target in the Daily Summary screen

#### Scenario: Auto-balancing macronutrients based on calorie target
- **GIVEN** the daily target configuration form with 2400 kcal entered
- **WHEN** the user taps "Equilibrar macros automáticamente"
- **THEN** the system SHALL compute and suggest proportional targets of 180g Protein (30%), 240g Carbohydrates (40%), and 80g Fat (30%)

---

### Requirement: Daily Supplements Tracking
The system SHALL provide a daily checklist for nutritional supplements (including Omega 3) within the daily summary screen, allowing users to log their intake with a single tap for any selected date and track their corresponding nutritional contribution.

#### Scenario: Marking Omega 3 as taken today
- **GIVEN** the Daily Summary screen for the current date with "Omega 3 (2 cápsulas)" unchecked
- **WHEN** the user taps the checkbox to mark Omega 3 as taken
- **THEN** the system SHALL record the supplement as completed for today, visually update the checkbox to checked, and include its nutritional contribution (+18 kcal, 2g fat) in the daily consumed totals

#### Scenario: Unchecking a previously marked supplement
- **GIVEN** a supplement marked as taken on the selected date
- **WHEN** the user taps the checkbox again to deselect it
- **THEN** the system SHALL remove the completion log for that date and deduct its nutritional contribution from the daily summary

---

### Requirement: Daily Streak Calculation and Motivation ("Días Buenos")
The system SHALL calculate and display a streak of consecutive "Good Days" (días buenos), where a day is defined as completed if the user has logged at least one food entry in each of the three main Chilean meal categories (Desayuno, Almuerzo, and Once / Cena).

#### Scenario: Maintaining streak with all three main meals logged
- **GIVEN** a user with a current streak of 4 consecutive days
- **WHEN** the user logs their third main meal (Once / Cena) for today after having already logged Desayuno and Almuerzo
- **THEN** the system SHALL classify today as a completed day, increase the streak counter to 5 days, and show a celebratory message in Chilean Spanish

#### Scenario: Prompting for missing main meals to maintain streak
- **GIVEN** a user who has logged Desayuno and Almuerzo today, but not yet Once / Cena
- **WHEN** the user views the Daily Summary screen
- **THEN** the system SHALL display the current streak count, show checkmarks for Desayuno and Almuerzo, an empty circle for Once / Cena, and display a motivational message reminding the user to log Once / Cena to secure today's streak
