# Especificación: Estadísticas y Calendario Mensual de Hábitos

## Requirements

### Requirement: Period Nutritional Statistics and Range Selection
The system SHALL aggregate logged food items and supplements across a user-selected time range (7 days, 14 days, or 30 days), compute daily averages for calories (kcal), protein (g), carbohydrates (g), and fat (g), and calculate the percentage of days with habit completion and supplement adherence.

#### Scenario: Viewing weekly statistics (7 days)
- **GIVEN** the user opens the Statistics screen
- **WHEN** the user selects the "7 días" range chip
- **THEN** the system SHALL compute and display the daily calorie average, average macros in grams, habit completion count, and current streak for the last 7 calendar days

#### Scenario: Switching to monthly statistics (30 days)
- **GIVEN** the Statistics screen with 7 days active
- **WHEN** the user taps the "30 días" range chip
- **THEN** the system SHALL immediately recalculate the daily metrics and display the 30-day averages and trend data

---

### Requirement: Calorie Evolution Chart with Target Reference Line
The system SHALL display a daily bar chart showing total energy intake in kcal for each day in the selected period, rendered alongside a horizontal reference line indicating the user's daily calorie target without punitive status indicators or streak deductions for exceeding or falling below the target.

#### Scenario: Displaying daily calorie intake against target line
- **GIVEN** a configured daily target of 2000 kcal and daily intakes of 1900 kcal, 2150 kcal, and 1980 kcal
- **WHEN** the user inspects the Calorie Evolution chart
- **THEN** the system SHALL render proportional vertical bars for each day and draw a horizontal reference line at the 2000 kcal mark

---

### Requirement: Interactive Monthly Habit Calendar
The system SHALL provide an interactive monthly calendar view accessible from the top app bar, displaying a grid of calendar days for the selected month, indicating whether the Chilean three-meal habit (Desayuno, Almuerzo, Once / Cena) was fulfilled and whether supplements were taken on each day.

#### Scenario: Opening the monthly calendar from the top bar
- **GIVEN** the user is viewing the Statistics screen or the Daily Summary screen
- **WHEN** the user taps the calendar icon button in the top app bar
- **THEN** the system SHALL open the monthly calendar showing the current month with days of the week from Monday to Sunday

#### Scenario: Displaying habit completion badge on fulfilled days
- **GIVEN** a day where the user logged at least one meal in Desayuno, one in Almuerzo, and one in Once / Cena
- **WHEN** the monthly calendar is rendered
- **THEN** the system SHALL display a habit completion indicator icon on that day's cell

---

### Requirement: Calendar Day Inspection and Direct Navigation to Diary
The system SHALL allow the user to select any day on the monthly calendar to preview its total calories, macronutrients, meal category status, and supplement intake, and provide a direct action button to jump to that date in the Daily Summary screen.

#### Scenario: Selecting a past date and navigating to the diary
- **GIVEN** the monthly calendar modal open with the 4th day of the month selected
- **WHEN** the user taps "Ir al Diario de este día"
- **THEN** the system SHALL dismiss the calendar modal, set the 4th day of the month as the active date in the Daily Summary, and navigate to the Daily Summary screen
