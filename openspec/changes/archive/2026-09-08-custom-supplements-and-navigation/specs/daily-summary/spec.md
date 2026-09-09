# Delta Spec: Resumen Diario (Daily Summary)

## MODIFIED Requirements

### Requirement: Daily Supplements Tracking
The system SHALL provide a daily checklist for nutritional supplements within the daily summary screen, positioned strictly after all meal categories, allowing users to log their intake with a single tap for any selected date and track their corresponding nutritional contribution without presenting in-card management actions.

#### Scenario: Marking a custom supplement as taken today
- **GIVEN** the Daily Summary screen for the current date with "Creatina Monohidrato (5g)" unchecked
- **WHEN** the user taps the checkbox to mark it as taken
- **THEN** the system SHALL record the supplement as completed for today, visually update the checkbox to checked, and include its nutritional contribution in the daily consumed totals

#### Scenario: Visual placement of daily supplements checklist
- **GIVEN** the Daily Summary screen with logged meals
- **WHEN** the user scrolls down through the screen
- **THEN** the system SHALL render the "Mis Suplementos de hoy" card below the final meal category (Desayuno, Almuerzo, Once / Cena, Colaciones)
