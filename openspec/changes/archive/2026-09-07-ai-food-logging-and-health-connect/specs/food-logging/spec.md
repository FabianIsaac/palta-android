# Food Logging Specification (Delta)

## MODIFIED Requirements

### Requirement: Meal Entry Logging
The system SHALL allow users to log food entries categorized under meal slots adapted to Chilean conventions (Desayuno, Almuerzo, Once / Cena, Colaciones) for any selected date, originating either from manual entry or AI-assisted image scanning.

#### Scenario: Adding entries via AI review confirmation
- **GIVEN** a confirmed meal scan containing 150g of "Pechuga de pollo a la plancha" (247.5 kcal, 46.5g protein, 0g carbs, 5.4g fat) and 100g of "Palta hass" (160 kcal, 2g protein, 9g carbs, 15g fat)
- **WHEN** the user selects the category "Almuerzo" and presses "Confirmar y Guardar"
- **THEN** the system SHALL persist the meal entries in the local database associated with the active date and the "Almuerzo" category, updating daily nutritional totals immediately

---

## ADDED Requirements

### Requirement: Food Image Analysis via AI
The system SHALL provide an image analysis mechanism capable of identifying food items and estimating portions from camera captures or selected gallery images using either on-device models or cloud multimodal models (MiniMax).

#### Scenario: Successful image analysis with MiniMax API
- **GIVEN** an active internet connection and a configured MiniMax API key
- **WHEN** the user captures a photograph of a lunch plate containing grilled chicken and rice
- **THEN** the system SHALL return a structured list of detected food items with estimated gram amounts, calories, protein, carbohydrates, and fat per portion

#### Scenario: Fallback to local on-device analyzer
- **GIVEN** no network connectivity or no cloud API key configured
- **WHEN** the user captures a photograph of a food item
- **THEN** the system SHALL execute the on-device edge classifier, map the top result to the local Room food catalog, and present the item with a default reference portion of 100g

---

### Requirement: Interactive Portion and Macro Review (Human-in-the-Loop)
The system SHALL present an interactive review screen prior to saving scanned foods, enabling the user to adjust portion quantities in grams, remove unwanted items, add missing foods, and witness real-time recalculation of total calories and macronutrients.

#### Scenario: Adjusting portion size on review screen
- **GIVEN** a scanned item "Arroz blanco cocido" with an initial estimate of 100g (130 kcal, 2.7g protein, 28.2g carbs, 0.3g fat)
- **WHEN** the user adjusts the portion input to 150g
- **THEN** the system SHALL immediately recalculate and display 195 kcal, 4.05g protein, 42.3g carbs, and 0.45g fat for that item, updating the total meal summary instantaneously

#### Scenario: Removing a false-positive detected item
- **GIVEN** a detected list containing "Cebolla picada" and an incorrectly identified "Pan marraqueta"
- **WHEN** the user taps the delete button on the "Pan marraqueta" item
- **THEN** the system SHALL remove the item from the list and deduct its calories and macronutrients from the meal total

---

### Requirement: Health Connect Synchronization
The system SHALL synchronize confirmed meal records with Android Health Connect as `NutritionRecord` entities whenever permission is granted, mapping Chilean meal categories to corresponding Health Connect standard meal types.

#### Scenario: Exporting a confirmed lunch meal to Health Connect
- **GIVEN** that the user has granted write permission for Health Connect (`WRITE_NUTRITION`)
- **WHEN** the user confirms and saves a meal under the "Almuerzo" category with 550 kcal, 45g protein, 50g carbohydrates, and 18g fat
- **THEN** the system SHALL create and write a `NutritionRecord` in Health Connect with `mealType` set to `MEAL_TYPE_LUNCH`, matching energy and macronutrient totals, and store the resulting record identifier locally

#### Scenario: Graceful handling when Health Connect permission is denied
- **GIVEN** that the user has denied Health Connect permissions or Health Connect is unavailable
- **WHEN** the user confirms and saves a scanned meal
- **THEN** the system SHALL persist the meal successfully in the local Room database without interrupting the user experience or throwing unhandled exceptions
