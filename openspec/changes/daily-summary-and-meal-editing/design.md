# Diseño Técnico: Resumen Diario y Edición Sincronizada de Comidas

## 1. Arquitectura General y Flujo de Datos Unidireccional (UDF)

El diseño sigue estrictamente **Clean Architecture** con separación de responsabilidades y flujo unidireccional de datos:

```
[ Compose UI (DailySummaryScreen / EditMealScreen) ]
                     │  ▲
        Eventos (UI) │  │  Estado Inmutable (UiState)
                     ▼  │
          [ ViewModel (StateFlow) ]
                     │  ▲
                     ▼  │
          [ Casos de Uso (Domain) ]
             /               \
            ▼                 ▼
  [ MealRepository ]    [ HealthConnectRepository ]
         │                        │
         ▼                        ▼
[ Room (Local DB) ]      [ Health Connect Client ]
```

---

## 2. Capa de Dominio (Domain Layer)

La capa de dominio no posee dependencias del framework de Android y encapsula toda la lógica de negocio, recálculos y modelos esenciales.

### 2.1. Modelos de Dominio
- **`MealEntry`**:
  ```kotlin
  data class MealEntry(
      val id: Long,
      val category: MealCategory,
      val timestamp: Long,
      val items: List<ScannedFoodItem>,
      val summary: NutritionSummary,
      val healthConnectRecordId: String? = null
  )
  ```
- **`DailyMacroBudget`**:
  ```kotlin
  data class DailyMacroBudget(
      val targetCalories: Double,
      val targetProteinGrams: Double,
      val targetCarbsGrams: Double,
      val targetFatGrams: Double
  )
  ```
- **`DailySummary`**:
  ```kotlin
  data class DailySummary(
      val dateEpochDay: Long,
      val budget: DailyMacroBudget,
      val consumed: NutritionSummary,
      val remainingCalories: Double,
      val mealsByCategory: Map<MealCategory, List<MealEntry>>
  )
  ```

### 2.2. Casos de Uso (Use Cases)
1. **`GetDailyMealSummaryUseCase`**:
   - Recibe la fecha seleccionada (timestamp o epoch day).
   - Observa el flujo de comidas registradas para ese día desde `MealRepository`.
   - Obtiene el perfil de usuario / presupuesto calórico diario desde `UserPreferencesRepository`.
   - Calcula el total acumulado de calorías y macronutrientes, determinando las calorías restantes y el progreso porcentual.
   - Retorna un `Flow<DailySummary>`.

2. **`UpdateMealWithHealthSyncUseCase`**:
   - Recibe el `mealId`, la categoría, la fecha/hora y la lista actualizada de `ScannedFoodItem`.
   - Valida que la lista de alimentos no esté vacía y que las porciones sean positivas.
   - Recalcula el `NutritionSummary` usando las relaciones nutricionales estándar.
   - Persiste la actualización en Room a través de `MealRepository.updateMeal`.
   - Si la comida posee un `healthConnectRecordId`:
     - Invoca `HealthConnectRepository.updateNutritionRecord` con los nuevos valores.
     - Si la sincronización con Health Connect falla o el permiso fue revocado, no revierte la base de datos local y retorna un estado exitoso local con advertencia de sincronización.
   - Si no poseía `healthConnectRecordId` pero ahora hay permisos, intenta registrarlo y actualiza la referencia.

3. **`DeleteMealWithHealthSyncUseCase`**:
   - Recibe el `mealId` a eliminar.
   - Consulta el `healthConnectRecordId` asociado.
   - Si existe, invoca `HealthConnectRepository.deleteNutritionRecord(recordId)`.
   - Elimina la comida de Room mediante `MealRepository.deleteMeal(mealId)`.

---

## 3. Capa de Datos (Data Layer)

### 3.1. Persistencia Local con Room
- **Objeto de Relación `MealWithItems`**:
  ```kotlin
  data class MealWithItems(
      @Embedded val meal: MealEntryEntity,
      @Relation(
          parentColumn = "id",
          entityColumn = "mealEntryId"
      )
      val items: List<MealFoodItemEntity>
  )
  ```
- **Métodos en `MealDao`**:
  ```kotlin
  @Transaction
  @Query("SELECT * FROM meal_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
  fun getMealsWithItemsBetween(startTime: Long, endTime: Long): Flow<List<MealWithItems>>

  @Transaction
  @Query("SELECT * FROM meal_entries WHERE id = :mealId")
  suspend fun getMealWithItemsById(mealId: Long): MealWithItems?

  @Query("DELETE FROM meal_food_items WHERE mealEntryId = :mealId")
  suspend fun deleteItemsForMeal(mealId: Long)

  @Transaction
  suspend fun updateMealWithItems(meal: MealEntryEntity, items: List<MealFoodItemEntity>) {
      insertMeal(meal) // OnConflictStrategy.REPLACE actualiza la cabecera
      deleteItemsForMeal(meal.id)
      insertMealItems(items)
  }
  ```

