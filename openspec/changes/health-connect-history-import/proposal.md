# Propuesta: health-connect-history-import

## 1. Contexto y Justificación

Actualmente, la aplicación únicamente escribe registros en Android Health Connect cuando el usuario registra o edita comidas en la app (`writeNutritionRecord`, `updateNutritionRecord`). Sin embargo, muchos usuarios utilizan otras aplicaciones o dispositivos (como MyFitnessPal, Samsung Health, Lifesum, relojes inteligentes o básculas inteligentes) para registrar comidas antes de empezar a usar la aplicación, o en paralelo.

Poder leer e importar registros pasados de Health Connect permite que el usuario consolide su historial nutricional dentro de la app sin tener que volver a ingresar manualmente cada comida anterior.

---

## 2. Alcance (Scope)

### Dentro del Alcance (In-Scope)
- **Consulta de permisos de lectura de Health Connect:**
  - Comprobar y solicitar el permiso `READ_NUTRITION` (ya declarado en el `AndroidManifest.xml`).
- **Ventana temporal de lectura histórica:**
  - Consulta de registros de nutrición (`NutritionRecord`) para rangos temporales definidos por el usuario:
    - Últimos 7 días.
    - Últimos 30 días (límite estándar garantizado por las políticas de privacidad de Health Connect sin requerir el permiso especial de auditoría histórica).
- **Deduplicación inteligente (Anti-Duplicados):**
  - Omitir cualquier registro cuyo origen (`dataOrigin.packageName`) sea la propia aplicación (`com.calculadoracalorias.app`).
  - Omitir registros cuyo identificador de Health Connect (`record.metadata.id`) ya se encuentre registrado en la base de datos local Room (`meal_entries.healthConnectRecordId`).
- **Mapeo a categorías de comidas chilenas:**
  - Mapear `NutritionRecord.mealType` a `MealCategory`:
    - `MEAL_TYPE_BREAKFAST` ➔ `Desayuno`
    - `MEAL_TYPE_LUNCH` ➔ `Almuerzo`
    - `MEAL_TYPE_DINNER` ➔ `Once / Cena`
    - `MEAL_TYPE_SNACK` ➔ `Colaciones`
    - `MEAL_TYPE_UNKNOWN` o no especificado ➔ Deducción automática según la hora local del registro utilizando `MealTimeWindows`.
- **Experiencia de Usuario en Configuración (Ajustes):**
  - Nueva tarjeta o sección en la pantalla de Configuración (`SettingsScreen`): *"Sincronización con Health Connect"*.
  - Selector de rango de importación (7 días o 30 días).
  - Botón *"Importar historial de comidas"*.
  - Indicadores visuales de estado: cargando, resumen de comidas importadas (ej. *"Se importaron 14 comidas de los últimos 7 días"*), o mensaje informativo si no se encontraron registros nuevos.
- **Idioma y localización:**
  - Español Chileno (`es-CL`) estricto sin voseo en todos los textos, descripciones y mensajes de retroalimentación.

### Fuera del Alcance (Out-of-Scope)
- Sincronización continua o en segundo plano periódica mediante WorkManager (se implementa como una acción manual controlada por el usuario para evitar consumo innecesario de batería y conflictos de datos).
- Edición de registros remotos creados por otras aplicaciones en Health Connect (la app solo los lee y los guarda localmente).
- Solicitud del permiso de historial de más de 30 días (`READ_HEALTH_DATA_HISTORY`), manteniéndose en el estándar de privacidad de 30 días de Android.

---

## 3. Impacto en Base de Datos y Persistencia

- **Esquema Room:**
  - `MealEntryEntity` ya cuenta con el campo `healthConnectRecordId: String?`.
  - Se agregará una consulta en `MealDao`:
    ```kotlin
    @Query("SELECT healthConnectRecordId FROM meal_entries WHERE healthConnectRecordId IS NOT NULL")
    suspend fun getAllHealthConnectRecordIds(): List<String>
    ```
  - Esto permite filtrar en memoria o mediante consulta directa cualquier registro ya existente con costo $O(1)$.
- No se requieren migraciones destructivas ni cambios de versión de base de datos.

---

## 4. Componentes y Capas Afectadas

- **Capa Data:**
  - `AndroidHealthConnectRepository`: Implementar `hasReadNutritionPermission()` y `readNutritionRecords(startTime, endTime)`.
  - `MealDao`: Método para consultar IDs existentes de Health Connect.
- **Capa Domain:**
  - `HealthConnectRepository`: Ampliación de contrato.
  - Nuevo modelo de dominio `ExternalNutritionRecord`.
  - Nuevo caso de uso: `ImportHealthConnectHistoryUseCase`.
- **Capa Presentation:**
  - `SettingsContract.kt`: Nuevos estados (`isImportingHealthHistory`, `importedMealsCount`) y eventos (`OnImportHealthHistoryClicked(rangeDays: Int)`).
  - `SettingsViewModel.kt`: Manejo de la lógica de importación y actualización de retroalimentación al usuario.
  - `SettingsScreen.kt`: Nueva sección interactiva de importación de historial.
