# Diseño Técnico: Catálogo de Suplementos con IA, Resumen Diario y Navegación Simétrica

## 1. Arquitectura y Modelado de Datos

El diseño sigue estrictamente Clean Architecture con flujo unidireccional de datos (UDF) y separación de responsabilidades:

```
[ Compose UI: SupplementsScreen ]
       |  (SupplementsEvent: OnEstimate, OnSave, OnDelete, OnToggleActive)
       v
[ SupplementsViewModel ]
       |  (Llama UseCases y expone SupplementsUiState)
       v
[ Domain Layer (Pura) ]
  • EstimateSupplementNutritionUseCase
  • GetSupplementsUseCase / SaveSupplementUseCase / DeleteSupplementUseCase
       |
       v
[ Data Layer ]
  • SupplementRepository (LocalSupplementRepository)
       |--> SupplementDao (Room: tabla "supplements")
       |--> SupplementLogDao (Room: tabla "supplement_logs")
       |--> RemoteSupplementAnalyzer (MiniMax LLM)
       |--> LocalSupplementCatalog (Heurística Offline)
```

---

### 1.1 Modelos de Dominio (`domain/model`)

Se amplía el modelo `Supplement` existente para incluir metadatos de configuración:

```kotlin
package com.calculadoracalorias.app.domain.model

data class Supplement(
    val id: String,
    val name: String,
    val dosageDescription: String,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val isActive: Boolean = true,
    val isCustom: Boolean = false,
    val isTakenToday: Boolean = false
) {
    companion object {
        val PRECONFIGURED_SUPPLEMENTS = listOf(
            Supplement(
                id = "creatina_monohidrato",
                name = "Creatina Monohidrato",
                dosageDescription = "5g",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "omega_3",
                name = "Omega 3",
                dosageDescription = "2 cápsulas",
                calories = 18.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 2.0,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "citrato_magnesio",
                name = "Citrato de Magnesio",
                dosageDescription = "400mg",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "proteina_whey",
                name = "Proteína Whey",
                dosageDescription = "1 scoop (30g)",
                calories = 120.0,
                proteinGrams = 24.0,
                carbsGrams = 2.0,
                fatGrams = 1.5,
                isActive = true,
                isCustom = false
            ),
            Supplement(
                id = "multivitaminico",
                name = "Multivitamínico",
                dosageDescription = "1 comprimido",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = false,
                isCustom = false
            )
        )
    }
}
```

---

### 1.2 Persistencia Room (`SupplementEntity` y `SupplementDao`)

#### Entidad Room:
```kotlin
package com.calculadoracalorias.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosageDescription: String,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val isActive: Boolean,
    val isCustom: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### DAO:
```kotlin
package com.calculadoracalorias.app.data.local.dao

