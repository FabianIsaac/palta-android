# Daily Summary Specification

## Requirements

### Requirement: Daily Calorie Budget and Balance
The system SHALL aggregate all logged food items for a chosen calendar date, calculate total calories consumed, and display remaining calories against the daily target.

$$\text{Calorías Restantes} = \text{Calorías Objetivo} - \text{Calorías Consumidas}$$

#### Scenario: Displaying daily caloric balance within limit
- **GIVEN** a daily target of 2200 kcal and logged meals totaling 1600 kcal
- **WHEN** the user views the Daily Summary screen
- **THEN** the system SHALL indicate 600 kcal remaining and display progress at 72.7%

#### Scenario: Displaying caloric surplus (over-budget)
- **GIVEN** a daily target of 2000 kcal and logged meals totaling 2250 kcal
- **WHEN** the user views the Daily Summary screen
- **THEN** the system SHALL display 250 kcal over budget with an alert color status indicator

---

### Requirement: Macronutrient Progress Tracking
The system SHALL calculate the sum of consumed Protein (g), Carbohydrates (g), and Fat (g) for the day and display individual progress bars against the respective daily targets.

#### Scenario: Macro breakdown visual indicators
- **GIVEN** daily targets of 150g Protein, 200g Carbs, and 65g Fat
- **WHEN** the user logs meals containing 75g Protein, 100g Carbs, and 30g Fat
- **THEN** the system SHALL show 50% progress for Protein, 50% for Carbs, and 46.1% for Fat

---

### Requirement: Date Navigation and Daily History
The system SHALL allow the user to navigate backward and forward between dates to review historical logs and past nutritional performance.

#### Scenario: Navigating to previous day
- **GIVEN** the current date view (e.g., today)
- **WHEN** the user taps the previous day button or swipes right
- **THEN** the system SHALL load and display the summary and meal logs corresponding strictly to that previous date
