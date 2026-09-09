# Tareas de Implementación: daily-goals-streaks-and-supplements

## Fase 1: Dominio y Pruebas Unitarias
- [x] 1.1 Crear el modelo de dominio `DailyStreak` en `com.calculadoracalorias.app.domain.model`.
- [x] 1.2 Crear el modelo de dominio `Supplement` con soporte para dosis, calorías y macronutrientes.
- [x] 1.3 Implementar el caso de uso `CalculateDailyStreakUseCase` evaluando las 3 comidas principales (`Desayuno`, `Almuerzo`, `Once / Cena`) a lo largo de los días en `MealRepository`.
- [x] 1.4 Escribir pruebas unitarias exhaustivas en `CalculateDailyStreakUseCaseTest` cubriendo rachas activas, días incompletos, días saltados y días en curso.

## Fase 2: Persistencia en Base de Datos y Preferencias
- [x] 2.1 Crear la entidad Room `SupplementLogEntity` y su correspondiente `SupplementLogDao`.
- [x] 2.2 Registrar la nueva entidad y DAO en `AppDatabase` (incrementando versión con migración segura o destructiva si corresponde a desarrollo).
- [x] 2.3 Crear la interfaz `SupplementRepository` y su implementación `LocalSupplementRepository`.
- [x] 2.4 Verificar que `UserPreferencesRepository.setDailyBudget()` persista correctamente los valores de calorías, proteína, carbos y grasa.

## Fase 3: ViewModels y Estados
- [x] 3.1 Actualizar `DailySummaryContract` con campos para `DailyStreak`, lista de `Supplement` del día y evento `OnToggleSupplement`.
- [x] 3.2 Integrar `CalculateDailyStreakUseCase` y `SupplementRepository` en `DailySummaryViewModel`.
- [x] 3.3 Conectar en `DailySummaryViewModel` el impacto calórico y de macronutrientes al marcar o desmarcar suplementos.
- [x] 3.4 Actualizar `SettingsScreen` (y su interacción en `MainActivity`) para cargar y guardar la meta diaria personalizada de calorías y macros.

## Fase 4: Componentes de UI y Pantallas
- [x] 4.1 Diseñar el widget `DailyStreakCard` con icono de fuego 🔥, mini-checklist de las 3 comidas del día y mensajes motivacionales en español chileno (`es-CL`).
- [x] 4.2 Diseñar el widget `DailySupplementsCard` con checkboxes táctiles, detalle de dosis y badge de macros de Omega 3.
- [x] 4.3 Integrar `DailyStreakCard` y `DailySupplementsCard` en `DailySummaryScreen`.
- [x] 4.4 Agregar la tarjeta interactiva de *"Meta Nutricional Diaria"* en `SettingsScreen` con campos numéricos y opción de cálculo/balance automático de macros.

## Fase 5: Verificación y Compilación
- [x] 5.1 Ejecutar la suite completa de pruebas unitarias con `./gradlew testDebugUnitTest`.
- [x] 5.2 Compilar el proyecto en modo depuración y verificar que no existan errores ni advertencias de UI.
- [x] 5.3 Compilar y enviar el APK generado a Telegram ejecutando `./scripts/send_apk_telegram.sh`.
