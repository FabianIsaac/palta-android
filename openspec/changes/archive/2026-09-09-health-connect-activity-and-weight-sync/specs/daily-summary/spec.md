# Daily Summary Specification Delta: Health Connect Activity Sync

## Requirements

### Requirement: Health Connect Activity and Exercise Tracking
The system SHALL query Android Health Connect for active calories burned and daily steps count corresponding to the selected date, displaying this activity in the Daily Summary screen and optionally incorporating burned calories into the remaining energy budget calculation according to user preference.

#### Scenario: Displaying daily active calories and steps from Health Connect
- **GIVEN** that the user has granted Health Connect activity permissions and recorded 420 kcal burned and 8,500 steps today
- **WHEN** the user views the Daily Summary screen for today
- **THEN** the system SHALL display an activity card showing 420 kcal burned and 8,500 steps for the day

#### Scenario: Remaining calories calculation when including burned calories
- **GIVEN** a daily target of 2000 kcal, logged meals of 1500 kcal, active burned calories of 400 kcal, and the setting "Sumar calorías quemadas al presupuesto diario" enabled
- **WHEN** the user views the Daily Summary screen
- **THEN** the system SHALL calculate remaining calories as:
  $$\text{Restantes} = 2000 - 1500 + 400 = 900\text{ kcal}$$
  and display the calculation breakdown clearly

#### Scenario: Remaining calories calculation when burned calories are not included (default)
- **GIVEN** a daily target of 2000 kcal, logged meals of 1500 kcal, active burned calories of 400 kcal, and the setting "Sumar calorías quemadas al presupuesto diario" disabled
- **WHEN** the user views the Daily Summary screen
- **THEN** the system SHALL calculate remaining calories as:
  $$\text{Restantes} = 2000 - 1500 = 500\text{ kcal}$$
  and display the 400 kcal burned as an informative fitness achievement without altering the nutritional intake limit

#### Scenario: Health Connect permissions not granted or unavailable
- **GIVEN** that the user has not granted activity permissions in Health Connect
- **WHEN** the user views the Daily Summary screen
- **THEN** the system SHALL operate normally with standard meal calculations, hide or show an unobtrusive prompt to connect activity data, and report 0 burned calories without crashing or blocking the UI
