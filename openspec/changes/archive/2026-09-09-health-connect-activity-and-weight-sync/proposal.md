# Propuesta: health-connect-activity-and-weight-sync

## 1. Contexto y Justificación

Actualmente, la aplicación interactúa con Android Health Connect exclusivamente en una dirección: exportando los registros nutricionales de las comidas consumidas (`writeNutritionRecord`, `updateNutritionRecord`). 

Sin embargo, para lograr un control calórico verdaderamente integral y un seguimiento preciso del progreso físico, existen dos métricas fundamentales que los usuarios ya registran en dispositivos externos o sensores del ecosistema de salud:
1. **Gasto energético activo (calorías quemadas) y pasos:** Medidos de forma continua por relojes inteligentes (Wear OS, Samsung Galaxy Watch, Garmin, Fitbit) o por el podómetro del teléfono.
2. **Peso corporal:** Registrado de manera automática por balanzas inteligentes (Withings, Xiaomi Mi Body Composition Scale, Huawei, etc.) o ingresado en aplicaciones de seguimiento de salud.

Al leer estas métricas desde Health Connect, la app puede:
- Mostrar el balance energético real del día (**Consumidas vs. Quemadas**).
- Ofrecer la opción de ajustar las calorías restantes según el ejercicio realizado ($\text{Restantes} = \text{Meta} - \text{Consumidas} + \text{Quemadas}$).
- Mantener actualizado el peso corporal del perfil sin fricción ni necesidad de ingresarlo manualmente tras cada pesaje.

---

## 2. Alcance (Scope)

### Dentro del Alcance (In-Scope)
- **Permisos de lectura en Health Connect:**
  - `READ_ACTIVE_CALORIES_BURNED` (Calorías quemadas por actividad física).
  - `READ_STEPS` (Total de pasos diarios).
  - `READ_WEIGHT` (Peso corporal).
  - Diálogo amigable e intuitivo de explicación de permisos con lenguaje chileno claro.
- **Visualización en Resumen Diario (`DailySummaryScreen`):**
  - Componente de actividad física diaria: muestra calorías activas quemadas y cantidad de pasos para el día seleccionado.
  - Indicador visual claro del balance calórico neto.
  - Opción configurable en Ajustes: *"Sumar calorías quemadas al presupuesto diario"*. 
    - Desactivado por defecto (recomendación para metas de pérdida de grasa estricta).
    - Si se activa: $\text{Calorías Restantes} = \text{Meta} - \text{Consumidas} + \text{Quemadas}$.
- **Sincronización de Peso Corporal (`SettingsScreen` / Perfil):**
  - Consulta del registro de peso más reciente disponible en Health Connect.
  - Botón o switch de sincronización de peso: detecta si hay un nuevo pesaje en la balanza inteligente y permite actualizar el peso de biometría del usuario con un toque.
  - Visualización de la fecha y hora del último pesaje detectado.
- **Persistencia en DataStore:**
  - Preferencias de usuario para habilitar/deshabilitar la lectura de actividad física.
  - Preferencia para incluir o no las calorías quemadas en el cálculo del presupuesto restante.
  - Preferencia para la sincronización de peso.
- **Idioma y Localización:**
  - 100% Español Chileno (`es-CL`) sin voseo (*"revisa", "conecta", "actualiza", "tus pasos"*).
- **Operación Offline-First:**
  - Health Connect opera íntegramente en el dispositivo local mediante IPC de Android, sin requerir conexión a internet ni servidores externos.

### Fuera del Alcance (Out-of-Scope)
- Escritura de pasos o peso hacia Health Connect (la app actúa como consumidora de estas métricas, no como fuente de registro de pasos o balanza).
- Servicios en segundo plano continuos (Foreground Services) que consuman batería; la lectura se efectúa de manera reactiva y bajo demanda al consultar la fecha en el resumen o al ingresar a ajustes.
- Lectura de ritmo cardíaco, fases de sueño o saturación de oxígeno (se mantiene el foco estricto en energía, déficit/superávit y biometría de peso).

---

## 3. Impacto en Base de Datos y Preferencias

- **Room Database:** No requiere modificaciones directas en el esquema de `MealEntryEntity` ni `MealFoodItemEntity`.
- **Jetpack DataStore (`UserPreferences`):**
  - `healthConnectActivitySyncEnabled: Boolean = true`
  - `includeBurnedCaloriesInBudget: Boolean = false`
  - `healthConnectWeightSyncEnabled: Boolean = true`
  - `lastSyncedWeightKg: Double? = null`
  - `lastSyncedWeightTimestamp: Long? = null`

---

## 4. Componentes y Capas Afectadas

- **Capa Data:**
  - `AndroidHealthConnectRepository`:
    - Métodos para consultar permisos de actividad y peso.
    - Lectura agregada de `ActiveCaloriesBurnedRecord` y `StepsRecord` para un rango de fecha (`LocalDate`).
    - Lectura del último `WeightRecord` registrado.
- **Capa Domain:**
  - `HealthConnectRepository`: Declaración de nuevos contratos.
  - Modelos de dominio: `DailyHealthActivity` y `HealthWeightRecord`.
  - Casos de uso:
    - `GetDailyHealthActivityUseCase`: Calcula calorías quemadas y pasos del día.
    - `GetLatestHealthWeightUseCase`: Obtiene el pesaje más reciente de Health Connect.
- **Capa Presentation:**
  - `DailySummaryContract.kt`: Nuevos campos en `DailySummaryUiState` (`burnedCalories`, `stepsCount`, `isActivitySyncEnabled`, `includeBurnedInBudget`).
  - `DailySummaryViewModel.kt`: Carga reactiva de la actividad al cambiar de fecha.
  - `DailySummaryScreen.kt`: Nueva tarjeta de actividad física integrada con Material Design 3.
  - `SettingsContract.kt` y `SettingsViewModel.kt`: Opciones de configuración de actividad y sincronización de balanza inteligente.
  - `SettingsScreen.kt`: Controles y retroalimentación visual para la sincronización de salud y balanza.
