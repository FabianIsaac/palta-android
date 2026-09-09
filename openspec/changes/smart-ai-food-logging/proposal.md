# Propuesta: Registro Inteligente con IA y Lenguaje Natural (smart-ai-food-logging)

## 1. Justificación y Problema a Resolver

Actualmente, registrar una comida en la aplicación presenta fricción importante para usuarios que no son nutricionistas:
1. **Falso escaneo en el motor local:** El analizador local de cámara (`LocalLiteRtVisionAnalyzer`) posee un retorno fijo simulado (`["pollo", "arroz"]`), lo que causa que cualquier fotografía tomada (por ejemplo, una taza de café o té) arroje siempre "Pechuga de pollo a la plancha" y "Arroz blanco cocido", generando confusión y la percepción de que la app "quedó pegada".
2. **Carencia de bebidas cotidianas en el catálogo:** El catálogo local carece de alimentos líquidos esenciales de la dieta diaria (café negro, café con leche, té, etc.).
3. **Cálculo manual intimidante:** La interfaz actual exige que el usuario ingrese o edite porciones en gramos crudos y conozca calorías o macronutrientes (proteínas, carbohidratos, grasas). Para una persona promedio, esto resulta confuso e impráctico: nadie pesa una taza de café ni conoce de memoria los gramos de carbohidratos de su desayuno.
4. **Falta de registro en lenguaje cotidiano:** No existe la posibilidad de simplemente escribir o dictar lo consumido (*"un café con leche y una de azúcar"*, *"dos huevos revueltos con media marraqueta"*) para que la inteligencia artificial se encargue del desglose completo.

---

## 2. Objetivos y Alcance

### En Alcance (In-Scope):
- **Entrada en Lenguaje Natural mediante IA:**
  - Nueva opción de registro rápido por texto libre: el usuario describe su comida en español chileno cotidiano.
  - Procesamiento con IA (LLM) que interpreta la descripción, descompone los alimentos, estima cantidades en medidas caseras y calcula calorías y macronutrientes automáticamente.
- **Soporte de Medidas Caseras y Cotidianas:**
  - Visualización y edición en unidades familiares (taza, tazón, vaso, cucharada, unidad, rebanada) con equivalencias automáticas a gramos/ml.
- **Reconocimiento Visual Real por Cámara (Visión con IA):**
  - Integración transparente con motor de visión multimodal (Gemini / MiniMax) capaz de reconocer bebidas como café, té e infusiones a partir de fotos reales.
  - Eliminación de la simulación engañosa del motor local: si no hay conexión o no hay IA disponible, la app informará claramente el estado en lugar de fingir detección de pollo y arroz.
- **Ampliación del Catálogo Local de Alimentos:**
  - Incorporación de bebidas habituales de desayuno y once chilena: Café solo/americano, Café cortado/con leche, Té negro/verde, Leche descremada/entera, Azúcar y Endulzante.

### Fuera de Alcance (Out-of-Scope):
- Integración de periféricos de báscula Bluetooth.
- Reconocimiento por voz en tiempo real con streaming de audio (se aprovecha el teclado nativo de Android con dictado por voz del sistema).

---

## 3. Experiencia de Usuario y Flujo de Interacción

```text
+-------------------------------------------------------------------------+
|                  NUEVO FLUJO DE REGISTRO INTELIGENTE                   |
+-------------------------------------------------------------------------+
                                    |
          +-------------------------+-------------------------+
          |                                                   |
          v                                                   v
   [ 📸 Foto con Cámara ]                              [ ✍️ Texto Cotidiano ]
   - Fotografía de taza de café                        - "Un café con leche y 1 azúcar"
          |                                                   |
          v                                                   v
   +----------------------------------------------------------------------+
   |                    Motor de IA Nutricional                            |
   |  - Identifica alimentos y porciones en unidades caseras             |
   |  - Calcula automáticamente: Calorías, Proteínas, Carbs, Grasas       |
   +----------------------------------------------------------------------+
                                    |
                                    v
   +----------------------------------------------------------------------+
   |                    Pantalla de Revisión Amigable                     |
   |  - Muestra: "Café con leche (1 taza - 200 ml)"                       |
   |  - Muestra: "Azúcar (1 cdta - 5 g)"                                  |
   |  - Calorías totales calculadas: ~65 kcal                             |
   |  - Botón: [ ✅ Confirmar y Guardar ]                                 |
   +----------------------------------------------------------------------+
```

---

## 4. Impacto en Base de Datos y Arquitectura
- **Domain:** Nuevo caso de uso `ParseNaturalLanguageMealUseCase` y nuevo contrato de repositorio `NaturalLanguageMealAnalyzer`.
- **Data:** 
  - Ampliación de `LocalFoodCatalogRepository` para incluir bebidas y porciones estándar.
  - Implementación del servicio de análisis de texto libre en `data/remote`.
  - Mejora en `LocalLiteRtVisionAnalyzer` para evitar resultados falsos.
- **UI:** 
  - Integración de un campo de texto rápido en la pantalla de cámara / registro manual.
  - Presentación de porciones en medidas caseras legibles además de gramos.
