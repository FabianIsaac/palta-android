# Propuesta: Descomposición Atómica de Ingredientes y Resiliencia Offline (offline-resilient-meal-decomposition)

## 1. Justificación y Problema a Resolver

Al registrar comidas mediante texto en lenguaje natural en la aplicación, se han identificado dos problemas críticos de usabilidad y exactitud:

1. **Fusión indiscriminada de preparaciones compuestas (Compound Dishes):**
   Cuando un usuario describe una comida armada detallando sus ingredientes (por ejemplo: *"me comí 2 fajitas con choclo, carne molida, lechuga, tomate aderezado con yogurt griego y tajín"*), el modelo de IA en la nube consolida todo en 1 o 2 platos genéricos (ej. *"Fajitas de carne"* y *"Yogurt con tajín"*). Esto priva al usuario del control granular: no puede revisar ni ajustar las cantidades individuales de carne, vegetales o aderezo, generando la percepción de que la app ignoró la mayor parte de lo ingresado.
2. **Pérdida irreversible del texto ante errores de red o de API:**
   Actualmente, el texto ingresado en el diálogo rápido viaja de forma efímera. Si la API de MiniMax sufre un error de red, límite de cuota o tiempo de espera, la app muestra un simple Snackbar de error y descarta por completo la descripción ingresada. El usuario pierde un párrafo extenso que tomó tiempo redactar y queda frente a una pantalla vacía.
3. **Inoperancia del catálogo local en modo offline para ingredientes cotidianos:**
   El catálogo offline carece de alimentos básicos habituales en preparaciones chilenas (como masas de fajita, carne molida, lechuga, choclo desgranado y yogurt griego). Si el usuario se encuentra sin conexión, la app no logra generar ni siquiera un cálculo preliminar.

---

## 2. Objetivos y Alcance

### En Alcance (In-Scope):
- **Descomposición Atómica de Ingredientes en Lenguaje Natural (Cloud LLM):**
  - Actualización del system prompt y ejemplos en `RemoteNaturalLanguageMealAnalyzer` para exigir la separación en ítems independientes de la base (masas/tortillas/pan) y de cada relleno, proteína, verdura, salsa y condimento cuando el usuario los detalle explícitamente.
  - Distribución proporcional y realista de porciones caseras (ej. 2 fajitas = 2 unidades de tortilla ~80g, carne molida ~100g, verduras ~30-50g c/u, aderezo ~30-40g).
- **Retención de Borrador y Reintento Inmediato en UI:**
  - Preservación del texto ingresado (`lastRawDescription`) en el estado del ViewModel ante cualquier fallo de red o excepción de la IA.
  - Interfaz de error con el texto original visible, botón directo de **[Reintentar]** sin volver a escribir y botón **[Editar]** que reabre el modal con el texto precargado.
- **Enriquecimiento del Catálogo Local Offline:**
  - Incorporación en `LocalFoodCatalogRepository` de alimentos cotidianos: tortillas de fajita/taco, carne molida de vacuno, lechuga picada, choclo desgranado, tomate picado y yogurt griego.
  - Generación de un estimado preliminar local offline cuando no haya internet disponible.
- **Sincronización y Reprocesamiento Automático con Conexión:**
  - Persistencia del texto original (`rawDescription`) y bandera de estado (`isPendingAiRefinement`) en la entidad Room de la comida.
  - Sincronizador automático que, al restablecerse la conectividad a internet, envía las comidas pendientes a la IA para refinar los ingredientes y recalcular los macronutrientes sin intervención obligatoria del usuario.

### Fuera de Alcance (Out-of-Scope):
- Reconocimiento de audio / voz con modelos locales offline (se sigue utilizando el dictado por voz nativo del teclado de Android).
- Soporte para recetas multinivel con pasos de cocción elaborados (el foco es el registro de ingesta de alimentos).

---

## 3. Impacto en Base de Datos y Persistencia Room

Se añade soporte para comidas con análisis pendiente o diferido:
- **`MealEntryEntity`:** Se agregan dos columnas opcionales/con valor por defecto:
  - `rawDescription: String? = null`: Texto original escrito por el usuario.
  - `isPendingAiRefinement: Boolean = false`: Indica si la comida requiere refinamiento con IA en cuanto haya conexión.
- **Estrategia de Migración:** Migración automática de Room o actualización de esquema `fallbackToDestructiveMigration` / migración manual de esquema v2 a v3 si aplica.

---

## 4. Componentes de UI y Estados de Compose Afectados

- **`QuickNaturalLanguageEntrySheet`:**
  - Acepta un parámetro de texto inicial (`initialText`) para no perder lo escrito si el usuario decide editar tras un intento fallido.
- **`FoodScanReviewScreen` & `FoodScanReviewViewModel`:**
  - Nuevo estado `rawDescription` y `canRetry` en `FoodScanReviewUiState`.
  - Visualización de tarjeta de estado cuando ocurre un error, con opciones para reintentar o usar el estimado local.
- **`DailySummaryScreen` & `DailySummaryViewModel`:**
  - Las tarjetas de comida con `isPendingAiRefinement = true` muestran un distintivo ("Estimado local - Pendiente de IA") y un indicador discreto de sincronización cuando se procesen en segundo plano.

---

## 5. Impacto en Fórmulas y Métricas Nutricionales

- No se alteran las fórmulas metabólicas base (Mifflin-St Jeor) ni las constantes energéticas (4 kcal/g para proteínas y carbohidratos, 9 kcal/g para grasas).
- Mejora sustancialmente la exactitud de los macronutrientes al desglosar proteínas magras (carne), carbohidratos complejos (tortillas, choclo), fibra (verduras) y grasas (aderezos) en lugar de usar un promedio opaco de plato combinado.
