## MODIFIED Requirements

### Requirement: Technical AI Diagnostic and Error Inspection
The system SHALL capture detailed diagnostic metadata for every AI text and image request—including timestamp, provider name, target endpoint, HTTP status code, response duration in milliseconds, raw response payload or error body, and exception details. The system SHALL make this diagnostic information accessible to the developer or advanced user through a direct inspection action on AI connection error cards and a dedicated log viewer with connectivity testing in settings. On the AI connection error card, the system SHALL display diagnostic actions ("Ver detalle técnico") and user recovery actions ("Editar texto" and "Reintentar") in separate hierarchical rows to prevent button squeezing, text fragmentation, or vertical button distortion across all standard mobile display widths.

#### Scenario: Inspecting failed AI connection details from review screen
- **GIVEN** a natural language meal analysis attempt that fails due to an HTTP error (e.g. 401 Unauthorized, 429 Too Many Requests, or network timeout)
- **WHEN** the user or developer views the AI connection error card on the food review screen
- **THEN** the system SHALL present "Ver detalle técnico" in an upper action area and present "Editar texto" and "Reintentar" in a dedicated lower action row with sufficient horizontal space
- **WHEN** the user taps "Ver detalle técnico"
- **THEN** the system SHALL display a modal dialog detailing the configured provider, model, HTTP status code, duration, endpoint, error body, and a button to copy the diagnostic summary to the clipboard

#### Scenario: Viewing recent AI call history in settings
- **GIVEN** the settings screen
- **WHEN** the user navigates to the "Diagnóstico y Logs de IA" section
- **THEN** the system SHALL list the recent AI interactions with their status (Success, Error, or Local Fallback), duration, provider, and timestamp, allowing the user to expand each entry to inspect the full request payload and raw response

#### Scenario: Testing AI provider connectivity in settings
- **GIVEN** an active internet connection and a configured AI provider API key
- **WHEN** the user taps "Probar conexión" in the AI settings panel
- **THEN** the system SHALL execute a lightweight ping request against the provider's completion endpoint and display a clear success confirmation or the exact HTTP failure reason

## ADDED Requirements

### Requirement: Resilient HTTP Timeout and High-Speed MiniMax Text Model
The system SHALL configure an explicit HTTP client timeout policy of at least 60 seconds (covering request timeout and socket timeout) for all outbound AI completions to prevent false connection cancellations during heavy model computation. Furthermore, the system SHALL designate `MiniMax-M2.7-highspeed` as the default text decomposition model for the MiniMax provider while retaining `https://api.minimaxi.chat/v1/chat/completions` as the default endpoint.

#### Scenario: Executing natural language meal analysis with MiniMax
- **GIVEN** an active internet connection and the MiniMax provider selected without custom text model overrides
- **WHEN** the user submits a natural language food description
- **THEN** the system SHALL issue the OpenAI-compatible completion request targeting model `MiniMax-M2.7-highspeed` with a 60-second socket timeout
