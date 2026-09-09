# Diseño Técnico: health-connect-history-import

## 1. Arquitectura y Flujo Unidireccional (UDF)

La solución respeta los principios de Clean Architecture y la constitución del proyecto:
- La capa `domain` define el contrato y el caso de uso sin depender de clases de `android.*` ni del SDK de Health Connect.
- La capa `data` implementa la lectura con `HealthConnectClient` utilizando `ReadRecordsRequest`.
- La persistencia se realiza a través de `MealDao`, encapsulando la inserción de `MealEntryEntity` y los ítems asociados `MealFoodItemEntity`.
- `SettingsViewModel` expone el estado inmutable y maneja el flujo de permisos y feedback.

---

## 2. Diagrama de Flujo de Importación

```
+-----------------------------------------------------------------------------------------+
|                        Flujo de Importación de Historial                                |
|                                                                                         |
|  [SettingsScreen]                                                                       |
|         │                                                                               |
|         ▼ Usuario selecciona rango (7 o 30 días) y pulsa "Importar historial"          |
|  SettingsViewModel.onEvent(OnImportHealthHistoryClicked(days))                          |
|         │                                                                               |
|         ▼ Verifica permiso READ_NUTRITION                                               |
|         ├─ Si no tiene permiso ──> Dispara solicitud de permisos en la UI               |
|         └─ Si tiene permiso:                                                            |
|                 │                                                                       |
|                 ▼                                                                       |
|  ImportHealthConnectHistoryUseCase(startTime, endTime)                                  |
|         │                                                                               |
|         ├──> HealthConnectRepository.readNutritionRecords(startTime, endTime)           |
|         │         │                                                                     |
|         │         ▼ (Filtra registros propios: packageName != appPackage)               |
|         │         └─ Devuelve List<ExternalNutritionRecord>                             |
|         │                                                                               |
|         ├──> MealDao.getAllHealthConnectRecordIds()                                     |
|         │         │                                                                     |
|         │         ▼ (Descarta registros ya importados previamente)                      |
|         │                                                                               |
|         ├──> Mapea registros a MealEntryEntity + MealFoodItemEntity                      |
|         │         │                                                                     |
|         │         ▼ Asigna MealCategory según mealType o ventana horaria                |
|         │                                                                               |
|         ├──> MealDao.insertMealWithItems() para cada registro nuevo                     |
|         │                                                                               |
|         ▼ Retorna Result<Int> con cantidad de comidas importadas                        |
|                                                                                         |
|  SettingsViewModel actualiza estado:                                                    |
|  "¡Listo! Se importaron 12 comidas de los últimos 7 días."                              |
+-----------------------------------------------------------------------------------------+
```

---

## 3. Modelos de Dominio y Contratos

### A. Modelo de Dominio: `ExternalNutritionRecord`
```kotlin
package com.calculadoracalorias.app.domain.model

import java.time.Instant

data class ExternalNutritionRecord(
    val recordId: String,
    val name: String,
    val startTime: Instant,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val mealType: Int, // MealType de Health Connect o constante neutra
    val sourcePackage: String
)
```

### B. Contrato: `HealthConnectRepository`
```kotlin
interface HealthConnectRepository {
    // ... Métodos existentes ...
    suspend fun hasReadNutritionPermission(): Boolean
    suspend fun readNutritionRecords(startTime: Long, endTime: Long): Result<List<ExternalNutritionRecord>>
}
```

### C. Implementación en `AndroidHealthConnectRepository`
```kotlin
override suspend fun readNutritionRecords(startTime: Long, endTime: Long): Result<List<ExternalNutritionRecord>> {
    val client = healthConnectClientProvider() 
        ?: return Result.failure(IllegalStateException("Health Connect no está disponible."))

    return try {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.ofEpochMilli(startTime),
                Instant.ofEpochMilli(endTime)
            )
        )
        val response = client.readRecords(request)
        val ownPackage = context.packageName

        val externalRecords = response.records
            .filter { it.metadata.dataOrigin.packageName != ownPackage }
            .map { record ->
                ExternalNutritionRecord(
                    recordId = record.metadata.id,
                    name = record.name ?: "Comida externa",
                    startTime = record.startTime,
                    calories = record.energy?.inKilocalories ?: 0.0,
                    proteinGrams = record.protein?.inGrams ?: 0.0,
                    carbsGrams = record.totalCarbohydrate?.inGrams ?: 0.0,
                    fatGrams = record.totalFat?.inGrams ?: 0.0,
                    mealType = record.mealType,
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }

        Result.success(externalRecords)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 4. UI en Pantalla de Ajustes (`SettingsScreen.kt`)

Se incorpora una tarjeta especializada en `SettingsScreen`:
- **Título:** "Sincronización con Health Connect"
- **Descripción:** "Importa registros de comidas y macronutrientes creados por otras aplicaciones en los últimos días."
- **Selector:** SegmentedButton o Chips para "Últimos 7 días" (por defecto) o "Últimos 30 días".
- **Botón:** "Importar historial" con icono de sincronización y estado de carga mientras se procesa.
- **Mensaje de confirmación:** Indicador visual de éxito o advertencia si no se concedieron los permisos.
