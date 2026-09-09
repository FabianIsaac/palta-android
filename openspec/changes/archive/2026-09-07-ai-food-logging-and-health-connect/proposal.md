# Propuesta: Registro de Alimentos con IA Visual y Sincronización con Health Connect

## 1. Contexto y Justificación

El registro manual de alimentos pesando cada ingrediente y buscando en catálogos es la mayor barrera de adherencia para usuarios que buscan controlar su ingesta calórica y de macronutrientes. 

Esta propuesta introduce dos capacidades esenciales para la aplicación:
1. **Registro visual asistido por Inteligencia Artificial:** Permite al usuario capturar una fotografía de su plato de comida (o elegirla de su galería) para obtener un desglose automático de alimentos, porciones estimadas en gramos y contenido nutricional.
2. **Sincronización con Google Health Connect:** Exporta de manera transparente y segura los registros nutricionales hacia el almacén central de salud de Android (`NutritionRecord`), permitiendo interoperabilidad con otras aplicaciones de salud y acondicionamiento físico (Google Fit, Samsung Health, etc.).

Para mantener la fiabilidad y el control por parte del usuario, el sistema incorpora un flujo de validación humana (*Human-in-the-Loop*), permitiendo corregir gramajes, eliminar ítems mal identificados o añadir ingredientes faltantes antes de persistir los datos.

---

## 2. Alcance (Scope)

### Dentro del alcance (In-Scope)
- **Captura e ingesta de imágenes:** Integración con CameraX para toma fotográfica en la aplicación y selector del sistema para imágenes de la galería.
- **Motor de análisis de visión desacoplado (Clean Architecture):**
  - **Analizador Local (Offline):** Clasificación básica de alimentos en el dispositivo mediante modelos edge ligeros (LiteRT / MediaPipe) asociados al catálogo de alimentos en Room.
  - **Analizador Remoto (MiniMax API):** Integración con el modelo de visión multimodal de MiniMax mediante prompts estructurados para detectar platos combinados y porciones estimadas en formato JSON.
  - **Configuración de usuario:** Preferencia en DataStore para ingresar API Key de MiniMax y seleccionar el motor de análisis preferido.
- **Pantalla de Revisión y Ajuste Interactivo (Compose):**
  - Vista previa de la fotografía capturada.
  - Lista editable de alimentos detectados con ajuste de porción en gramos/ml.
  - Recálculo reactivo e inmediato de calorías, proteínas, carbohidratos y grasas totales.
  - Posibilidad de agregar o quitar alimentos de la lista antes de confirmar.
  - Selección de categoría de comida chilena (*Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*).
- **Integración con Health Connect:**
  - Verificación de disponibilidad de Health Connect en el dispositivo.
  - Solicitud de permisos en tiempo de ejecución para escritura de nutrición (`WRITE_NUTRITION`).
  - Mapeo de comidas y persistencia de `NutritionRecord` en Health Connect al guardar la comida en la base de datos local.

### Fuera del alcance (Out-of-Scope)
- Sincronización bidireccional continua (importar comidas registradas en otras apps hacia esta aplicación; por ahora solo exportación/escritura).
- Estimación volumétrica 3D por sensores LiDAR o Time-of-Flight (se utilizará estimación heurística basada en visión).
- Dependencia obligatoria de conexión: si el usuario no tiene internet o no configura MiniMax, la aplicación conserva su funcionalidad completa con el analizador local o el registro manual.

---

## 3. Impacto en Componentes del Sistema

### 3.1. Impacto en Base de Datos (Room)
- **Entidades afectadas:** 
  - `MealEntryEntity`: Se añade una columna opcional `healthConnectRecordId: String?` para mantener la trazabilidad con el registro insertado en Health Connect y permitir futuras actualizaciones o eliminaciones sincronizadas.
  - `FoodScanCacheEntity` (opcional): Almacenamiento temporal de los resultados del último análisis para evitar re-análisis en rotaciones de pantalla o interrupciones.
- **Estrategia de migración:** Si la base de datos está en versión inicial (v1), se añade la columna en el esquema inicial; en caso contrario, se define una migración determinista sin pérdida de datos.

### 3.2. Componentes de UI y Estados de Compose Afectados
- **Nuevas Pantallas / Composables:**
  - `FoodCameraScreen`: Interfaz de captura fotográfica optimizada con CameraX y botón de galería.
  - `FoodScanReviewScreen`: Pantalla de revisión interactiva (*Human-in-the-Loop*) con edición de gramos por alimento y totales reactivos.
  - `HealthConnectPermissionDialog`: Diálogo explicativo con tono local chileno para solicitar permisos de sincronización.
  - Sección en `SettingsScreen`: Configuración de motor de IA (Local vs MiniMax) y campo seguro para API Key.
- **Estados de UI (`UiState`):**
  - `FoodScanReviewUiState`: Representa el estado inmutable de la pantalla de revisión (cargando análisis, lista de ítems detectados, valores totales calculados, categoría de comida seleccionada, estado de error o confirmación).

### 3.3. Impacto en Fórmulas y Métricas Nutricionales
- No altera las fórmulas fundamentales de gasto calórico (Mifflin-St Jeor / TDEE).
- Los macronutrientes mantienen las relaciones calóricas fijadas en la arquitectura:
  $$\text{Calorías} = (\text{Proteínas} \times 4) + (\text{Carbohidratos} \times 4) + (\text{Grasas} \times 9)$$
- Cada ajuste de gramaje en la pantalla de revisión recalcula los macronutrientes proporcionalmente a la porción de referencia de 100g.

---

## 4. Localización y Tono (`es-CL`)

Todo el flujo de interacción, mensajes de error y etiquetas se implementan en español chileno sin voseo:
- "Toma una foto de tu plato o selecciona una imagen de tu galería."
- "Revisa y ajusta las porciones antes de guardar tu comida."
- "Guarda tu comida para sincronizarla automáticamente con Health Connect."
- Categorías estándar: *Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*.
