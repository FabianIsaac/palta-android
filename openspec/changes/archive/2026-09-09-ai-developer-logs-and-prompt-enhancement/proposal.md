# Proposal: Diagnóstico de IA para Desarrolladores y Refuerzo de Prompt para Salsas/Pastas

## Why

Al procesar descripciones de comidas en lenguaje natural o imágenes, los fallos de conexión o respuestas erróneas de los proveedores de IA (NVIDIA NIM, Google Gemini, MiniMax, Custom) son silenciosamente absorbidos por el fallback local sin dejar registro técnico ni traza accesible para el desarrollador. Esto impide saber si un error se debe a cuotas agotadas (HTTP 429), claves no autorizadas (HTTP 401), timeouts o fallos de parseo JSON. 

Adicionalmente, las preparaciones tradicionales chilenas que combinan proteínas con salsas o aderezos calóricos (como "ave mayo", "atún mayo", "huevo con mayo" o ensaladas con aliños) carecen de una regla estricta en el prompt del sistema, provocando que la IA omita la mayonesa o aderezos grasos, subestimando drásticamente el cálculo de calorías y macronutrientes.

## What Changes

- **Diagnóstico y Observabilidad de IA (Logs para Desarrollador):**
  - Creación de un gestor de logs en memoria (`AiDebugLogManager`) que captura cada interacción con el LLM: timestamp, tipo de análisis (texto/visión), proveedor, modelo, endpoint, código de respuesta HTTP, duración en milisegundos, payload enviado, respuesta cruda (JSON o error body) y mensaje de excepción.
  - Trazas estructuradas en `Logcat` con el tag `AiMealAnalyzer`.
  - Acción *"Ver detalle técnico"* en la tarjeta de error de IA (`FoodScanReviewScreen`) que despliega un diálogo con el código de error, proveedor, motivo de falla y botón *"Copiar al portapapeles"*.
  - Sección de *"Diagnóstico y Logs de IA"* en la pantalla de Ajustes (`SettingsScreen`) para revisar el historial reciente de llamadas y ejecutar una prueba de conexión en vivo (*Ping*) con el proveedor configurado.
- **Refuerzo del System Prompt para Pastas y Salsas Chilenas:**
  - Inclusión de una directiva estricta y universal en `OpenAiCompatibleMealAnalyzer` para obligar a la IA a desglosar siempre en ítems atómicos las preparaciones que contengan pastas, aderezos o salsas (ej. "ave mayo" -> Pechuga de pollo desmenuzada + Mayonesa con su gramaje y calorías reales).

## Capabilities

### New Capabilities
<!-- No se introducen nuevas capacidades de nivel superior; se extiende y robustece el logging de comidas existente. -->

### Modified Capabilities
- `food-logging`: Se añaden requisitos de observabilidad técnica y diagnóstico ante fallos de IA, junto con la regla obligatoria de descomposición atómica de preparaciones compuestas y pastas con salsas/grasas.

## Impact

- **Capa de Dominio y Datos:** `AiDebugLogManager` / `AiCallLogEntry` para recolectar métricas de red y diagnóstico en `OpenAiCompatibleMealAnalyzer`.
- **Capa de Presentación:** Actualización de `FoodScanReviewScreen`, `FoodScanReviewViewModel`, `SettingsScreen` y `SettingsViewModel` para exponer datos de diagnóstico y prueba de conexión.
- **Sin impacto en base de datos:** Los logs de depuración se mantienen en memoria (ring buffer) para no generar sobrecarga en Room Database ni requerir migraciones de esquema.
- **Nutrición:** Corrección de la subestimación calórica en preparaciones con salsas y mayonesa.
