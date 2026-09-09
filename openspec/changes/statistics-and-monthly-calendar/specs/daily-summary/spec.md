# Especificación Delta: Resumen Diario (Acceso a Calendario Mensual)

## ADDED Requirements

### Requirement: Top App Bar Calendar Shortcut
The Daily Summary screen SHALL include a calendar action button in its top app bar that opens the monthly habit calendar, allowing the user to select any past or future date and jump immediately to that date's log without repeatedly pressing sequential day navigation buttons.

#### Scenario: Opening calendar from Daily Summary toolbar
- **GIVEN** the user is on the Daily Summary screen
- **WHEN** the user taps the calendar icon button in the top app bar
- **THEN** the system SHALL display the monthly habit calendar dialog with the currently viewed date highlighted

#### Scenario: Selecting a date in the calendar from Daily Summary
- **GIVEN** the calendar dialog opened from the Daily Summary screen
- **WHEN** the user selects a date (e.g., 5 days ago) and taps "Ir al Diario de este día"
- **THEN** the system SHALL close the dialog and immediately update the Daily Summary view to display that selected date's meals and nutritional balance
