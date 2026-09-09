# Diseño Técnico: Edición de Alimentos y Adición de Ingredientes en Comidas

## 1. Arquitectura y Modelos

### 1.1 Flujo Unidireccional de Datos (UDF)

```
+-------------------------------------------------------------------------+
|                  FLUJO DE EDICIÓN Y ADICIÓN DE INGREDIENTES             |
+-------------------------------------------------------------------------+
|                                                                         |
|  [EditMealScreen / FoodScanReviewScreen]                                |
|    |                                                                    |
|    |-- Toque en nombre / icono lápiz -> Muestra EditItemNameDialog      |
|    |     |                                                              |
|    |     v (Confirma nuevo nombre)                                      |
|    |   OnItemNameChanged(index / id, newName)                           |
|    |     |                                                              |
|    |     v                                                              |
|    |   Actualiza ScannedFoodItem.copy(name = newName) en UiState        |
|    |                                                                    |
|    +-- Toque "+ Agregar ingrediente" -> Muestra AddIngredientSheet      |
|          |                                                              |
|          +---> Pestaña 1: Catálogo local (Buscador instantáneo)         |
|          +---> Pestaña 2: Personalizado / Rápido                        |
|          |                                                              |
|          v (Selecciona o confirma)                                      |
|        OnItemAdded(item)                                                |
|          |                                                              |
|          v                                                              |
|        items = items + item                                             |
|        summary = NutritionSummary.fromItems(items)                      |
|                                                                         |
|  Al presionar "Guardar cambios" -> MealRepository.updateMealEntry()     |
|  Sincronización con Health Connect (NutritionRecord actualizado)        |
+-------------------------------------------------------------------------+
```

### 1.2 Eventos y Estados

#### En `EditMealContract.kt`:
```kotlin
sealed interface EditMealEvent {
    // Eventos existentes
    data class OnCategoryChanged(val category: MealCategory) : EditMealEvent
    data class OnServingGramsChanged(val itemIndex: Int, val newGrams: Double) : EditMealEvent
    data class OnRemoveItem(val itemIndex: Int) : EditMealEvent
    data object OnSaveMealClicked : EditMealEvent
    // Nuevos eventos
    data class OnItemNameChanged(val itemIndex: Int, val newName: String) : EditMealEvent
    data class OnItemAdded(val item: ScannedFoodItem) : EditMealEvent
}
```

#### En `FoodScanReviewContract.kt`:
```kotlin
sealed interface FoodScanReviewEvent {
    // Nuevos eventos
    data class OnItemNameChanged(val itemId: String, val newName: String) : FoodScanReviewEvent
    // (OnItemAdded ya existe en FoodScanReviewContract)
}
```

---

## 2. Componentes de Interfaz de Usuario (Jetpack Compose)

### 2.1 Diálogo de Edición de Nombre (`EditItemNameDialog`)
Componente reutilizable en `presentation/common/` o `presentation/edit/`:
- `AlertDialog` de Material 3.
- Título: *"Editar nombre del alimento"*.
- `OutlinedTextField` con el nombre actual enfocado y selección de texto.
- Validación: botón *"Guardar"* deshabilitado si el texto está en blanco.
- Botones de acción: *"Cancelar"* y *"Guardar"*.

### 2.2 Hoja Inferior para Agregar Ingredientes (`AddIngredientSheet`)
Implementado con `ModalBottomSheet`:
- **Barra de búsqueda superior:** Filtrado en tiempo real sobre `FoodCatalogRepository.searchFood(query)` (catálogo local de alimentos chilenos como palta, marraqueta, leche, pollo, huevos, etc.).
- **Lista de resultados:** Muestra nombre, porción de referencia y calorías/macros por cada 100g.
- **Selector de porción rápida:** Al seleccionar un alimento, permite elegir la cantidad en gramos o porciones caseras (ej. 1 cucharada sopera, 1 taza, 1 rebanada) usando `HouseholdPortion`.
- **Modo personalizado:** Permite ingresar un alimento propio si no se encuentra en el catálogo local.
- Al confirmar, emite `OnItemAdded` con el nuevo `ScannedFoodItem` generado.

### 2.3 Modificaciones en `EditMealScreen`
- En `EditableFoodItemRow`:
  - El título del alimento muestra un icono sutil de edición (lápiz) o es clickeable, abriendo `EditItemNameDialog`.
- En el `LazyColumn` de `EditMealScreen`:
  - Se añade el botón ancho `+ Agregar ingrediente` debajo de la lista de ítems, manteniendo un estilo cómodo y accesible.
- En el `TopAppBar` o barra inferior:
  - Mantiene el botón de guardado habilitado cuando hay al menos 1 alimento y los totales actualizados.

### 2.4 Modificaciones en `FoodScanReviewScreen`
- En `ScannedFoodItemCard`:
  - Se añade soporte para abrir `EditItemNameDialog` y cambiar el nombre del alimento detectado antes de guardar por primera vez.

---

## 3. Persistencia y Casos de Uso

1. **`EditMealViewModel`:**
   - Recibe `FoodCatalogRepository` para ejecutar búsquedas de catálogo dentro del ViewModel o en un caso de uso liviano `SearchFoodCatalogUseCase`.
   - Al invocar `OnItemNameChanged`:
     ```kotlin
     val updated = _uiState.value.items.toMutableList()
     updated[event.itemIndex] = updated[event.itemIndex].copy(name = event.newName.trim())
     _uiState.update { it.copy(items = updated) }
     ```
   - Al invocar `OnItemAdded`:
     ```kotlin
     val updated = _uiState.value.items + event.item
     val newSummary = NutritionSummary.fromItems(updated)
     _uiState.update { it.copy(items = updated, summary = newSummary) }
     ```
   - Al invocar `OnSaveMealClicked`:
     - Invoca `UpdateMealEntryUseCase` o `MealRepository.updateMealEntry()` con los ítems y totales actualizados.
     - Si la comida tenía `healthConnectRecordId`, se actualiza el registro en Health Connect.
