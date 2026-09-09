# Propuesta: Soporte Multiproveedor de Inteligencia Artificial (NVIDIA NIM, Gemini, MiniMax y Custom)

## 1. Problema y Justificación

Actualmente, las capacidades de inteligencia artificial de la aplicación (registro de comidas en lenguaje natural, análisis visual de fotos de platos y estimación de suplementos deportivos) se encuentran acopladas de forma exclusiva al proveedor **MiniMax** (`https://api.minimaxi.chat/v1/chat/completions`).

Esto introduce las siguientes limitaciones para los usuarios:
1. **Falta de alternativas gratuitas accesibles:** MiniMax requiere registro y saldo/créditos específicos. Los usuarios necesitan poder utilizar proveedores con niveles gratuitos (free tiers) generosos y de alta calidad, como **NVIDIA NIM** (que entrega 1.000 créditos gratuitos para desarrolladores y acceso a modelos multimodales punteros como `meta/llama-3.2-11b-vision-instruct`) o **Google Gemini** (a través de su endpoint OpenAI-compatible gratuito en Google AI Studio).
2. **Dependencia de un único punto de falla (Vendor Lock-in):** Si el servicio de MiniMax presenta intermitencias, saturación o latencias elevadas, los usuarios quedan sin respaldo en la nube, viéndose limitados al clasificador local en el dispositivo.
3. **Rigidez en la configuración de modelos:** No existe la posibilidad de elegir qué modelo de texto o de visión utilizar, ni de configurar un endpoint local o privado compatible con OpenAI (por ejemplo, servidores Ollama locales, LM Studio, vLLM o proxies compatibles).

Dado que la gran mayoría de proveedores modernos (NVIDIA NIM, OpenAI, Groq, OpenRouter, Google Gemini OpenAI-compatible, DeepSeek y servidores locales) implementan la especificación estándar de **OpenAI Chat Completions API**, desacoplar el cliente HTTP y parametrizar el proveedor, endpoint, modelo y credenciales permite una solución robusta, flexible y de bajo costo de mantenimiento.

---

## 2. Alcance (In-Scope)

- **Arquitectura de red unificada y desacoplada:**
  - Transformar los analizadores remotos (`RemoteNaturalLanguageMealAnalyzer`, `MiniMaxVisionAnalyzer` y `RemoteSupplementNutritionAnalyzer`) en implementaciones universales compatibles con la especificación OpenAI Chat Completions (`/v1/chat/completions`).
  - Capacidad de procesar texto y visión multimodal (imágenes codificadas en Base64 con data URL o partes de contenido estándar).
  - Soporte de limpieza de etiquetas de razonamiento (como `<think>...</think>`) para modelos de razonamiento (DeepSeek R1, Qwen, etc.).

- **Catálogo de Presets de Proveedores:**
  - **NVIDIA NIM (Recomendado / Capa gratuita):**
    - Endpoint por defecto: `https://integrate.api.nvidia.com/v1/chat/completions`
    - Modelo de visión: `meta/llama-3.2-11b-vision-instruct`
    - Modelo de texto: `meta/llama-3.3-70b-instruct`
    - Autenticación: Clave API de NVIDIA (`nvapi-...`).
  - **Google Gemini (OpenAI Endpoint / Capa gratuita):**
    - Endpoint por defecto: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
    - Modelo unificado: `gemini-1.5-flash` o `gemini-2.0-flash`
    - Autenticación: Clave API de Google AI Studio.
  - **MiniMax (Proveedor actual de referencia):**
    - Endpoint por defecto: `https://api.minimaxi.chat/v1/chat/completions`
    - Modelo de texto: `MiniMax-Text-01`
    - Modelo de visión: `MiniMax-M3`
  - **Personalizado / Compatible con OpenAI:**
    - Permite al usuario definir libremente la URL del endpoint, modelo de texto, modelo de visión y clave de autorización (ideal para OpenRouter, Groq, servidores locales Ollama, etc.).

- **Persistencia de Preferencias (DataStore):**
  - Almacenar el proveedor activo seleccionado por el usuario.
  - Almacenar credenciales de API de forma independiente por proveedor (para que al alternar entre NVIDIA y MiniMax no se pierdan las claves ingresadas previamente).
  - Almacenar configuraciones personalizadas (URL base y modelos elegidos).

- **Interfaz de Ajustes Renovada (SettingsScreen):**
  - Sección *"Motor de Inteligencia Artificial"* rediseñada y limpia:
    - Selector amigable de proveedor (Local LiteRT, NVIDIA NIM, Google Gemini, MiniMax, Personalizado).
    - Campo seguro para ingresar y alternar visibilidad de la API Key del proveedor activo, con link de ayuda o indicación de cómo obtenerla gratis.
    - Opciones avanzadas colapsables para configurar o afinar nombres de modelos y endpoint URL si el usuario lo requiere.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Soporte nativo para el protocolo propietario de Anthropic Claude (`/v1/messages` con headers `x-api-key`), dado que Claude no ofrece capa gratuita y puede ser utilizado mediante proveedores compatibles como OpenRouter en el modo Personalizado.
- Modificación del modelo local on-device (`LocalLiteRtVisionAnalyzer`), el cual permanece como respaldo local offline sin cambios.
- Modificación de la base de datos Room (las entidades `MealEntryEntity` y `FoodItemEntity` se mantienen intactas).

---

## 4. Impacto en Base de Datos, UI y Métricas Nutricionales

- **Base de datos (Room):**
  - No requiere cambios ni migraciones en SQLite/Room.
- **Preferencias del usuario (DataStore):**
  - Se agregan claves en `UserPreferencesRepository`:
    - `AI_PROVIDER`: Identificador del proveedor activo (`LOCAL`, `NVIDIA`, `GEMINI`, `MINIMAX`, `CUSTOM`).
    - Claves independientes por proveedor: `NVIDIA_API_KEY`, `GEMINI_API_KEY`, `MINIMAX_API_KEY`, `CUSTOM_API_KEY`.
    - Claves opcionales de personalización: `CUSTOM_ENDPOINT_URL`, `CUSTOM_TEXT_MODEL`, `CUSTOM_VISION_MODEL`.
- **Componentes de UI y Estados de Compose:**
  - `SettingsScreen` y `SettingsViewModel`: Gestión del estado del proveedor seleccionado, API keys y parámetros avanzados.
  - `FoodScanReviewScreen`: Sin cambios en su contrato público (recibe `DetectedMealResult` inmutable).
- **Fórmulas y Métricas Nutricionales:**
  - No se alteran las fórmulas metabólicas (Mifflin-St Jeor / Harris-Benedict) ni el cálculo de calorías (4-4-9 kcal/g). El parseo y cálculo de equivalencias de porciones chilenas se mantiene estricto y determinista.
