# Diseño Técnico: Metas Nutricionales, Suplementos Diarios y Rachas

## 1. Arquitectura y Modelos de Dominio

### 1.1 Modelo de Rachas (`DailyStreak`)
Se define en la capa de `domain` pura:

```kotlin
package com.calculadoracalorias.app.domain.model

data class DailyStreak(
    val currentStreakDays: Int,
    val hasBreakfastToday: Boolean,
    val hasLunchToday: Boolean,
    val hasDinnerToday: Boolean
) {
    val isTodayCompleted: Boolean get() = hasBreakfastToday && hasLunchToday && hasDinnerToday
    val missingMealsToday: List<MealCategory> get() = buildList {
        if (!hasBreakfastToday) add(MealCategory.DESAYUNO)
        if (!hasLunchToday) add(MealCategory.ALMUERZO)
        if (!hasDinnerToday) add(MealCategory.ONCE_CENA)
    }
}
```

#### Regla de Negocio para el Cálculo de Racha:
Un día se considera "Día Bueno / Cumplido" si:
- Existe al menos una comida registrada en `MealCategory.DESAYUNO`.
- Existe al menos una comida registrada en `MealCategory.ALMUERZO`.
- Existe al menos una comida registrada en `MealCategory.ONCE_CENA`.

El caso de uso `CalculateDailyStreakUseCase` consulta las comidas de los últimos 60 días en `MealRepository` y calcula la racha consecutiva:
1. Evalúa el día de hoy: si ya tiene las 3 comidas, suma hoy a la racha y continúa hacia atrás (ayer, anteayer...).
2. Si hoy aún no se completan las 3 comidas, evalúa si ayer se completaron. Si ayer fue día cumplido, la racha activa se mantiene (iniciando el conteo desde ayer) a la espera de que el usuario complete las comidas de hoy. Si ayer no fue día cumplido, la racha es 0.

---

### 1.2 Modelo y Persistencia de Suplementos (`Supplement`)

#### Modelo de Dominio:
```kotlin
package com.calculadoracalorias.app.domain.model

data class Supplement(
    val id: String,
    val name: String,
    val dosageDescription: String,
    val calories: Double,
    val fatGrams: Double,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val isTakenToday: Boolean = false
)
```

Por defecto, se incluye:
- `id = "omega_3"`
- `name = "Omega 3"`
- `dosageDescription = "2 cápsulas"`
- `calories = 18.0`
- `fatGrams = 2.0`

#### Persistencia en Room (`SupplementLogEntity`):
```kotlin
@Entity(
    tableName = "supplement_logs",
    primaryKeys = ["date", "supplementId"]
)
data class SupplementLogEntity(
    val date: String,            // Formato "YYYY-MM-DD"
    val supplementId: String,
    val takenTimestamp: Long
)
```

Un repositorio `SupplementRepository` expone:
- `getSupplementsForDate(date: LocalDate): Flow<List<Supplement>>`
- `toggleSupplementTaken(date: LocalDate, supplementId: String, isTaken: Boolean)`

---

### 1.3 Configuración de Metas en DataStore (`DailyMacroBudget`)
`UserPreferencesRepository` ya cuenta con los campos:
- `TARGET_CALORIES`
- `TARGET_PROTEIN`
- `TARGET_CARBS`
- `TARGET_FAT`
- Método `setDailyBudget(calories, protein, carbs, fat)`

En esta propuesta conectamos la interfaz de usuario en `SettingsScreen` para permitir la edición directa y validada de estos valores, con cálculo asistido si el usuario lo desea.

---

## 2. Componentes de UI (Jetpack Compose)

```
+-------------------------------------------------------------------------+
|                  VISTA GENERAL EN DAILYSUMMARYSCREEN                    |
+-------------------------------------------------------------------------+
|                                                                         |
|  [Selector de Fecha: Martes, 8 de Septiembre]                           |
|                                                                         |
|  +-------------------------------------------------------------------+  |
|  | 🔥 Racha: 5 días seguidos                                         |  |
|  | Hoy: [✓] Desayuno  [✓] Almuerzo  [ ] Once / Cena                  |  |
|  | 👉 ¡Te falta registrar Once / Cena para asegurar la racha de hoy! |  |
|  +-------------------------------------------------------------------+  |
|                                                                         |
|  [Tarjeta de Resumen Nutricional: Calorías y Macros vs Meta Diaria]     |
|                                                                         |
|  +-------------------------------------------------------------------+  |
|  | 💊 Mis Suplementos de hoy                                         |  |
|  | [✓] Omega 3 (2 cápsulas)                     Tomado a las 09:30   |  |
|  |     +18 kcal  •  2.0g grasas                                      |  |
|  +-------------------------------------------------------------------+  |
|                                                                         |
|  [Comidas del Día por Categoría: Desayuno, Almuerzo, Once/Cena, Colac.] |
|                                                                         |
+-------------------------------------------------------------------------+
```

### 2.1 Widget de Racha (`DailyStreakCard`)
- Ubicado en la parte superior del resumen diario para máxima visibilidad y motivación.
- Contenedor con gradiente sutil y tonalidad cálida (color primario / secundario).
- Fila con icono de fuego 🔥 animado o destacado y texto de días consecutivos.
- Mini-checklist horizontal con las 3 comidas principales y estado visual (check verde o círculo pendiente).
- Mensaje en español chileno contextual según el avance del día.

### 2.2 Widget de Suplementos (`DailySupplementsCard`)
- Tarjeta limpia que lista los suplementos activos.
- Checkbox táctil amplio con animación fluida al marcar/desmarcar.
- Muestra el nombre ("Omega 3"), la dosis ("2 cápsulas") y el aporte calórico/nutricional.
- Al marcar, se actualiza inmediatamente el registro local del día y se suma al balance calórico diario si corresponde.

### 2.3 Sección en `SettingsScreen`: "Meta Nutricional y Suplementos"
- **Tarjeta "Meta Nutricional Diaria":**
  - Campo numérico para Calorías Objetivo (ej. 2300 kcal).
  - Tres campos numéricos para Proteínas (g), Carbohidratos (g) y Grasas (g).
  - Indicador automático de coherencia calórica: informa las calorías resultantes de la suma de macros `(P*4 + C*4 + G*9)`.
  - Botón *"Equilibrar macros automáticamente"* (30% P, 40% C, 30% G) a partir de las calorías indicadas.
- **Tarjeta "Suplementos habituales":**
  - Interruptor para habilitar o deshabilitar el recordatorio/checklist de Omega 3 u otros suplementos en el resumen diario.
