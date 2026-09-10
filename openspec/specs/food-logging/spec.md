# Food Logging Specification

## Purpose

Permite el registro, escaneo inteligente con IA, edición interactiva y catalogación local de alimentos y comidas consumidas por el usuario, calculando macronutrientes y calorías con precisión y sincronizando con Health Connect.

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
The system SHALL allow users to log food entries categorized under meal slots adapted to Chilean conventions (Desayuno, Almuerzo, Once / Cena, Colaciones) for any selected date, originating either from manual entry or AI-assisted image scanning.

#### Scenario: Adding entries via AI review confirmation
- **GIVEN** a confirmed meal scan containing 150g of "Pechuga de pollo a la plancha" (247.5 kcal, 46.5g protein, 0g carbs, 5.4g fat) and 100g of "Palta hass" (160 kcal, 2g protein, 9g carbs, 15g fat)
- **WHEN** the user selects the category "Almuerzo" and presses "Confirmar y Guardar"
- **THEN** the system SHALL persist the meal entries in the local database associated with the active date and the "Almuerzo" category, updating daily nutritional totals immediately

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

---

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

---

### Requirement: Smart Time-Based Meal Category Detection
The system SHALL automatically infer the appropriate Chilean meal category (Desayuno, Almuerzo, Once / Cena, Colaciones) based on the user's current local time and their configured meal time windows, applying it as the default selection whenever a new meal is logged.

#### Scenario: Detecting lunch during midday
- **GIVEN** meal time windows where Almuerzo is configured between 11:30 and 16:00
- **WHEN** the user opens the camera, text entry sheet, or manual entry at 13:45
- **THEN** the system SHALL pre-select "Almuerzo" as the meal category and present a clear option to switch categories if desired

#### Scenario: Detecting breakfast in the morning
- **GIVEN** meal time windows where Desayuno is configured between 06:00 and 11:30
- **WHEN** the user logs food at 08:30
- **THEN** the system SHALL pre-select "Desayuno" as the meal category

#### Scenario: Detecting once / cena in the evening
- **GIVEN** meal time windows where Once / Cena is configured between 16:00 and 22:00
- **WHEN** the user logs food at 19:30
- **THEN** the system SHALL pre-select "Once / Cena" as the meal category

---

### Requirement: Configurable Meal Time Windows
The system SHALL provide settings allowing the user to customize the start and end times for Desayuno, Almuerzo, and Once / Cena, persisting these preferences locally across app restarts.

#### Scenario: Updating lunch time window
- **GIVEN** the settings screen
- **WHEN** the user changes the start time of Almuerzo to 12:30 and saves
- **THEN** the system SHALL persist the updated time window and use 12:30 for future automated meal detection

---

### Requirement: Responsive Review Screen Actions Layout
The system SHALL render the meal review screen actions (including adding food items and describing with AI) with dedicated layout spacing that prevents button compression, awkward line breaking, or visual truncation across all screen densities.

#### Scenario: Adding items without label truncation
- **GIVEN** the meal review screen with detected food items
- **WHEN** the actions section is displayed
- **THEN** the "Agregar alimento" button SHALL remain fully legible on a single line with proper padding and icon alignment without clipping or circular deformation

---

### Requirement: Immediate Feedback on Text Description Analysis
The system SHALL transition immediately to a visible loading state upon submitting a food description in natural language, displaying progress animation and clear status feedback until parsing finishes.

#### Scenario: Submitting meal text description
- **GIVEN** the quick text entry sheet
- **WHEN** the user taps the interpret button with a valid description
- **THEN** the system SHALL dismiss the sheet and immediately show the full review loading indicator displaying "Interpretando tu comida con IA..." while the analyzer processes the request

---

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

---

### Requirement: Technical AI Diagnostic and Error Inspection
The system SHALL capture detailed diagnostic metadata for every AI text and image request—including timestamp, provider name, target endpoint, HTTP status code, response duration in milliseconds, raw response payload or error body, and exception details. The system SHALL make this diagnostic information accessible to the developer or advanced user through a direct inspection action on AI connection error cards and a dedicated log viewer with connectivity testing in settings.

#### Scenario: Inspecting failed AI connection details from review screen
- **GIVEN** a natural language meal analysis attempt that fails due to an HTTP error (e.g. 401 Unauthorized or 429 Too Many Requests)
- **WHEN** the user or developer taps the "Ver detalle técnico" option on the AI connection error card
- **THEN** the system SHALL display a modal dialog detailing the configured provider, model, HTTP status code, error body, and a button to copy the diagnostic summary to the clipboard

#### Scenario: Viewing recent AI call history in settings
- **GIVEN** the settings screen
- **WHEN** the user navigates to the "Diagnóstico y Logs de IA" section
- **THEN** the system SHALL list the recent AI interactions with their status (Success, Error, or Local Fallback), duration, provider, and timestamp, allowing the user to expand each entry to inspect the full request payload and raw response

#### Scenario: Testing AI provider connectivity in settings
- **GIVEN** an active internet connection and a configured AI provider API key
- **WHEN** the user taps "Probar conexión" in the AI settings panel
- **THEN** the system SHALL execute a lightweight ping request against the provider's completion endpoint and display a clear success confirmation or the exact HTTP failure reason

---

### Requirement: Atomic Decomposition of Sauces and Pastes in AI Prompt
The system SHALL mandate that remote AI nutritional analyzers decompose all composite sandwich pastes, mixed fillings, and preparations containing sauces, oils, or dressings (such as "ave mayo", "atún mayo", "huevo con mayo", or dressed salads) into separate, atomic ingredient items. Salsas and dressings with high caloric density (specifically mayonnaise and oils) MUST NOT be omitted or fused into the base protein item.

#### Scenario: Interpreting chicken with mayonnaise in natural language
- **GIVEN** a natural language meal description "marraqueta con ave mayo y café negro"
- **WHEN** the remote AI analyzer processes the text
- **THEN** the system SHALL return distinct items for "Pan marraqueta" (or equivalent bread), "Pechuga de pollo" (or shredded chicken), "Mayonesa" (with realistic gram portion ~15g-25g and calories corresponding to ~680 kcal/100g), and "Café negro"

#### Scenario: Interpreting salads or bowls with heavy dressings
- **GIVEN** a text description "ensalada de atún con mayonesa y choclo"
- **WHEN** the remote AI analyzer processes the input
- **THEN** the system SHALL produce separate items for the base vegetables, the tuna protein, and the mayonnaise dressing as an independent entry


