# Tareas de Implementación: Modelo MiniMax de Alta Velocidad, Timeouts HTTP y Corrección de UI en Tarjeta de Error

## 1. Dominio y Configuración de Red

- [x] 1.1 Actualizar el modelo de texto predeterminado en `AiProvider.MINIMAX` a `MiniMax-M2.7-highspeed` en `domain/model/AiProvider.kt` y verificar que compile.
- [x] 1.2 Actualizar los constructores predeterminados en `OpenAiCompatibleMealAnalyzer.kt`, `RemoteNaturalLanguageMealAnalyzer.kt` y `OpenAiCompatibleSupplementAnalyzer.kt` para usar `MiniMax-M2.7-highspeed`.
- [x] 1.3 Configurar el plugin `HttpTimeout` en `MainActivity.kt` en el cliente Ktor (`requestTimeoutMillis = 60_000`, `socketTimeoutMillis = 60_000`, `connectTimeoutMillis = 30_000`).
- [x] 1.4 Actualizar y ejecutar las pruebas unitarias en `AiConfigurationTest`, `SettingsViewModelTest` y `AiDebugLogManagerTest` para validar los nuevos valores por defecto.

## 2. Interfaz de Usuario y Layout Compose

- [x] 2.1 Reestructurar el contenedor de acciones en la tarjeta de error de IA en `FoodScanReviewScreen.kt` en dos niveles (nivel superior para *"Ver detalle técnico"* y nivel inferior alineado a la derecha para *"Editar texto"* y *"Reintentar"*).
- [x] 2.2 Actualizar las descripciones en `strings.xml` (y `values-es-rCL/strings.xml`) si aplica para reflejar el modelo MiniMax-M2.7.
- [x] 2.3 Verificar las vistas previas de Compose (`@Preview`) en `FoodScanReviewScreen.kt` confirmando que los botones se visualizan holgados y sin compresión de texto en anchos de pantalla compactos.

## 3. Verificación y Entrega

- [x] 3.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`) asegurando 100% de éxito.
- [x] 3.2 Compilar el APK ejecutable (`./gradlew assembleDebug`).
- [x] 3.3 Ejecutar el script obligatorio de envío del APK a Telegram (`bash ./scripts/send_apk_telegram.sh`).

