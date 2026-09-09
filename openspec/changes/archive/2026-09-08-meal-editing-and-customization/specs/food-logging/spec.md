# Food Logging Specification Delta

## Requirements

### Requirement: Interactive Item Name Editing
The system SHALL allow users to edit the display name of any food item both during initial scan review and when modifying an existing logged meal, immediately updating the item title and persisting the customized name.

#### Scenario: Renaming an item on the review screen
- **GIVEN** a detected meal item with default name "Pan blanco de molde"
- **WHEN** the user taps to edit the item name, enters "Media marraqueta tostada", and confirms
- **THEN** the system SHALL update the item name to "Media marraqueta tostada" in the review screen without altering its nutritional density values

#### Scenario: Renaming an item in an existing saved meal
- **GIVEN** a saved meal containing an item named "Carne con verduras"
- **WHEN** the user opens the meal in the edit screen, renames the item to "Charquicán casero", and saves the changes
- **THEN** the system SHALL persist the updated name in the local database and retain the updated title across all views

---

### Requirement: Adding Ingredients to Existing Meals
The system SHALL allow users to add missing food ingredients to an existing saved meal, selecting items from the local food catalog or entering custom items, and SHALL immediately recalculate the meal's total calories and macronutrients.

#### Scenario: Adding an ingredient from the local food catalog
- **GIVEN** a saved "Almuerzo" meal containing 150g of "Pechuga de pollo" and 200g of "Arroz blanco"
- **WHEN** the user taps "+ Agregar ingrediente", searches for "Palta hass", selects a portion of 50g, and adds it to the meal
- **THEN** the system SHALL append the "Palta hass" (50g) to the meal items list, recalculate the consolidated nutritional totals, and update the display in real time

#### Scenario: Saving a modified meal with newly added ingredients
- **GIVEN** a meal with newly added ingredients in the edit meal screen
- **WHEN** the user taps "Guardar cambios"
- **THEN** the system SHALL update the meal entry and all its associated items in the local database and update any synchronized Health Connect nutrition record
