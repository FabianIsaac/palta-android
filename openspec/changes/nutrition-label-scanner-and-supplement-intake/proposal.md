# Propuesta: Escaneo de Tablas Nutricionales con IA y Mejoras en Gestión de Suplementos

## 1. Contexto y Justificación

Al momento de incorporar alimentos envasados o suplementos deportivos (proteínas en polvo, creatinas, pre-entrenos, barras proteicas), los usuarios se enfrentan a tablas nutricionales complejas y extensas. Estas tablas suelen incluir múltiples columnas (por 100g vs por porción), decenas de micronutrientes irrelevantes para el conteo calórico general (vitaminas, sales, minerales), y unidades confusas (microgramos, porcentajes de valor diario `% DV`, miligramos).

Para un usuario común, descifrar manualmente estas etiquetas es lento y propenso a errores de transcripción (por ejemplo, registrar el porcentaje diario en lugar de los gramos reales o confundir las calorías de la porción con las de los 100g).

Asimismo, en la pantalla de gestión de suplementos (`SupplementsScreen`) se identificaron dos oportunidades claras de mejora de experiencia de usuario:
1. **Ocupación excesiva de espacio vertical por sugerencias:** El catálogo de sugerencias preconfiguradas (*Creatina, Citrato de Magnesio, Omega 3, etc.*) se despliega completamente por defecto mediante un `FlowRow`, ocupando gran parte de la pantalla y empujando el formulario y la rutina activa fuera del primer plano visual.
2. **Falta de visibilidad sobre la constancia de consumo:** Aunque la base de datos registra cada toma diaria en la tabla `supplement_logs`, la interfaz de la rutina no le muestra al usuario cuántas veces ha consumido o registrado efectivamente cada suplemento, perdiendo una oportunidad de gamificación y feedback de constancia.

---

## 2. Metas y Alcance

### In-Scope (Dentro del alcance)
* **Escaneo de Tablas Nutricionales Asistido por IA (Cámara y Galería):**
  - Implementar la capacidad de capturar o seleccionar una imagen de una etiqueta nutricional o envase de producto (mediante `TakePicturePreview` y `PickVisualMedia`).
  - Procesar la imagen con el analizador multimodal de IA del proyecto (compatible con Gemini, MiniMax y proveedores OpenAI-compatible).
  - Extraer de forma estructurada: nombre del producto (si es deducible), tamaño/descripción de la porción habitual (ej: `1 scoop (33g)` o gramos por porción), calorías totales por porción y desglose de macronutrientes (proteínas, carbohidratos, grasas).
  - Integrar este escaneo tanto en el formulario de creación de suplementos (`SupplementsScreen`) como en la pestaña de alimentos personalizados al agregar ingredientes a una comida (`AddIngredientSheet`).
* **Sugerencias de Suplementos Plegables / Colapsadas:**
  - Rediseñar la sección de sugerencias de suplementos para que aparezca colapsada por defecto con un encabezado compacto y un indicador de expansión (flecha o chevron con conteo de sugerencias disponibles).
  - Permitir expandir y contraer la sección de manera interactiva con un toque.
* **Contador de Tomas Acumuladas en la Rutina:**
  - Consultar en `SupplementLogDao` el número total de registros históricos asociados a cada suplemento (`COUNT(*) WHERE supplementId = :id`).
  - Mostrar en cada tarjeta de suplemento de la rutina activa un distintivo o etiqueta con las tomas registradas (ej. *"14 tomas registradas"*).
* **Consistencia Lingüística:**
  - Garantizar redacción 100% en español de Chile (`es-CL`) sin voseo en todos los textos, botones, diálogos y descripciones.

### Out-of-Scope (Fuera del alcance)
* Modificar el esquema de base de datos de Room existente (`SupplementEntity` y `SupplementLogEntity` ya admiten las operaciones requeridas).
* Reconocimiento de códigos de barra (UPC/EAN) con bases de datos externas como OpenFoodFacts (se prioriza el escaneo visual directo por IA).
* Alterar las fórmulas de gasto energético diario o el cálculo de metas de macronutrientes.

---

## 3. Criterios de Aceptación

1. El usuario puede tocar un botón de escaneo en la pantalla de Suplementos o en el diálogo de Agregar Ingrediente Personalizado, eligiendo entre capturar una fotografía con la cámara o seleccionar una imagen existente desde su galería.
2. Tras procesar una imagen de tabla nutricional, los campos de porción/dosis, calorías, proteínas, carbohidratos y grasas se rellenan automáticamente de forma coherente con la etiqueta analizada, permaneciendo editables para el usuario.
3. En caso de error de red, falla de API o imagen ilegible, el sistema presenta un mensaje comprensible en español chileno y mantiene los campos editables sin bloquear la pantalla.
4. La sección de sugerencias preconfiguradas en `SupplementsScreen` se muestra colapsada por defecto y se expande/contrae fluidamente al presionar su encabezado.
5. Cada suplemento listado en la rutina activa muestra claramente el número acumulado de veces que ha sido marcado como tomado en el historial.
6. Todo el código nuevo de dominio y ViewModel cuenta con pruebas unitarias deterministas.
