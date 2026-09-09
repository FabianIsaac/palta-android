# Food Logging Specification Delta

## Requirements

### ADDED Requirement: Natural Language Meal Entry with AI
The system SHALL allow users to log meals by describing them in everyday natural language without requiring knowledge of macronutrients or raw gram weights, automatically parsing the text into discrete food items with estimated portions and nutritional calculations.

#### Scenario: Logging breakfast with coffee and bread in natural language
- **GIVEN** the natural language meal entry interface
- **WHEN** the user inputs "Una taza de café negro con 1 cucharadita de azúcar y una marraqueta con palta"
- **THEN** the system SHALL parse the input using the AI nutritional assistant into three distinct items: "Café negro" (~200 ml, 2 kcal), "Azúcar blanca" (~5 g, 20 kcal), and "Marraqueta con palta" (~150 g, 320 kcal), computing total calories and macronutrients automatically without manual formula calculations by the user.

#### Scenario: Handling ambiguous or colloquial descriptions
- **GIVEN** the natural language meal entry interface
- **WHEN** the user enters a colloquial term such as "Un té con leche y dos galletas de agua"
- **THEN** the system SHALL interpret standard Chilean household serving sizes (1 taza de té con leche ~60 kcal, 2 galletas ~45 kcal) and present them directly on the review screen for one-tap confirmation.

---

### ADDED Requirement: Household Portion Representation
The system SHALL present serving quantities to the user in familiar household units (tazas, tazones, cucharadas, unidades, rebanadas) alongside metric equivalents, allowing portion adjustments without forcing raw gram calculations.

#### Scenario: Adjusting portion using household units
- **GIVEN** a detected entry of "Café cortado" with an initial serving of "1 taza (200 ml)"
- **WHEN** the user selects the portion and changes it to "1 tazón (350 ml)"
- **THEN** the system SHALL scale the calories and macronutrients proportionally to 350 ml and update the meal totals instantaneously.

---

### MODIFIED Requirement: Food Image Analysis via AI
The system SHALL provide an image analysis mechanism capable of identifying solid and liquid food items (including coffee, tea, and beverages) and estimating portions from camera captures. If no cloud AI is available and the on-device model cannot provide a high-confidence match, the system SHALL inform the user transparently instead of returning false deterministic results.

#### Scenario: Analyzing a cup of coffee with cloud vision AI
- **GIVEN** an active internet connection and configured cloud vision provider
- **WHEN** the user captures a photograph of a transparent mug containing black coffee
- **THEN** the system SHALL recognize the item as "Café negro", estimate a standard serving of 200-250 ml, calculate approximately 2-5 kcal with 0g fat, and present the result on the review screen.

#### Scenario: Honest fallback when local classifier has low confidence
- **GIVEN** that the user captures a photo while offline or without cloud AI configured
- **WHEN** the local image classifier cannot identify the food item with confidence >= 0.60
- **THEN** the system SHALL notify the user with a friendly prompt ("No pudimos identificar con certeza tu comida. Puedes buscarla por nombre o describirla") rather than producing fabricated items.

---

### MODIFIED Requirement: Food Catalog and Custom Items
The system SHALL include standard beverage items and condiments in the default local catalog, including black coffee, coffee with milk, tea varieties, sugar, and sweeteners, with predefined household serving sizes.

#### Scenario: Searching for coffee in local catalog
- **GIVEN** the local food catalog
- **WHEN** the user searches for "café"
- **THEN** the system SHALL return "Café negro" (2 kcal/100ml) and "Café con leche" (45 kcal/100ml) with default household portion suggestions of 200 ml.
