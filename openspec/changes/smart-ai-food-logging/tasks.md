# Tareas de Implementación: smart-ai-food-logging

## Fase 1: Dominio y Casos de Uso (Domain Layer)
- [x] 1.1. Crear modelo de dominio `HouseholdPortion` y enum `HouseholdUnit` (taza, tazón, cucharada, unidad, etc.).
- [x] 1.2. Definir interfaz de repositorio `NaturalLanguageMealAnalyzer` para análisis de descripciones de texto.
- [x] 1.3. Implementar `ParseNaturalLanguageMealUseCase` con validaciones de entrada vacía y sanitización de texto.
- [x] 1.4. Escribir pruebas unitarias exhaustivas para `ParseNaturalLanguageMealUseCase` y conversión de medidas caseras.

## Fase 2: Capa de Datos y Servicios de IA (Data Layer)
- [x] 2.1. Implementar `RemoteNaturalLanguageMealAnalyzer` con llamada HTTP a LLM (MiniMax / Gemini) para interpretar texto cotidiano y responder en JSON estructurado.
- [x] 2.2. Corregir `LocalLiteRtVisionAnalyzer`: eliminar el retorno fijo de `["pollo", "arroz"]`. Si la confianza es insuficiente, retornar resultado vacío o error legible en vez de datos falsos.
- [x] 2.3. Ajustar el prompt de `MiniMaxVisionAnalyzer` para reforzar la detección de bebidas calientes, tazas transparentes, café, té y desayunos chilenos.
- [x] 2.4. Ampliar `LocalFoodCatalogRepository` con bebidas e infusiones (Café negro, Café con leche, Té, Azúcar, Endulzante, Leche).
- [x] 2.5. Escribir pruebas unitarias para el parseo del JSON de respuesta de lenguaje natural y el catálogo enriquecido.

## Fase 3: Capa de Presentación (UI & Compose)
- [x] 3.1. Diseñar e implementar componente Compose `QuickNaturalLanguageEntryBar` (o diálogo/bottom sheet) para ingresar comida por texto o dictado por voz.
- [x] 3.2. Integrar el botón/acceso de entrada rápida por texto tanto en `FoodCameraScreen` como en `FoodScanReviewScreen`.
- [x] 3.3. Actualizar `FoodCameraScreen` para mostrar un indicador claro del motor activo (ej. "IA en la nube activa" vs "Modo local").
- [x] 3.4. Actualizar `FoodScanReviewScreen` para mostrar porciones amigables (ej. "1 taza (~200 ml)") junto a los gramos.
- [x] 3.5. Crear vistas previas (`@Preview`) de Compose en español de Chile (`es-CL`) sin voseo para los nuevos estados y componentes.

## Fase 4: Verificación y Entrega
- [x] 4.1. Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 4.2. Compilar APK y validar ausencia de advertencias críticas o errores de compilación (`./gradlew assembleDebug`).
- [x] 4.3. Enviar APK generado automáticamente por Telegram mediante `bash ./scripts/send_apk_telegram.sh`.