### 3.2. Contratos de Repositorio
- **Ampliación de `MealRepository`**:
  ```kotlin
  interface MealRepository {
      suspend fun saveMeal(
          category: MealCategory,
          timestamp: Long,
          items: List<ScannedFoodItem>,
          healthConnectRecordId: String? = null
      ): Result<Long>

      fun getMealsForDay(startOfDayTimestamp: Long, endOfDayTimestamp: Long): Flow<List<MealEntry>>

      suspend fun getMealById(mealId: Long): Result<MealEntry?>

      suspend fun updateMeal(
          mealId: Long,
          category: MealCategory,
          timestamp: Long,
          items: List<ScannedFoodItem>,
          healthConnectRecordId: String?
      ): Result<Unit>

      suspend fun deleteMeal(mealId: Long): Result<Unit>
  }
  ```

### 3.3. Integración con Google Health Connect
- **Ampliación de `HealthConnectRepository`**:
  ```kotlin
  interface HealthConnectRepository {
      suspend fun isHealthConnectAvailable(): Boolean
      suspend fun hasWriteNutritionPermission(): Boolean
      suspend fun writeNutritionRecord(...): Result<String>
      suspend fun updateNutritionRecord(
          recordId: String,
          mealName: String,
          mealCategory: MealCategory,
          timestamp: Long,
          calories: Double,
          proteinGrams: Double,
          carbsGrams: Double,
          fatGrams: Double
      ): Result<Unit>
      suspend fun deleteNutritionRecord(recordId: String): Result<Unit>
  }
  ```
- **Implementación en `AndroidHealthConnectRepository`**:
  - `updateNutritionRecord`: Genera un `NutritionRecord` asignando `metadata = Metadata(id = recordId)` y llama a `client.updateRecords(listOf(record))`.
  - `deleteNutritionRecord`: Invoca `client.deleteRecords(NutritionRecord::class, recordIdsList = listOf(recordId), clientRecordIdsList = emptyList())`.

---

## 4. Capa de Presentación (Presentation Layer)

### 4.1. Pantalla de Resumen Diario (`DailySummaryScreen`)
- **Estado de UI (`DailySummaryUiState`)**:
  ```kotlin
  data class DailySummaryUiState(
      val selectedDate: LocalDate = LocalDate.now(),
      val isLoading: Boolean = true,
      val targetCalories: Double = 2000.0,
      val consumedCalories: Double = 0.0,
      val remainingCalories: Double = 2000.0,
      val targetProteinGrams: Double = 150.0,
      val consumedProteinGrams: Double = 0.0,
      val targetCarbsGrams: Double = 200.0,
      val consumedCarbsGrams: Double = 0.0,
      val targetFatGrams: Double = 65.0,
      val consumedFatGrams: Double = 0.0,
      val mealsByCategory: Map<MealCategory, List<MealEntry>> = emptyMap(),
      val errorMessage: String? = null
  )
  ```
- **Eventos de UI (`DailySummaryEvent`)**:
  - `OnPreviousDayClicked`: Navega al día anterior.
  - `OnNextDayClicked`: Navega al día siguiente.
  - `OnDateSelected(date: LocalDate)`: Selecciona una fecha específica.
  - `OnAddMealClicked(category: MealCategory)`: Abre la cámara / scanner para esa categoría.
  - `OnMealItemClicked(mealId: Long)`: Abre la pantalla de edición para esa comida.

### 4.2. Pantalla de Edición de Comida (`EditMealScreen`)
- **Estado de UI (`EditMealUiState`)**:
  ```kotlin
  data class EditMealUiState(
      val mealId: Long = 0L,
      val category: MealCategory = MealCategory.DESAYUNO,
      val timestamp: Long = 0L,
      val items: List<ScannedFoodItem> = emptyList(),
      val summary: NutritionSummary = NutritionSummary(0.0, 0.0, 0.0, 0.0),
      val isSaving: Boolean = false,
      val isDeleting: Boolean = false,
      val isSavedSuccessfully: Boolean = false,
      val isDeletedSuccessfully: Boolean = false,
      val errorMessage: String? = null
  )
  ```
- **Eventos de UI (`EditMealEvent`)**:
  - `OnServingGramsChanged(index: Int, newGrams: Double)`: Recalcula inmediatamente la porción.
  - `OnRemoveItem(index: Int)`: Quita el alimento de la lista.
  - `OnSaveMealClicked`: Dispara `UpdateMealWithHealthSyncUseCase`.
  - `OnDeleteMealClicked`: Dispara diálogo de confirmación y posterior `DeleteMealWithHealthSyncUseCase`.
  - `OnDismiss`: Cierra la pantalla y regresa al resumen diario.

---

## 5. Localización y Tono Chileno (`es-CL`)

| Clave / Elemento | Texto en Interfaz |
| :--- | :--- |
| Tarjeta Desayuno | "Desayuno" |
| Tarjeta Almuerzo | "Almuerzo" |
| Tarjeta Once / Cena | "Once / Cena" |
| Tarjeta Colaciones | "Colaciones" |
| Calorías restantes | "Te quedan X kcal" |
| Exceso calórico | "Te pasaste por X kcal" |
| Botón guardar | "Guardar cambios" |
| Diálogo eliminar | "¿Quieres eliminar este registro?" |
| Confirmar eliminar | "Eliminar comida" |
| Cancelar diálogo | "Volver" |
