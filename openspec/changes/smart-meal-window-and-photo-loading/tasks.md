# Tareas de Implementación: Ventana Inteligente de Comidas y Carga Inmediata de Foto

## Fase 1: Capa de Dominio y Lógica de Consolidación (30 Minutos)

- [x] 1.1 Extender `MealRepository` con método para consultar la comida más reciente de una categoría en un rango de tiempo (`getMostRecentMealForCategory`).
- [x] 1.2 Implementar en `RoomMealRepository` la consulta Room correspondiente.
- [x] 1.3 Actualizar `SaveMealWithHealthSyncUseCase` para aplicar la regla de los 30 minutos:
  - Si existe comida previa en la misma categoría y `(timestamp - recentMeal.timestamp) <= 30 * 60 * 1000L`, consolidar uniendo alimentos y actualizando el registro existente (junto con Health Connect).
  - Si no existe o pasaron más de 30 minutos, guardar como nuevo registro.
- [x] 1.4 Crear pruebas unitarias deterministas en `SaveMealWithHealthSyncUseCaseTest`:
  - Prueba de consolidación dentro de la ventana de 30 minutos (verifica unión de items y recálculo de macros).
  - Prueba de creación de nuevo registro cuando transcurren más de 30 minutos.

---

## Fase 2: ViewModel y Ciclo de Vida del Estado

- [x] 2.1 Actualizar `FoodScanReviewViewModel` para exponer función de inicio de análisis de imagen (`startImageAnalysis`).
- [x] 2.2 Agregar método de limpieza de estado `resetState()` y vincularlo al guardado exitoso y a la navegación de salida.
- [x] 2.3 Implementar en `FoodScanReviewViewModel` la detección de comida reciente para informar a la UI si se consolidará (`consolidateTargetMealMinutesAgo`).
- [x] 2.4 Escribir pruebas unitarias en `FoodScanReviewViewModelTest` verificando que el estado no arrastra alimentos anteriores tras guardar.

---

## Fase 3: Pantallas Compose y Experiencia de Usuario (UI)

- [x] 3.1 Actualizar `FoodCameraScreen`:
  - Añadir estado `isCapturing` para deshabilitar el botón de captura y prevenir toques duplicados.
  - Añadir feedback visual en el botón de obturador al presionar.
- [x] 3.2 Actualizar `MainActivity`:
  - Transicionar inmediatamente a `AppDestination.REVIEW` al disparar la captura o selección de imagen.
  - Orquestar el análisis en background mientras la pantalla de revisión muestra el estado de carga.
- [x] 3.3 Diseñar la pantalla de carga espaciosa en `FoodScanReviewScreen`:
  - Contenedor amplio y respirable, spinner moderno y textos amigables en español chileno.
  - Tarjeta de tips de nutrición saludable.
- [x] 3.4 Añadir banner informativo de consolidación en `FoodScanReviewScreen`:
  - Notificación visual clara cuando aplique la regla de los 30 minutos (*"Se sumará a tu Almuerzo de hace X min"*).

---

## Fase 4: Verificación y Entrega

- [x] 4.1 Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 4.2 Compilar APK de depuración (`./gradlew assembleDebug`).
- [x] 4.3 Ejecutar script obligatorio de envío a Telegram (`bash ./scripts/send_apk_telegram.sh`).
