# Diseño Técnico: Modelo MiniMax de Alta Velocidad, Timeouts HTTP y Corrección de UI en Tarjeta de Error

## Context

Véase `proposal.md` para la motivación general.
Actualmente, `AiProvider.MINIMAX` define `MiniMax-Text-01` como su modelo de texto por defecto. En `MainActivity.kt`, la instancia compartida de `HttpClient` (Ktor) no tiene instalado el plugin `HttpTimeout`, heredando límites por defecto del socket (~10s) que provocan excepciones `SocketTimeoutException` prematuras al interactuar con modelos de IA.
En la capa de interfaz, la tarjeta de error de `FoodScanReviewScreen` agrupa tres botones en una sola fila `Row(fillMaxWidth(), Arrangement.SpaceBetween)`: *"Ver detalle técnico"*, *"Editar texto"* y *"Reintentar"*. En anchos de pantalla móviles comunes (~360dp a 390dp), el espacio horizontal resulta insuficiente, comprimiendo el botón primario *"Reintentar"* y provocando un quiebre de caracteres vertical.

## Goals / Non-Goals

**Goals:**
- Proporcionar un diseño de Compose libre de compresión o desbordamiento para las acciones de error en `FoodScanReviewScreen`.
- Actualizar el modelo predeterminado de texto de MiniMax a `MiniMax-M2.7-highspeed`.
- Configurar un timeout robusto de 60 segundos en `HttpClient` para llamadas a APIs de LLM.
- Actualizar y validar las pruebas unitarias existentes vinculadas al modelo y configuración de MiniMax.

**Non-Goals:**
- Modificar el protocolo de transporte hacia Anthropic API (se mantiene el protocolo universal OpenAI Chat Completions en `https://api.minimaxi.chat/v1/chat/completions`).
- Cambiar el modelo de visión predeterminado (`MiniMax-M3`).
- Modificar la estructura de base de datos o lógica de fallback heurístico local.

## Decisions

### 1. Reestructuración de la tarjeta de error en Compose (Dos filas)

En `FoodScanReviewScreen.kt`, reemplazamos la fila única horizontal por una estructura vertical ordenada:

```kotlin
Column(modifier = Modifier.fillMaxWidth()) {
    // Fila secundaria: detalle técnico para depuración
    if (uiState.lastTechnicalError != null) {
        TextButton(
            onClick = { showDiagnosticDialog = true },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ver detalle técnico", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Fila principal: acciones de recuperación del usuario
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { showTextEntrySheet = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(id = R.string.btn_edit_text))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onEvent(FoodScanReviewEvent.OnRetryNaturalLanguage) },
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(id = R.string.btn_retry_analysis))
        }
    }
}
```

*Razón:* Al otorgar una línea propia al botón de diagnóstico, las dos acciones de usuario (`Editar texto` y `Reintentar`) tienen más de 250dp disponibles a la derecha, garantizando renderizado perfecto en cualquier pantalla sin aplastarse.

### 2. Actualización de modelo de texto MiniMax a `MiniMax-M2.7-highspeed`

En `AiProvider.kt`:
```kotlin
MINIMAX(
    id = "minimax",
    displayName = "MiniMax Cloud",
    defaultEndpointUrl = "https://api.minimaxi.chat/v1/chat/completions",
    defaultTextModel = "MiniMax-M2.7-highspeed",
    defaultVisionModel = "MiniMax-M3"
)
```
Y en los constructores por defecto de los analizadores (`OpenAiCompatibleMealAnalyzer`, `RemoteNaturalLanguageMealAnalyzer`, etc.), actualizar `model = "MiniMax-M2.7-highspeed"`.

*Razón:* `MiniMax-M2.7-highspeed` entrega 100 tokens por segundo frente a los modelos estándar de 60 tps o los legacy, garantizando alta velocidad de respuesta sin sacrificar precisión al extraer ingredientes y macronutrientes.

### 3. Configuración de `HttpTimeout` en Ktor

En `MainActivity.kt`:
```kotlin
val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
}
```

*Razón:* Evita que Ktor aborte conexiones a los 10 segundos cuando el modelo LLM tarda en generar respuestas estructuradas complejas.

## Risks / Trade-offs

- **[Riesgo] Clientes antiguos con preferencia persistida de `MiniMax-Text-01` en DataStore:**
  *Mitigación:* Si el usuario no ha especificado un modelo personalizado (o si su modelo coincide con el valor legacy default), el sistema resuelve dinámicamente el `effectiveTextModel` del enum `AiProvider.MINIMAX`. En `AiConfiguration.kt`, `customTextModel.takeIf { it.isNotBlank() } ?: provider.defaultTextModel` utiliza automáticamente el nuevo valor por defecto si no hubo personalización manual explícita.
