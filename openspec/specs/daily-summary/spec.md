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
The system SHALL allow the user to navigate backward and forward between dates to review historical logs and past nutritional performance, including opening an interactive monthly habits calendar dialog that accurately reflects and updates the selected day across views.

#### Scenario: Navigating to previous day
- **GIVEN** the current date view (e.g., today)
- **WHEN** the user taps the previous day button or swipes right
- **THEN** the system SHALL load and display the summary and meal logs corresponding strictly to that previous date

#### Scenario: Opening monthly calendar from daily summary
- **GIVEN** the user is viewing a specific date in the daily summary screen (e.g. 5 days ago)
- **WHEN** the user taps the calendar icon or the date header
- **THEN** the system SHALL open the monthly habits calendar initialized to the month and date currently visible in the daily summary

#### Scenario: Selecting a different date in the calendar dialog
- **GIVEN** the monthly habits calendar dialog is open
- **WHEN** the user taps any day cell within the visible month
- **THEN** the system SHALL immediately highlight the selected cell and update the selected day detail card with the corresponding nutritional intake, habit indicators, and supplement completion status for that specific date

#### Scenario: Navigating to diary date from calendar dialog
- **GIVEN** the user has selected a date in the monthly habits calendar dialog
- **WHEN** the user taps "Ir al Diario de este día"
- **THEN** the system SHALL dismiss the dialog, update the daily summary screen to the selected date, and display the meals and progress of that date

---

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
The system SHALL provide a daily checklist for nutritional supplements within the daily summary screen, positioned strictly after all meal categories, allowing users to log their intake with a single tap for any selected date and track their corresponding nutritional contribution without presenting in-card management actions.

#### Scenario: Marking a custom supplement as taken today
- **GIVEN** the Daily Summary screen for the current date with "Creatina Monohidrato (5g)" unchecked
- **WHEN** the user taps the checkbox to mark it as taken
- **THEN** the system SHALL record the supplement as completed for today, visually update the checkbox to checked, and include its nutritional contribution in the daily consumed totals

#### Scenario: Unchecking a previously marked supplement
- **GIVEN** a supplement marked as taken on the selected date
- **WHEN** the user taps the checkbox again to deselect it
- **THEN** the system SHALL remove the completion log for that date and deduct its nutritional contribution from the daily summary

#### Scenario: Visual placement of daily supplements checklist
- **GIVEN** the Daily Summary screen with logged meals
- **WHEN** the user scrolls down through the screen
- **THEN** the system SHALL render the "Mis Suplementos de hoy" card below the final meal category (Desayuno, Almuerzo, Once / Cena, Colaciones)

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
