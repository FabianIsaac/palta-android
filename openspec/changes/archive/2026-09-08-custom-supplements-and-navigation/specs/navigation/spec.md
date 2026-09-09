# Delta Spec: Navegación y Estadísticas (Navigation & Statistics)

## ADDED Requirements

### Requirement: Symmetric 5-Destination Bottom Navigation Bar
The system SHALL provide a persistent bottom navigation bar featuring 4 side destinations symmetrically balanced around a centered floating action button for camera scanning: Diario and Estadísticas on the left, Suplementos and Ajustes on the right.

#### Scenario: Navigating to the Supplements screen from bottom navigation
- **GIVEN** any top-level screen displaying the bottom navigation bar
- **WHEN** the user taps the "Suplementos" navigation item
- **THEN** the system SHALL switch the destination to the full-screen Supplements module and highlight the Suplementos icon

#### Scenario: Navigating to the Statistics screen from bottom navigation
- **GIVEN** any top-level screen displaying the bottom navigation bar
- **WHEN** the user taps the "Estadísticas" navigation item
- **THEN** the system SHALL switch the destination to the Statistics screen and highlight the Estadísticas icon

---

### Requirement: Statistics Screen Placeholder
The system SHALL display an informative placeholder screen for the Statistics destination, explaining upcoming features including caloric progression graphs and macronutrient adherence metrics.

#### Scenario: Viewing the statistics placeholder
- **GIVEN** the Statistics screen
- **WHEN** rendered
- **THEN** the system SHALL display an informative illustration or icon, a title "Estadísticas", and a message in Chilean Spanish indicating that nutrition analytics are currently in development
