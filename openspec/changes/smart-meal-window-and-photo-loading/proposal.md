# Propuesta: Ventana Inteligente de Comidas y Carga Inmediata de Foto

## 1. Problema y Justificación

Durante las pruebas de usuario en dispositivo físico se identificaron dos fricciones críticas en el flujo de registro de alimentos:

1. **Falta de feedback y estado de carga al tomar fotos:**
   Al presionar el obturador de la cámara o seleccionar una foto de la galería, la interfaz de la cámara permanecía estática y reactiva sin ningún indicador visual mientras el hardware de la cámara capturaba y el servicio de IA en la nube (MiniMax) procesaba la imagen (latencia de 3 a 7 segundos). Esto generó la sensación de que el teléfono se había "pegado" o que el botón no había respondido.
   
2. **Duplicación de alimentos y acumulación indebida de registros:**
   Al guardar una comida (por ejemplo, el plato de fondo en el Almuerzo) y posteriormente registrar un nuevo alimento (por ejemplo, 6 wantanes ingresados mediante descripción por texto a los 15 minutos):
   - El estado del ViewModel de revisión (`FoodScanReviewViewModel`) no se reiniciaba a vacío tras guardar.
   - La entrada por texto combinaba los alimentos de la comida anterior con el nuevo alimento.
   - Al presionar guardar, se creaba un segundo registro con ambos elementos acumulados, resultando en dos entradas bajo Almuerzo: Registro 1 (Plato de fondo) y Registro 2 (Plato de fondo + Wantanes), duplicando las calorías totales.

3. **Necesidad de consolidación en comidas divididas en tandas:**
   En la vida real, una persona consume sus alimentos a lo largo de un período de 15 a 30 minutos (plato de fondo, ensalada, postre, agregado). Si un alimento se registra dentro de una ventana de 30 minutos de una comida previa de la misma categoría, debe sumarse al mismo registro existente, manteniendo una vista limpia y ordenada. Si se registra transcurridos más de 30 minutos, debe crearse un registro separado.

4. **Principio de diseño espacioso y cómodo:**
   La interfaz debe ser limpia, ordenada y minimalista, pero sin escatimar en espacio: las tarjetas, los paddings, los indicadores y los textos deben respirar, permitiendo que la interfaz crezca verticalmente de manera natural y cómoda.

---

## 2. Alcance (In-Scope)

- **Feedback visual y transición inmediata a pantalla de carga:**
  - Bloqueo inmediato del botón del obturador tras el toque para prevenir disparos múltiples.
  - Transición inmediata a la pantalla de revisión en modo de carga espacioso con animación, spinner moderno y mensajes tranquilizadores en español chileno ("Analizando tu plato con IA...", tips de nutrición).
- **Limpieza estricta de estado:**
  - Reinicio automático del estado de `FoodScanReviewViewModel` tras confirmar el guardado o al cancelar/navegar hacia atrás.
  - Asegurar que cualquier nueva captura o descripción de texto inicie con una lista limpia de ingredientes.
- **Ventana inteligente de 30 minutos para consolidación de comidas:**
  - Lógica de dominio que detecta si existe un registro previo en la misma categoría en la fecha actual.
  - Si `(timestamp_actual - timestamp_anterior) <= 30 minutos`: se anexan los nuevos alimentos al registro existente, recalculando calorías, macronutrientes y actualizando el registro en Health Connect.
  - Si `(timestamp_actual - timestamp_anterior) > 30 minutos`: se genera un registro nuevo independiente en dicha categoría.
- **Indicador contextual en la pantalla de revisión:**
  - Banner informativo amplio y claro en la pantalla de revisión que avise al usuario: *"Se sumará a tu Almuerzo reciente (hace X min)"* cuando aplique la regla de los 30 minutos.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Modificación de los algoritmos de detección de imágenes de MiniMax o LiteRT.
- Modificación del esquema de tablas de Room (la entidad `MealEntryEntity` y `MealFoodItemEntity` ya soportan actualización y acumulación).
- Creación de categorías de comida personalizadas fuera de las 4 chilenas estándar.
