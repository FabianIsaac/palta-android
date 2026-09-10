# Diseño Técnico: health-connect-activity-and-weight-sync

## 1. Arquitectura y Principios de Diseño

El diseño sigue estrictamente los principios de **Clean Architecture**, **Unidirectional Data Flow (UDF)** y **Offline-First**:
- **Capa Domain pura:** Sin dependencias de Android framework ni del SDK de Health Connect. Expone modelos inmutables y contratos claros.
- **Capa Data:** `AndroidHealthConnectRepository` interactúa con `HealthConnectClient` utilizando agregaciones nativas (`AggregateRecordsRequest`) para maximizar el rendimiento y la eficiencia energética al consultar pasos y calorías activas.
- **Capa Presentation:** `DailySummaryViewModel` y `SettingsViewModel` consumen los casos de uso y emiten estados inmutables `UiState` consumidos por Jetpack Compose.

---

## 2. Modelos de Dominio y Contratos

### A. Modelos de Dominio (`domain/model/`)

#### `DailyHealthActivity.kt`
```kotlin
package com.calculadoracalorias.app.domain.model

import java.time.LocalDate

data class DailyHealthActivity(
    val date: LocalDate,
    val burnedCalories: Double = 0.0,
    val stepsCount: Long = 0L
)
```

#### `HealthWeightRecord.kt`
```kotlin
package com.calculadoracalorias.app.domain.model

import java.time.Instant

data class HealthWeightRecord(
    val weightKg: Double,
    val recordedAt: Instant
)
```

### B. Contrato de Repositorio (`domain/repository/HealthConnectRepository.kt`)

```kotlin
interface HealthConnectRepository {
    // Métodos existentes de nutrición
    suspend fun isHealthConnectAvailable(): Boolean
    suspend fun hasWriteNutritionPermission(): Boolean
    suspend fun writeNutritionRecord(...): Result<String>
    suspend fun updateNutritionRecord(...): Result<Unit>
    suspend fun deleteNutritionRecord(...): Result<Unit>

    // Nuevos métodos de lectura de actividad física y biometría
    suspend fun hasActivityPermissions(): Boolean
    suspend fun hasWeightPermission(): Boolean
    suspend fun getDailyActivity(date: LocalDate): Result<DailyHealthActivity>
    suspend fun getLatestWeight(): Result<HealthWeightRecord?>
}
```

### C. Casos de Uso (`domain/usecase/`)

1. **`GetDailyHealthActivityUseCase`:**
   - Recibe la `LocalDate`.
   - Consulta si la sincronización de actividad está habilitada en `UserPreferencesRepository`.
   - Si está activa y cuenta con permisos, consulta `HealthConnectRepository.getDailyActivity(date)`.
   - Retorna `DailyHealthActivity(date, burnedCalories, stepsCount)`. Si no hay permisos o está inactiva, retorna ceros sin lanzar error.

2. **`GetLatestHealthWeightUseCase`:**
   - Comprueba permiso `READ_WEIGHT`.
   - Llama a `HealthConnectRepository.getLatestWeight()`.
   - Retorna el pesaje más reciente registrado por la balanza inteligente o app externa.

---

## 3. Implementación en Capa de Datos (`AndroidHealthConnectRepository`)

### A. Permisos requeridos
```kotlin
val ACTIVITY_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class)
)

val WEIGHT_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(WeightRecord::class)
)
```

### B. Agregación de Actividad Diaria
Para un día determinado (`LocalDate`), se define el intervalo de tiempo entre el inicio del día `00:00:00` y el fin del día `23:59:59.999` en la zona horaria del sistema:
```kotlin
override suspend fun getDailyActivity(date: LocalDate): Result<DailyHealthActivity> {
    val client = healthConnectClientProvider() 
        ?: return Result.success(DailyHealthActivity(date))

    val zoneId = ZoneId.systemDefault()
    val startTime = date.atStartOfDay(zoneId).toInstant()
    val endTime = date.plusDays(1).atStartOfDay(zoneId).toInstant()

    return try {
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    StepsRecord.COUNT_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        val burnedCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L

        Result.success(DailyHealthActivity(date, burnedCalories, steps))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### C. Lectura de Peso más Reciente
```kotlin
override suspend fun getLatestWeight(): Result<HealthWeightRecord?> {
    val client = healthConnectClientProvider() ?: return Result.success(null)

    return try {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.before(Instant.now()),
            ascendingOrder = false,
            pageSize = 1
        )
        val response = client.readRecords(request)
        val latestRecord = response.records.firstOrNull()

        val result = latestRecord?.let {
            HealthWeightRecord(
                weightKg = it.weight.inKilograms,
                recordedAt = it.time
            )
        }
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 4. Diseño de Interfaz de Usuario y Experiencia

### A. Resumen Diario (`DailySummaryScreen.kt`)
Se añade una tarjeta informativa estilizada de **Actividad y Ejercicio** debajo del desglose calórico:
- **Icono de Fuego (🔥):** Muestra las calorías quemadas por actividad (ej. `320 kcal quemadas`).
- **Icono de Pasos (👣):** Muestra el total de pasos del día (ej. `7.430 pasos`).
- **Cálculo de Calorías Restantes:**
  - Si la opción *"Sumar calorías quemadas al presupuesto"* está **activa**:
    $$\text{Restantes} = \text{Meta} - \text{Consumidas} + \text{Quemadas}$$
    Con indicación visual que aclare el desglose (`Meta: 2000 - Consumidas: 1600 + Ejercicio: 320 = 720 restantes`).
  - Si está **inactiva** (por defecto):
    $$\text{Restantes} = \text{Meta} - \text{Consumidas}$$
    Y la tarjeta muestra la actividad como métrica complementaria de progreso físico.

### B. Ajustes y Perfil (`SettingsScreen.kt`)
Nueva sección: **"Salud y Dispositivos Conectados"**:
1. **Interruptor: Actividad física y pasos (Health Connect):**
   - Habilita la lectura de pasos y calorías de relojes inteligentes o podómetro.
   - Solicita permisos con diálogo explicativo si no han sido otorgados.
2. **Interruptor: Sumar calorías quemadas al presupuesto diario:**
   - Explica de forma pedagógica que para pérdida de grasa estricta se recomienda mantener apagado.
3. **Tarjeta: Balanza inteligente (Sincronización de peso):**
   - Muestra el último peso detectado en Health Connect y cuándo se registró (ej. *"Último pesaje detectado: 74,5 kg (Hoy a las 08:30)"*).
   - Botón *"Actualizar peso"* para sincronizar directamente con el perfil y recalcular BMR/TDEE si aplica.
