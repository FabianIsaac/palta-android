# Diseño Técnico: Horarios Inteligentes Configurables y Mejoras de UX en Revisión

## 1. Arquitectura y Modelos de Datos

### 1.1 Modelo de Ventanas Horarias (`MealTimeWindows`)
Se crea un modelo inmutable en la capa de `domain` pura para representar las ventanas horarias de cada comida del día:

```kotlin
package com.calculadoracalorias.app.domain.model

import java.time.LocalTime

data class MealTimeWindows(
    val breakfastStart: LocalTime = LocalTime.of(6, 0),
    val breakfastEnd: LocalTime = LocalTime.of(11, 30),
    val lunchStart: LocalTime = LocalTime.of(11, 30),
    val lunchEnd: LocalTime = LocalTime.of(16, 0),
    val dinnerStart: LocalTime = LocalTime.of(16, 0),
    val dinnerEnd: LocalTime = LocalTime.of(22, 0)
) {
    fun detectCategory(time: LocalTime = LocalTime.now()): MealCategory {
        return when {
            !time.isBefore(breakfastStart) && time.isBefore(breakfastEnd) -> MealCategory.DESAYUNO
            !time.isBefore(lunchStart) && time.isBefore(lunchEnd) -> MealCategory.ALMUERZO
            !time.isBefore(dinnerStart) && time.isBefore(dinnerEnd) -> MealCategory.ONCE_CENA
            else -> MealCategory.COLACIONES
        }
    }
}
```

### 1.2 Persistencia en DataStore (`UserPreferencesRepository`)
Se agregan las claves de preferencias (en minutos desde medianoche o formato `"HH:mm"`) para persistir la configuración del usuario:
- `BREAKFAST_START_MINUTE` (por defecto: 360 = 06:00)
- `BREAKFAST_END_MINUTE` (por defecto: 690 = 11:30)
- `LUNCH_START_MINUTE` (por defecto: 690 = 11:30)
- `LUNCH_END_MINUTE` (por defecto: 960 = 16:00)
- `DINNER_START_MINUTE` (por defecto: 960 = 16:00)
- `DINNER_END_MINUTE` (por defecto: 1320 = 22:00)

Se expone en `UserPreferences` el objeto `mealTimeWindows: MealTimeWindows` y el método correspondiente:
```kotlin
suspend fun updateMealTimeWindows(windows: MealTimeWindows)
```

---

## 2. Flujo de Navegación y Detección Automática

```
+-------------------------------------------------------------------------+
|                  FLUJO DE DETECCIÓN INTELIGENTE DE HORA                 |
+-------------------------------------------------------------------------+
|                                                                         |
|  Usuario pulsa "Escanear / Agregar comida"                              |
|          |                                                              |
|          v                                                              |
|  [MainActivity / ViewModel]                                             |
|  Obtiene mealTimeWindows de UserPreferences                             |
|  Calcula: category = mealTimeWindows.detectCategory(LocalTime.now())     |
|          |                                                              |
|          +---> Cámara: Sugerencia activa para la captura                |
|          +---> Entrada de Texto: Pre-asigna la categoría detectada       |
|          +---> Registro Manual: Abre revisión con la categoría sugerida |
|                                                                         |
+-------------------------------------------------------------------------+
```

---

## 3. Componentes de UI y Rediseño Visual

### 3.1 Selector Compacto de Categoría de Comida (`MealCategorySelector`)
En reemplazo del `FlowRow` con chips dispersos, se crea un componente seleccionable limpio basado en Material 3:
- Permite seleccionar entre:
  - ☕ Desayuno
  - 🍲 Almuerzo
  - 🫖 Once / Cena
  - 🍎 Colaciones
- Muestra un subtítulo o chip contextual sutil: *(Detectado automáticamente por la hora: 13:45)* si coincide con la sugerencia horaria.
- Al pulsar, despliega un menú elegante o selector segmentado donde cambiar de categoría requiere solo un toque.

### 3.2 Rediseño de Acciones en "Alimentos detectados"
Para erradicar la deformación y compresión de botones:
1. **Fila de Título:** Contiene únicamente el título de la sección y el contador de ítems (`Alimentos detectados (2)`).
2. **Fila de Acciones:** Inmediatamente debajo de la lista de alimentos (o bajo el encabezado en una fila horizontal dedicada con pesos equilibrados):
   - Botón secundario `+ Agregar alimento` (con ancho suficiente y texto ininterrumpido).
   - Botón tonal/outline `✨ Describir más con IA`.
   Ambos con altura estándar de 44.dp y `singleLine = true`, garantizando que jamás colapsen verticalmente.

### 3.3 Estado de Carga Inmediato en Entrada por Texto
En `FoodScanReviewViewModel.onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage)`:
- Se actualiza el estado con:
  ```kotlin
  _uiState.update {
      it.copy(
          isLoading = true,
          isAnalyzingText = true,
          errorMessage = null,
          lastRawDescription = description
      )
  }
  ```
- En `FoodScanReviewScreen`, si `uiState.isLoading` es verdadero:
  - Si `uiState.isAnalyzingText == true`, muestra el título: *"Interpretando tu comida con IA..."* y la descripción ingresada entre comillas.
  - Si es captura de imagen, muestra: *"Analizando tu plato con IA..."*.
  De esta forma, la transición desde el modal de texto es inmediata y el usuario ve animación continua.

### 3.4 Configuración de Horarios en `SettingsScreen`
Se añade una tarjeta de configuración:
- Card *"Horarios habituales de comida"*.
- Cada comida principal cuenta con dos campos interactivos: [Hora inicio] y [Hora fin].
- Al tocar una hora se despliega el `TimePickerDialog` estándar de Material 3 con formato 24 horas (`is24Hour = true`).
- Botón *"Restablecer horarios estándar"* para volver a los valores chilenos por defecto.

### 3.5 Limpieza en `FoodCameraScreen`
Se retira el `IconButton` de Ajustes ubicado en la fila superior derecha (`onNavigateToSettings`), dejando la interfaz de captura minimalista y enfocada.
