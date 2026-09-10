## Context

Actualmente, las llamadas remotas a los proveedores LLM (`OpenAiCompatibleMealAnalyzer`, `OpenAiCompatibleVisionAnalyzer`, `OpenAiCompatibleSupplementAnalyzer`) no registran telemetría técnica ni exponen las causas de fallo cuando se activa el fallback local de emergencia. Si la API devuelve `HTTP 401`, `HTTP 429` o un error de parseo JSON, la excepción se descarta y el usuario/desarrollador solo ve un genérico "No se pudo conectar con la IA".

En paralelo, el system prompt actual para interpretar comidas en lenguaje natural carece de directivas sobre pastas compuestas chilenas (como "ave mayo", "atún mayo", "huevo con mayo"), lo que provoca que los modelos omitan la mayonesa y asuman únicamente la proteína base, generando un error nutricional significativo.

Ver `proposal.md` para justificación completa y `specs/food-logging/spec.md` para especificaciones normativas.

## Goals / Non-Goals

**Goals:**
- Proporcionar observabilidad completa y en tiempo real de cada interacción con la IA (prompt enviado, endpoint, HTTP status, duración, payload crudo devuelto o excepción).
- Permitir inspección técnica con 1 toque desde la tarjeta de error en `FoodScanReviewScreen`.
- Añadir sección de diagnóstico y prueba de conexión en vivo (*Ping*) en `SettingsScreen`.
- Asegurar trazas legibles en Logcat con el tag `AiMealAnalyzer`.
- Fortalecer el prompt de sistema para garantizar la separación de proteínas y salsas/grasas (especialmente mayonesa) en cualquier preparación compuesta.

**Non-Goals:**
- No se creará persistencia en base de datos Room para los logs de depuración (se mantendrán en un búfer circular en memoria para evitar overhead y migraciones).
- No se modifica el catálogo offline en esta fase (postergado para una fase futura según decisión del usuario).

## Decisions

### 1. Registro de Diagnóstico en Memoria (`AiDebugLogManager`)
- **Decisión:** Implementar un gestor de logs singleton/inyectable que mantiene un búfer circular de las últimas 50 llamadas en memoria expuesto mediante `StateFlow<List<AiCallLogEntry>>`.
- **Alternativas descartadas:**
  - *Guardar en Room:* Descartado para no saturar SQLite con payloads de imágenes o textos largos ni requerir migraciones de base de datos.
  - *Solo Logcat:* Descartado porque el usuario/desarrollador en el dispositivo físico no siempre tiene un cable USB conectado a Android Studio.
- **Estructura del modelo `AiCallLogEntry`:**
  - `id: String` (UUID)
  - `timestamp: Long` (Epoch millis)
  - `callType: AiCallType` (`MEAL_TEXT`, `MEAL_IMAGE`, `SUPPLEMENT_TEXT`, `CONNECTIVITY_TEST`)
  - `provider: AiProvider`
  - `model: String`
  - `endpointUrl: String`
  - `promptSummary: String` (o descripción del texto)
  - `httpStatus: Int?` (ej. 200, 401, 429, 500, o null si fue timeout de socket)
  - `durationMs: Long`
  - `isSuccess: Boolean`
  - `rawResponse: String?` (cuerpo de la respuesta JSON o cuerpo de error)
  - `errorMessage: String?` (mensaje de excepción formateado)

### 2. Integración en los Analizadores Remotos
- En `OpenAiCompatibleMealAnalyzer.kt`, se intercepta el inicio, fin y captura de excepciones de cada llamada HTTP.
- Toda llamada registra en `AiDebugLogManager` y emite con `android.util.Log.i(...)` o `android.util.Log.e(...)`.
- Ante fallo remoto, la excepción resultante contiene un objeto estructurado `AiTechnicalDetails` para que `FoodScanReviewViewModel` pueda propagarlo al `FoodScanReviewUiState`.

### 3. Inspección en UI (Human-in-the-Loop Developer Tools)
- **En `FoodScanReviewScreen`:**
  - En la tarjeta roja de error (`title_error_ai_connection`), se añade un botón sutil `"Ver detalle técnico"`.
  - Al presionarlo, abre `AiDiagnosticDetailsDialog` mostrando:
    - Proveedor y Modelo
    - Código HTTP y URL del endpoint
    - Causa o error retornado por la API
    - Botón para copiar el log completo al portapapeles.
- **En `SettingsScreen`:**
  - Nueva tarjeta dentro de la sección de IA: *"Diagnóstico y Conexión"*.
  - Botón *"Probar conexión"* que envía un mensaje mínimo de prueba ("ping") al endpoint configurado y muestra en un Snackbar/Diálogo el resultado inmediato con código HTTP y tiempo de respuesta.
  - Botón *"Ver registro de llamadas"* que despliega un BottomSheet con la lista cronológica de llamadas recientes.

### 4. Refuerzo Universal del System Prompt para Pastas y Salsas
- **Decisión:** Añadir una regla universal al system prompt de `OpenAiCompatibleMealAnalyzer`:
  ```text
  REGLA GENERAL PARA PASTAS, RELLENOS Y MEZCLAS CON SALSAS:
  Si el usuario menciona una preparación compuesta o pasta donde se mezcla una proteína o vegetal con una salsa o aderezo calórico (ej: ave mayo, atún mayo, huevo con mayo, papas mayo, sándwiches con salsa, ensaladas con aliño pesado):
  1. NUNCA combines la salsa con la proteína en un solo ítem.
  2. NUNCA omitas la salsa: la mayonesa y los aderezos grasos aportan alta densidad calórica (~680 kcal/100g).
  3. DEBES generar SIEMPRE dos ítems independientes en detected_items:
     a) La proteína o base: ej. "Pechuga de pollo cocida desmenuzada" (~60g - 80g).
     b) La salsa o grasa: ej. "Mayonesa" (~15g - 25g, 1 a 2 cucharadas soperas, ~680 kcal/100g, 75g grasa).
  ```

## Risks / Trade-offs

- **[Riesgo] Consumo de memoria por payloads de imágenes:** Las llamadas con imágenes Base64 pueden saturar la memoria RAM si se guardan completas en el log.
  - **Mitigación:** En `AiCallLogEntry`, el payload de entrada para imágenes se trunca o almacena solo como metadato (`"[Imagen JPEG: X bytes]"`), guardando solo el JSON de respuesta devuelto por el modelo.
- **[Riesgo] Exposición accidental de API Keys en logs:** Si la URL o el cuerpo contiene la clave secreta.
  - **Mitigación:** Se ofuscan cabeceras de autorización (`Bearer nvapi-***`) y parámetros de clave antes de almacenar en `AiCallLogEntry`.
