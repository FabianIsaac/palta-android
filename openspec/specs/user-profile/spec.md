# User Profile Specification

## Requirements

### Requirement: Biometric Profile Management
The system SHALL allow the user to define and update their demographic and biometric profile data, including biological sex, age in years, height in centimeters, and current weight in kilograms.

#### Scenario: First-time profile creation
- **GIVEN** a new user who opens the application for the first time
- **WHEN** the user submits valid biometric details (Sex: Male, Age: 28, Height: 178 cm, Weight: 75 kg)
- **THEN** the system SHALL store the profile locally and mark the initial onboarding as completed

#### Scenario: Validation of biometric values
- **GIVEN** the profile editing form
- **WHEN** the user inputs an invalid value (such as negative weight, age under 12, or height over 260 cm)
- **THEN** the system SHALL display an actionable validation error and prevent saving the profile

---

### Requirement: Basal Metabolic Rate (BMR) Calculation
The system SHALL compute the Basal Metabolic Rate (BMR) automatically whenever the user's age, sex, weight, or height changes, utilizing the Mifflin-St Jeor formula.

#### Scenario: BMR calculation for male
- **GIVEN** a male user with weight 80 kg, height 180 cm, and age 30
- **WHEN** the system calculates the BMR
- **THEN** the resulting BMR SHALL equal 1780 kcal/day (within ±1 kcal tolerance)

#### Scenario: BMR calculation for female
- **GIVEN** a female user with weight 60 kg, height 165 cm, and age 25
- **WHEN** the system calculates the BMR
- **THEN** the resulting BMR SHALL equal 1345 kcal/day (within ±1 kcal tolerance)

---

### Requirement: Total Daily Energy Expenditure (TDEE) and Target Calories
The system SHALL calculate the Total Daily Energy Expenditure (TDEE) by multiplying BMR by the chosen physical activity factor, and compute daily caloric targets based on the user's objective (Fat Loss, Maintenance, Muscle Gain).

#### Scenario: Moderate activity with fat loss goal
- **GIVEN** a user with a BMR of 1800 kcal and Moderate Activity level (factor 1.55, giving TDEE = 2790 kcal)
- **WHEN** the user selects a caloric deficit of 500 kcal for fat loss
- **THEN** the system SHALL set the daily target to 2290 kcal/day

#### Scenario: Muscle gain goal
- **GIVEN** a user with a TDEE of 2500 kcal
- **WHEN** the user selects muscle gain with a surplus of 300 kcal
- **THEN** the system SHALL set the daily target to 2800 kcal/day

---

### Requirement: Macronutrient Distribution Target
The system SHALL compute recommended daily macronutrient targets in grams based on the user's total calorie goal and configured macro split ratios.

#### Scenario: Standard 40/30/30 distribution
- **GIVEN** a daily target of 2000 kcal with a macro split of 30% Protein, 40% Carbohydrates, and 30% Fat
- **WHEN** the targets are computed
- **THEN** the system SHALL assign 150g Protein (600 kcal), 200g Carbohydrates (800 kcal), and ~67g Fat (600 kcal)
