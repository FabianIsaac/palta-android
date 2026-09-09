# Propuesta: Resumen Diario y Edición Sincronizada de Comidas

## 1. Contexto y Justificación

Actualmente, la aplicación permite registrar alimentos mediante visión computacional con IA (local y remota con MiniMax) y sincronizarlos inicialmente con Google Health Connect. Sin embargo, no existe una vista principal que consolide el balance calórico diario, ni un mecanismo para consultar o modificar comidas registradas con anterioridad.

En la vida cotidiana, es común que una persona registre su desayuno por la mañana (por ejemplo, 1 marraqueta con palta y un café) y más tarde en el día, incluso a la hora de la once o cena, recuerde que consumió media porción adicional o que registró un alimento por error. 

Esta propuesta resuelve este problema garantizando que:
1. El usuario disponga de una pantalla principal de **Resumen Diario** que muestre su balance calórico (presupuesto diario, calorías consumidas, calorías restantes), barras de macronutrientes (proteínas, carbohidratos, grasas) y tarjetas ordenadas según las comidas chilenas (*Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*).
2. El usuario pueda tocar cualquier comida ya guardada (sea de hoy o de días anteriores) para abrir una pantalla de **Edición y Detalle de Comida**.
3. Las modificaciones se propaguen y recalculen de forma **inmediata y coherente en todos lados**:
   - **Base de datos local (Room):** Se recalculan los totales del registro y sus ítems en una transacción atómica.
   - **Panel de control diario (UI):** Los totales del día (calorías y macros consumidos vs objetivo) se actualizan reactivamente vía `Flow`.
   - **Google Health Connect:** Se actualiza el `NutritionRecord` existente en Health Connect mediante su identificador único (`healthConnectRecordId`), o se elimina si la comida es borrada, manteniendo sincronizadas las aplicaciones externas de salud.

---

## 2. Alcance (Scope)

### Dentro del alcance (In-Scope)
- **Pantalla de Resumen Diario (`DailySummaryScreen`):**
  - Selector de fecha con navegación hacia el día anterior, día siguiente o selección mediante DatePicker.
  - Indicador circular de progreso calórico: meta diaria, calorías consumidas y calorías restantes o excedentes.
  - Barras de progreso de macronutrientes: Proteínas (g), Carbohidratos (g) y Grasas (g) con porcentaje alcanzado respecto a la meta calculada.
  - Lista de tarjetas por categoría de comida típica chilena:
    - *Desayuno*
    - *Almuerzo*
    - *Once / Cena*
    - *Colaciones*
  - Cada tarjeta de comida exhibe: calorías totales, desglose de macros y lista de alimentos registrados con sus porciones en gramos.
  - Botón de acceso directo para agregar comida a la categoría (mediante cámara con IA o manual).
- **Flujo de Edición y Detalle de Comida (`EditMealScreen`):**
  - Carga reactiva de los alimentos asociados a la comida seleccionada.
  - Ajuste interactivo de gramos por alimento con recálculo dinámico en tiempo real.
  - Eliminación de alimentos individuales o eliminación completa de la comida.
  - Adición de nuevos alimentos a la comida existente.
  - Botón "Guardar Cambios" que confirma las modificaciones.
- **Capa de Persistencia y Sincronización:**
  - Consulta reactiva de comidas e ítems por rango de fecha en Room (`MealDao`).
  - Actualización atómica de `MealEntryEntity` y sustitución de `MealFoodItemEntity`.
  - Actualización de registros nutricionales en Health Connect (`updateNutritionRecord`).
  - Eliminación de registros nutricionales en Health Connect (`deleteNutritionRecord`) al borrar una comida.
- **Navegación e Integración Principal:**
  - `DailySummaryScreen` como pantalla de inicio predeterminada en `MainActivity`.
  - Navegación fluida hacia `FoodCameraScreen` (para escanear nueva comida) y `EditMealScreen` (para modificar una comida existente).

### Fuera del alcance (Out-of-Scope)
- Gráficos avanzados de tendencias semanales o mensuales (reservado para la fase de estadísticas históricas).
- Sincronización con servicios en la nube propietarios (mantiene arquitectura offline-first).

---

## 3. Impacto en Componentes del Sistema

### 3.1. Impacto en Base de Datos (Room)
- **Entidades:** No se requieren modificaciones en las tablas existentes (`meal_entries` y `meal_food_items`), ya que cuentan con `healthConnectRecordId` y clave foránea en cascada.
- **DAO (`MealDao`):**
  - Consulta `getMealsWithItemsByDate(startOfDay: Long, endOfDay: Long): Flow<List<MealWithItems>>` mediante `@Transaction` y `@Relation`.
  - Consulta `getMealWithItemsById(mealId: Long): Flow<MealWithItems?>`.
  - Método transaccional `updateMealWithItems(meal: MealEntryEntity, items: List<MealFoodItemEntity>)`.
  - Método `deleteMeal(mealId: Long)` que por eliminación en cascada remueve automáticamente los ítems secundarios.

### 3.2. Componentes de UI y Estados de Compose Afectados
- **Nuevas Pantallas:**
  - `DailySummaryScreen`: Muestra el estado del día, navegación por fecha, metas y tarjetas de comidas.
  - `EditMealScreen`: Permite editar cantidades, eliminar ítems o descartar la comida entera.
- **Contratos de Estado (`UiState`):**
  - `DailySummaryUiState`: Contiene la fecha seleccionada, presupuesto calórico, totales consumidos, desglose de macronutrientes, lista de comidas agrupadas por categoría y estado de carga.
  - `EditMealUiState`: Contiene la comida en edición, lista de alimentos con sus gramos, totales recalculados y estado de guardado/eliminación.
- **Localización:**
  - Todo texto visible en español de Chile sin voseo (*"Guarda los cambios"*, *"Revisa tu consumo"*, *"Elimina este alimento"*, *"Once / Cena"*, *"Desayuno"*).

### 3.3. Impacto en Fórmulas y Métricas Nutricionales
- El presupuesto calórico diario y las metas de macronutrientes se obtienen del perfil del usuario (fórmula Mifflin-St Jeor: TDEE y distribución de macros: 4 kcal/g proteína, 4 kcal/g carbohidratos, 9 kcal/g grasa).
- El cálculo de balance diario es determinista:
  $$\text{Calorías Restantes} = \text{Presupuesto Diario} - \sum \text{Calorías Comidas del Día}$$
- La edición de una porción en gramos recalcula las calorías y macros del alimento proporcionalmente a su base de 100g.
