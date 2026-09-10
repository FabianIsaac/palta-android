# Propuesta: Actualización de Modelo MiniMax de Alta Velocidad, Extensión de Timeouts HTTP y Corrección de UI en Tarjeta de Error

## Why

Al registrar alimentos con lenguaje natural, la tarjeta de error de IA en `FoodScanReviewScreen` presenta un fallo visual grave: los botones *"Ver detalle técnico"*, *"Editar texto"* y *"Reintentar"* se encuentran en una sola fila horizontal sin espacio suficiente en pantallas móviles estándar, lo que comprime el botón de reintento hasta convertirlo en una barra vertical verde deformada e ilegible. 

Por otra parte, la app utiliza el modelo `MiniMax-Text-01`, el cual se encuentra desactualizado frente al catálogo vigente de MiniMax. Además, la conexión HTTP a través de Ktor carece de una configuración explícita de `HttpTimeout`, provocando cancelaciones prematuras por socket timeout (~10.5 segundos) cuando la generación de respuesta del LLM requiere mayor tiempo.

Este cambio resuelve la deformación de los botones en la interfaz, actualiza el modelo predeterminado de texto de MiniMax al modelo de alto rendimiento y velocidad (`MiniMax-M2.7-highspeed`), y extiende el timeout de Ktor a 60 segundos manteniendo el endpoint actual `https://api.minimaxi.chat/v1/chat/completions`.

## What Changes

- **Corrección de layout en tarjeta de error de IA (`FoodScanReviewScreen`):**
  - Reestructurar el contenedor de acciones en dos niveles jerárquicos:
    - Nivel superior: Enlace/botón discreto *"Ver detalle técnico"* a la izquierda.
    - Nivel inferior: Botones de acción del usuario (*"Editar texto"* y *"Reintentar"*) con espacio horizontal completo y holgado, previniendo cualquier deformación o compresión de texto.
- **Actualización de modelo de texto MiniMax:**
  - Cambiar el modelo de texto predeterminado en `AiProvider.MINIMAX` de `MiniMax-Text-01` a `MiniMax-M2.7-highspeed` (~100 tokens/segundo, confiable y de baja latencia).
  - Mantener `MiniMax-M3` como el modelo predeterminado para visión multimodal.
- **Configuración de timeout HTTP en Ktor (`MainActivity`):**
  - Instalar el plugin `HttpTimeout` en el `HttpClient` de la aplicación con:
    - `requestTimeoutMillis = 60_000` (60s)
    - `socketTimeoutMillis = 60_000` (60s)
    - `connectTimeoutMillis = 30_000` (30s)
- **Actualización de valores por defecto en analizadores y pruebas:**
  - Actualizar valores predeterminados y pruebas unitarias correspondientes (`SettingsViewModelTest`, `AiConfigurationTest`, etc.).

## Capabilities

### Modified Capabilities
- `food-logging`: Actualiza la presentación de acciones de error de conexión de IA en la pantalla de revisión (`FoodScanReviewScreen`), garantizando accesibilidad y legibilidad completa de los botones *"Ver detalle técnico"*, *"Editar texto"* y *"Reintentar"*, y optimiza los tiempos de espera y el modelo de lenguaje de MiniMax.

## Impact

- **Código afectado:**
  - `FoodScanReviewScreen.kt`: Layout de botones en la tarjeta de error.
  - `AiProvider.kt`: Constante de modelo de texto predeterminado para MiniMax.
  - `MainActivity.kt`: Configuración de `HttpClient` con `HttpTimeout`.
  - Analizadores remotos (`OpenAiCompatibleMealAnalyzer`, etc.) y tests unitarios.
- **Base de datos:** Sin cambios en Room ni DataStore (no requiere migraciones).
- **Fórmulas nutricionales:** Sin impacto en fórmulas metabólicas ni cálculo de macronutrientes.
