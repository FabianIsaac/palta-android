# Bottom Navigation & App Navigation Specification

## Requirements

### Requirement: Persistent Bottom Navigation Bar
The system SHALL provide a bottom navigation bar on primary screens (`Diario` and `Ajustes`) featuring a left item for the food diary, an elevated center action button for scanning food, and a right item for settings.

#### Scenario: Switching from Diary to Settings via Bottom Navigation
- **GIVEN** the user is viewing the Daily Summary (`Diario`)
- **WHEN** the user taps the "Ajustes" button on the right of the bottom navigation bar
- **THEN** the system SHALL navigate to the Settings screen and highlight the "Ajustes" tab

#### Scenario: Launching the Food Scanner via Center Action Button
- **GIVEN** the user is viewing any primary screen (`Diario` or `Ajustes`) with the bottom bar visible
- **WHEN** the user taps the prominent center "Escanear" button
- **THEN** the system SHALL launch the Food Camera scanner screen and hide the bottom navigation bar

#### Scenario: Returning to Diary from Settings via Bottom Navigation
- **GIVEN** the user is viewing the Settings screen
- **WHEN** the user taps the "Diario" button on the left of the bottom navigation bar
- **THEN** the system SHALL navigate back to the Daily Summary screen and highlight the "Diario" tab

---

### Requirement: Immersive Secondary Screen Navigation
The system SHALL hide the bottom navigation bar when navigating to modal or focused secondary tasks, including camera capture (`FoodCameraScreen`), meal scan review (`FoodScanReviewScreen`), and meal editing (`EditMealScreen`).

#### Scenario: Hiding bottom navigation during camera capture
- **GIVEN** the user is on the Daily Summary screen
- **WHEN** the user initiates food scanning
- **THEN** the system SHALL render the camera preview in full screen without displaying the bottom navigation bar

---

### Requirement: Safe Back Navigation Handling
The system SHALL intercept hardware back button presses and navigation back gestures on all secondary screens, returning to the preceding screen or the food diary without closing the application.

#### Scenario: System back gesture pressed on Camera screen
- **GIVEN** the user is on the Food Camera screen
- **WHEN** the user performs Android's back gesture or presses the system back button
- **THEN** the system SHALL intercept the event and navigate back to the Daily Summary screen without finishing the Activity

#### Scenario: Tap visual back button on Camera screen
- **GIVEN** the user is on the Food Camera screen
- **WHEN** the user taps the back arrow button in the top header
- **THEN** the system SHALL return to the Daily Summary screen

#### Scenario: System back gesture pressed on Settings screen
- **GIVEN** the user is on the Settings screen
- **WHEN** the user performs Android's back gesture or presses the system back button
- **THEN** the system SHALL return to the Daily Summary screen without closing the application
