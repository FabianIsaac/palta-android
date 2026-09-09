# Tareas de Implementación: Soporte Multiproveedor de IA

## Fase 1: Dominio y Modelos de Configuración de IA

- [x] 1.1 Crear enum `AiProvider` en `domain/model/AiProvider.kt` con presets para Local, NVIDIA NIM, Google Gemini, MiniMax y Custom.
- [x] 1.2 Crear clase de datos `AiConfiguration` en `domain/model/AiConfiguration.kt` con resolución dinámica de endpoints y modelos efectivos.
- [x] 1.3 Crear pruebas unitarias en `AiConfigurationTest` verificando la resolución de valores predeterminados y personalizados para cada proveedor.

---

## Fase 2: Repositorio de Preferencias y Persistencia (DataStore)

- [x] 2.1 Extender `UserPreferencesRepository` para almacenar y emitir las claves de cada proveedor (`NVIDIA`, `GEMINI`, `MINIMAX`, `CUSTOM`), el proveedor activo y las URLs/modelos personalizados.
- [x] 2.2 Migrar de forma transparente la clave existente `MINIMAX_API_KEY` hacia el nuevo esquema multiproveedor para no interrumpir a usuarios existentes.
- [x] 2.3 Crear pruebas unitarias en `UserPreferencesRepositoryTest` validando el almacenamiento y recuperación independiente de credenciales por proveedor.

---

## Fase 3: Capa de Red y Analizadores Universales OpenAI-Compatible

- [x] 3.1 Refactorizar / implementar analizadores universales compatibles con OpenAI:
  - `OpenAiCompatibleMealAnalyzer` para análisis por texto / voz.
  - `OpenAiCompatibleVisionAnalyzer` para análisis de fotos (soporte de Base64 multimodal).
  - `OpenAiCompatibleSupplementAnalyzer` para estimación de suplementos.
- [x] 3.2 Implementar soporte de sanitización para etiquetas de razonamiento (`<think>...</think>`) y bloques Markdown en respuestas de LLMs.
- [x] 3.3 Adaptar `FoodVisionAnalyzerFactory` para instanciar el analizador correspondiente según el proveedor activo y la conectividad.
- [x] 3.4 Actualizar o agregar pruebas unitarias deterministas (`OpenAiCompatibleMealAnalyzerTest`, `OpenAiCompatibleVisionAnalyzerTest`).

---

## Fase 4: Inyección de Dependencias y ViewModel de Ajustes

- [x] 4.1 Actualizar `SettingsViewModel` para exponer el estado del proveedor de IA seleccionado, claves por proveedor y parámetros avanzados.
- [x] 4.2 Conectar `MainActivity` para suministrar la `AiConfiguration` activa a los casos de uso y analizadores.
- [x] 4.3 Escribir pruebas unitarias en `SettingsViewModelTest` validando el cambio reactivo de proveedor y el guardado de credenciales.

---

## Fase 5: Interfaz de Usuario (Ajustes en Compose)

- [x] 5.1 Actualizar recursos de texto `strings.xml` (en español de Chile sin voseo) con las descripciones y nombres de los proveedores y enlaces de ayuda.
- [x] 5.2 Rediseñar la sección *"Motor de Inteligencia Artificial"* en `SettingsScreen`:
  - Selector intuitivo de proveedor (tarjetas o radio buttons).
  - Campo de API Key adaptado al proveedor activo con indicador para obtenerla gratis.
  - Sección expandible para parámetros avanzados (endpoints y modelos personalizados).
- [x] 5.3 Actualizar y verificar los `@Preview` de `SettingsScreen` con los distintos estados de proveedor.

---

## Fase 6: Verificación, Compilación y Entrega

- [x] 6.1 Ejecutar suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 6.2 Compilar el APK ejecutable en modo debug (`./gradlew assembleDebug`).
- [x] 6.3 Ejecutar el script obligatorio de envío del APK a Telegram (`bash ./scripts/send_apk_telegram.sh`).
