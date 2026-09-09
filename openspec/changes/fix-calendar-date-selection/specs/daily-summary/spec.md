# Daily Summary Specification (Delta)

## MODIFIED Requirements

### Requirement: Date Navigation and Daily History
The system SHALL allow the user to navigate backward and forward between dates to review historical logs and past nutritional performance, including opening an interactive monthly habits calendar dialog that accurately reflects and updates the selected day across views.

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