import androidx.room.*
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements ORDER BY isCustom ASC, name ASC")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE isActive = 1 ORDER BY isCustom ASC, name ASC")
    fun getActiveSupplements(): Flow<List<SupplementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(supplement: SupplementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(supplements: List<SupplementEntity>)

    @Query("UPDATE supplements SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM supplements WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

---

### 1.3 Estimación Asistida con IA (`EstimateSupplementNutritionUseCase`)

1. **Consulta Remota (MiniMax Cloud):**
   - Prompt de sistema especializado en suplementos deportivos y micronutrientes:
     * Interpreta la sustancia (ej. "Creatina", "Whey Protein Isolate", "Magnesio", "Cafeína", "Pre-entreno").
     * Si no tiene calorías (vitaminas, minerales, creatina pura), asigna 0.0 a todo.
     * Si contiene macronutrientes (proteínas en polvo, aminoácidos calóricos, aceites como omega 3 o MCT), extrae o estima los gramos reales por dosis.
     * Genera una dosis sugerida estándar si el usuario no la escribió (ej: "5g (1 scoop)").
2. **Respaldo Local Heurístico (Offline-First):**
   - Si no hay API key o no hay conexión de red, busca en un mapa de palabras clave locales (*creatina*, *magnesio*, *proteína*, *whey*, *omega*, *multivitamínico*, *colágeno*, *zinc*, *bcaa*).

---

## 2. Componentes de Interfaz y Navegación

### 2.1 Destinos de la Aplicación (`AppDestination`)
```kotlin
enum class AppDestination {
    SUMMARY,      // Diario
    STATISTICS,   // Estadísticas (Nueva)
    CAMERA,       // Cámara (FAB central)
    SUPPLEMENTS,  // Suplementos (Nueva)
    SETTINGS,     // Ajustes
    REVIEW,       // Subpantalla de revisión
    EDIT_MEAL     // Subpantalla de edición
}
```

### 2.2 Barra de Navegación Simétrica (`AppBottomNavigationBar`)
La barra de navegación aloja 4 botones distribuidos equitativamente a ambos lados del botón flotante central:

```
+-----------------------------------------------------------------------------------+
|                                      [ 📷 ]                                       |
|     [ 📖 ]              [ 📊 ]                     [ 💊 ]              [ ⚙️ ]     |
|     Diario          Estadísticas                Suplementos           Ajustes     |
+-----------------------------------------------------------------------------------+
```

- Espacio izquierdo: 2 botones (`Diario` y `Estadísticas`).
- Cuna central: `FloatingActionButton` de Cámara.
- Espacio derecho: 2 botones (`Suplementos` y `Ajustes`).

### 2.3 Reorganización en `DailySummaryScreen`
En el `LazyColumn` principal:
1. `DateSelectorHeader` (Selector de fecha).
2. `DailyStreakCard` (Racha de días buenos).
3. `CalorieMacroOverviewCard` (Anillo calórico y barras de macros).
4. `ChileanMealCategoryCard` (Desayuno, Almuerzo, Once / Cena, Colaciones).
5. **`DailySupplementsCard` (Suplementos del día):** Ubicado estrictamente al final, después de todas las comidas.
   - Solo contiene la lista de checkboxes de los suplementos activos para la fecha.
   - Sin botones de navegación ni configuración.

### 2.4 Pantalla de Suplementos (`SupplementsScreen`)
- **TopAppBar:** Título "Mis Suplementos" y botón de retroceso.
- **Tarjeta 1: Sugerencias Rápidas:** Chips con suplementos preconfigurados que permiten activarlos o desactivarlos con un solo toque.
- **Tarjeta 2: Agregar con IA:**
  - Campo de texto para nombre del suplemento.
  - Botón interactivo "✨ Estimar macros con IA".
  - Campos numéricos editables: Dosis, Calorías, Proteína (g), Carbohidratos (g), Grasas (g).
  - Botón "Guardar suplemento".
- **Tarjeta 3: Rutina Activa:** Lista de suplementos configurados con interruptor de activación y botón para eliminar.

### 2.5 Pantalla de Estadísticas (`StatisticsScreen`)
- Vista con TopAppBar "Estadísticas".
- Contenedor centrado con icono descriptivo, título y texto explicativo en español chileno indicando que los reportes de evolución calórica y macronutrientes estarán disponibles en una próxima actualización.

---

## 3. Flujo de Datos Unidireccional (UDF)

```
[ Usuario marca checkbox en Diario ]
                 |
                 v
  DailySummaryEvent.OnToggleSupplement(supplementId, isTaken)
                 |
                 v
  DailySummaryViewModel.onEvent(...)
                 |
                 v
  SupplementRepository.toggleSupplementTaken(date, supplementId, isTaken)
                 |
                 v
  SupplementLogDao.insertLog(...) / deleteLog(...)
                 |
                 v
  Flow<List<Supplement>> emite la lista actualizada
                 |
                 v
  DailySummaryViewModel recalcula calorías y macros sumando suplementos tomados
                 |
                 v
  DailySummaryUiState se actualiza reactivamente en Compose
```
