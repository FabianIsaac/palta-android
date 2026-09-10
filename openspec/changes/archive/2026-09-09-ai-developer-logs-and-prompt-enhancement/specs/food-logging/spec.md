## ADDED Requirements

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
