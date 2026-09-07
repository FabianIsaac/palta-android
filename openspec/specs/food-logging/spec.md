# Food Logging Specification

## Requirements

### Requirement: Food Catalog and Custom Items
The system SHALL maintain a local catalog of common food items and allow users to create, view, edit, and delete custom food items with nutritional data per standard reference portion (100 grams or 100 ml).

#### Scenario: Creating a custom food item
- **GIVEN** the custom food creation screen
- **WHEN** the user enters name "Pechuga de Pollo cocida", serving size 100g, Calories 165 kcal, Protein 31g, Carbs 0g, and Fat 3.6g
- **THEN** the system SHALL validate the macronutrient total consistency and persist the food item in the local database

#### Scenario: Searching existing food items
- **GIVEN** an existing database with food items
- **WHEN** the user types "avena" into the search bar
- **THEN** the system SHALL display matching results ordered by relevance and frequent usage

---

### Requirement: Meal Entry Logging
The system SHALL allow users to log food entries categorized under meal slots adapted to Chilean conventions (Desayuno, Almuerzo, Once / Cena, Colaciones) for any selected date.

#### Scenario: Adding a food item to a meal
- **GIVEN** a food item ("Palta hass" with 160 kcal, 2g protein, 9g carbs, 15g fat per 100g)
- **WHEN** the user logs a serving of 80g into the "Once / Cena" category
- **THEN** the system SHALL calculate 128 kcal, 1.6g protein, 7.2g carbs, and 12g fat for that entry and associate it with the active date and "Once / Cena" category

#### Scenario: Modifying entry portion size
- **GIVEN** an existing logged entry of 150g
- **WHEN** the user edits the portion size to 200g
- **THEN** the system SHALL recalculate the entry's calories and macronutrients proportionally and update the meal total immediately

#### Scenario: Deleting a logged entry
- **GIVEN** a meal entry in the daily food log
- **WHEN** the user swipes to delete or confirms deletion of the entry
- **THEN** the system SHALL remove the entry from the database and recalculate the meal and daily totals immediately
