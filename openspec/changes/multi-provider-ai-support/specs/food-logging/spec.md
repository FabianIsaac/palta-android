# Food Logging Specification Delta: Soporte Multiproveedor de IA

## Requirements

### MODIFIED Requirement: Food Image Analysis via AI
The system SHALL provide an image analysis mechanism capable of identifying food items and estimating portions from camera captures or selected gallery images using either on-device edge models or any configured cloud AI provider that adheres to the standard OpenAI chat completions specification (including NVIDIA NIM, Google Gemini, MiniMax, or user-defined custom endpoints).

#### Scenario: Successful image analysis with NVIDIA NIM provider
- **GIVEN** an active internet connection and NVIDIA NIM selected as the active AI provider with a valid API key
- **WHEN** the user captures a photograph of a lunch plate containing "Cazuela de ave"
- **THEN** the system SHALL send the image request using the configured vision model (`meta/llama-3.2-11b-vision-instruct`), parse the structured JSON response, and present the recognized food item with estimated portion in grams (~350 g, ~280 kcal) on the review screen.

#### Scenario: Switching AI providers preserves independent credentials
- **GIVEN** that the user previously entered an API key for MiniMax and switches the active provider to NVIDIA NIM in Ajustes
- **WHEN** the user enters their NVIDIA API key and subsequently switches back to MiniMax
- **THEN** the system SHALL restore the previously saved MiniMax API key without requiring the user to re-enter it.

#### Scenario: Fallback to local on-device classifier when cloud provider fails
- **GIVEN** that an active cloud AI provider is configured but the remote request fails due to network timeout or HTTP error
- **WHEN** the image analysis is executed
- **THEN** the system SHALL fall back automatically to the local LiteRT on-device classifier or notify the user with a friendly Chilean Spanish message without crashing the application.

---

### MODIFIED Requirement: Natural Language Meal Entry with AI
The system SHALL allow users to log meals by describing them in everyday Chilean natural language, utilizing the configured active cloud AI provider (NVIDIA NIM, Google Gemini, MiniMax, or Custom) to parse colloquial descriptions into structured food items and macronutrient calculations.

#### Scenario: Parsing natural language with NVIDIA NIM text model
- **GIVEN** NVIDIA NIM selected with model `meta/llama-3.3-70b-instruct`
- **WHEN** the user inputs "Dos huevos revueltos con una marraqueta y un café negro"
- **THEN** the system SHALL execute the text analysis request, strip any chain-of-thought tags (such as `<think>...</think>`), parse the JSON response into discrete items ("Huevos revueltos" ~100g, "Marraqueta" ~100g, "Café negro" ~200ml), and calculate the corresponding total calories and macronutrients for one-tap review.

#### Scenario: Missing API key prompt
- **GIVEN** a cloud AI provider selected without an API key configured
- **WHEN** the user attempts to log a meal via natural language description or camera capture
- **THEN** the system SHALL inform the user with an actionable message in Chilean Spanish: "Ingresa tu clave de API en Ajustes para usar este proveedor de IA."
