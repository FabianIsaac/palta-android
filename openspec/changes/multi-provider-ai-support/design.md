# Diseño Técnico: Soporte Multiproveedor de IA

## 1. Arquitectura y Flujo Unidireccional (UDF)

La solución desacopla la capa de red del proveedor específico MiniMax, introduciendo una abstracción de configuración de proveedor que suministra dinámicamente el endpoint, el modelo y la clave de autorización:

```text
[Ajustes / SettingsScreen]
       |
       | Evento: onProviderSelected(provider), onApiKeyChanged(key)
       v
[SettingsViewModel]
       |
       v Actualiza preferencias
[UserPreferencesRepository (DataStore)]
       |
       v Expone AiConfiguration
+-------------------------------------------------------------+
| AiConfiguration:                                            |
|   - provider: AiProvider (LOCAL, NVIDIA, GEMINI, MINIMAX...) |
|   - activeEndpointUrl: String                               |
|   - activeVisionModel: String                               |
|   - activeTextModel: String                                 |
|   - activeApiKey: String                                    |
+-------------------------------------------------------------+
       |
       +----------------------------+
       |                            |
       v                            v
[OpenAiCompatibleMealAnalyzer] [OpenAiCompatibleVisionAnalyzer]
       |                            |
       | Ktor HTTP POST             | Ktor HTTP POST (Base64)
       v                            v
[Provider Endpoint: integrate.api.nvidia.com / etc.]
       |
       v JSON Response
[Limpieza de <think> tags y parseo a DetectedMealResult]
       |
       v
[FoodScanReviewViewModel -> FoodScanReviewScreen]
```

---

## 2. Modelos de Dominio y Contratos Técnicos

### 2.1. Enumeración de Proveedores de IA (`AiProvider`)
Ubicado en `domain/model/AiProvider.kt`:
```kotlin
enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultEndpointUrl: String,
    val defaultTextModel: String,
    val defaultVisionModel: String,
    val isCloud: Boolean = true
) {
    LOCAL(
        id = "local",
        displayName = "En el dispositivo (LiteRT sin conexión)",
        defaultEndpointUrl = "",
        defaultTextModel = "",
        defaultVisionModel = "",
        isCloud = false
    ),
    NVIDIA_NIM(
        id = "nvidia",
        displayName = "NVIDIA NIM (Gratis / Llama 3.2 Vision)",
        defaultEndpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
        defaultTextModel = "meta/llama-3.3-70b-instruct",
        defaultVisionModel = "meta/llama-3.2-11b-vision-instruct"
    ),
    GOOGLE_GEMINI(
        id = "gemini",
        displayName = "Google Gemini (Gratis / Flash)",
        defaultEndpointUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        defaultTextModel = "gemini-1.5-flash",
        defaultVisionModel = "gemini-1.5-flash"
    ),
    MINIMAX(
        id = "minimax",
        displayName = "MiniMax Cloud",
        defaultEndpointUrl = "https://api.minimaxi.chat/v1/chat/completions",
        defaultTextModel = "MiniMax-Text-01",
        defaultVisionModel = "MiniMax-M3"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Personalizado (Compatible con OpenAI)",
        defaultEndpointUrl = "https://api.openai.com/v1/chat/completions",
        defaultTextModel = "gpt-4o-mini",
        defaultVisionModel = "gpt-4o-mini"
    );

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id == id } ?: NVIDIA_NIM
    }
}
```

### 2.2. Objeto de Configuración Activa (`AiConfiguration`)
```kotlin
data class AiConfiguration(
    val provider: AiProvider = AiProvider.NVIDIA_NIM,
    val apiKey: String = "",
    val customEndpointUrl: String = "",
    val customTextModel: String = "",
    val customVisionModel: String = ""
) {
    val effectiveEndpointUrl: String
        get() = if (provider == AiProvider.CUSTOM && customEndpointUrl.isNotBlank()) customEndpointUrl else provider.defaultEndpointUrl

    val effectiveTextModel: String
        get() = if (provider == AiProvider.CUSTOM && customTextModel.isNotBlank()) customTextModel else provider.defaultTextModel

    val effectiveVisionModel: String
        get() = if (provider == AiProvider.CUSTOM && customVisionModel.isNotBlank()) customVisionModel else provider.defaultVisionModel
}
```

---

## 3. Capa de Red y Analizadores

### 3.1. Generalización de Clientes OpenAI-Compatible
- Tanto para texto como para visión multimodal, la especificación OpenAI utiliza:
  - Header: `Authorization: Bearer <apiKey>`
  - Header: `Content-Type: application/json`
  - Body:
    ```json
    {
      "model": "<effectiveModel>",
      "messages": [
        { "role": "system", "content": "..." },
        { "role": "user", "content": [...] }
      ],
      "temperature": 0.1
    }
    ```
- En visión, el contenido de usuario incluye la imagen codificada como `{"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}`.
- Esta estructura es soportada de forma idéntica por **NVIDIA NIM**, **MiniMax**, **Google Gemini (OpenAI Endpoint)** y servidores locales (Ollama con soporte visión / vLLM).

### 3.2. Sanitización Robusta de Respuestas
- Se mantiene e independiza la función de limpieza de JSON:
  - Extracción de bloques de código Markdown (````json ... ````).
  - Eliminación de etiquetas de razonamiento (como `<think>...</think>`) que emiten modelos como DeepSeek R1 o Qwen 2.5 Max disponibles en NVIDIA NIM.
  - Extracción de subcadena desde el primer `{` hasta el último `}`.

---

## 4. Persistencia en `UserPreferencesRepository`

Nuevas claves en DataStore:
- `AI_PROVIDER_KEY`: String ("local", "nvidia", "gemini", "minimax", "custom")
- `API_KEY_NVIDIA`: String
- `API_KEY_GEMINI`: String
- `API_KEY_MINIMAX`: String
- `API_KEY_CUSTOM`: String
- `CUSTOM_ENDPOINT_URL`: String
- `CUSTOM_TEXT_MODEL`: String
- `CUSTOM_VISION_MODEL`: String

Al seleccionar un proveedor en la UI, se consulta y edita la clave específica de dicho proveedor, garantizando que el usuario no pierda credenciales al comparar modelos.

---

## 5. Diseño de la Interfaz de Usuario (SettingsScreen)

Se actualiza la sección de IA con diseño limpio, espacioso y sin apiñamiento:
1. **Selector de Proveedor (Radio Buttons / Tarjetas seleccionables):**
   - NVIDIA NIM *(Recomendado - Gratis)*
   - Google Gemini *(Gratis con AI Studio)*
   - MiniMax Cloud
   - Personalizado (OpenAI-compatible)
   - Solo en el dispositivo (sin internet)
2. **Campo de Clave de API:**
   - Label contextual: *"Clave de API de NVIDIA"* / *"Clave de API de Gemini"*, etc.
   - Botón para ver/ocultar contraseña.
   - Mensaje de ayuda contextual con enlace o indicación clara para obtener la clave gratuita (ej. *"Obtén tu clave gratis en build.nvidia.com"*).
3. **Opciones avanzadas (acordeón expandible, visible en Personalizado o para usuarios avanzados):**
   - Endpoint URL personalizado.
   - Nombre de modelo para texto.
   - Nombre de modelo para visión.
