# Food Logging Specification (Delta)

## ADDED Requirements

### Requirement: Meal Modification and Item Adjustment
The system SHALL allow users to modify any previously logged meal entry (regardless of the time elapsed or current hour of the day) by adjusting portion weights in grams, removing items, or adding new foods, recalculating the meal's total calories and macronutrients immediately.

#### Scenario: Editing breakfast portion in the evening
- **GIVEN** a breakfast meal logged at 08:30 containing 100g of "Palta hass" (160 kcal, 2g protein, 9g carbs, 15g fat) and 100g of "Pan marraqueta" (270 kcal, 9g protein, 56g carbs, 1g fat)
- **WHEN** the user opens the meal at 20:00 and updates the "Palta hass" portion to 50g
- **THEN** the system SHALL recalculate the palta nutrition to 80 kcal, 1g protein, 4.5g carbs, 7.5g fat, update the meal total to 350 kcal, and persist the changes in the database

#### Scenario: Removing an item from a logged meal
- **GIVEN** a meal containing two logged items
- **WHEN** the user removes one of the items and confirms the change
- **THEN** the system SHALL delete the item from the meal, deduct its macronutrients and calories, and save the updated meal

#### Scenario: Deleting an entire logged meal
- **GIVEN** an existing logged meal
- **WHEN** the user confirms deletion of the meal
- **THEN** the system SHALL remove the meal and all its associated food items from the local database and any linked external services

---

### Requirement: Synchronized Health Connect Update and Deletion
The system SHALL propagate modifications and deletions of meal entries to Android Health Connect whenever a corresponding `healthConnectRecordId` exists and permissions are active.

#### Scenario: Propagating meal update to Health Connect
- **GIVEN** a meal entry with an existing `healthConnectRecordId` of "hc_rec_123" and active `WRITE_NUTRITION` permission
- **WHEN** the user modifies the meal items and taps "Guardar cambios"
- **THEN** the system SHALL update the existing `NutritionRecord` with id "hc_rec_123" in Health Connect with the new energy and macronutrient totals

#### Scenario: Propagating meal deletion to Health Connect
- **GIVEN** a meal entry with an existing `healthConnectRecordId` of "hc_rec_456"
- **WHEN** the user deletes the meal entry
- **THEN** the system SHALL delete the record with id "hc_rec_456" from Health Connect
