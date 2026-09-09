# Tareas de Implementación: custom-supplements-and-navigation

## Fase 1: Dominio y Pruebas Unitarias
- [x] 1.1 Actualizar el modelo de dominio `Supplement` con campos `isActive` e `isCustom`, y lista de suplementos predeterminados `PRECONFIGURED_SUPPLEMENTS`.
- [x] 1.2 Crear el caso de uso `EstimateSupplementNutritionUseCase` con interfaz para estimación remota vía LLM y catálogo heurístico local offline.
- [x] 1.3 Escribir pruebas unitarias en `EstimateSupplementNutritionUseCaseTest` validando tanto casos de aporte calórico (proteína, omega 3) como suplementos sin calorías (creatina, magnesio, multivitamínico) y fallback offline.

## Fase 2: Persistencia en Room y Repositorios
- [x] 2.1 Crear la entidad Room `SupplementEntity` y su correspondiente `SupplementDao` con operaciones CRUD y filtros de activos.
- [x] 2.2 Actualizar `AppDatabase` registrando la nueva entidad y DAO, incrementando la versión de base de datos.
- [x] 2.3 Actualizar `LocalSupplementRepository` para interactuar reactivamente con `SupplementDao` (catálogo persistente) y `SupplementLogDao` (registros diarios de toma), asegurando la siembra inicial de suplementos predeterminados.
- [x] 2.4 Actualizar pruebas unitarias en `LocalSupplementRepositoryTest`.

## Fase 3: ViewModels y Estados
- [x] 3.1 Crear `SupplementsContract` (UiState, UiEvent, UiEffect) para la gestión de suplementos.
- [x] 3.2 Implementar `SupplementsViewModel` gestionando la lista de suplementos, activación de sugerencias, estimación con IA y guardado/eliminación.
- [x] 3.3 Escribir pruebas unitarias en `SupplementsViewModelTest`.
- [x] 3.4 Verificar que `DailySummaryViewModel` consuma reactivamente los suplementos activos de `SupplementRepository` y sume calorías/macros de los marcados para la fecha seleccionada.

## Fase 4: Componentes de UI y Navegación
- [x] 4.1 Actualizar el enum `AppDestination` en `MainActivity` añadiendo `STATISTICS` y `SUPPLEMENTS`.
- [x] 4.2 Rediseñar `AppBottomNavigationBar` para albergar 4 pestañas laterales distribuidas simétricamente alrededor del FAB central: `Diario`, `Estadísticas` | [📷 FAB] | `Suplementos`, `Ajustes`.
- [x] 4.3 Construir la pantalla completa `StatisticsScreen` con información descriptiva de futuras funcionalidades.
- [x] 4.4 Construir la pantalla completa `SupplementsScreen` con sugerencias de 1 toque, formulario asistido con IA y lista de rutina activa.
- [x] 4.5 Reorganizar `DailySummaryScreen` moviendo `DailySupplementsCard` inmediatamente después de la última comida y colaciones, eliminando botones de administración interna.
- [x] 4.6 Conectar la navegación en `MainActivity` para todos los destinos, manteniendo la visibilidad de la barra inferior en las 4 pantallas principales.
- [x] 4.7 Configurar todos los strings en `strings.xml` en español de Chile (`es-CL`) sin voseo argentino y añadir `@Preview` de Compose en modo claro y oscuro.

## Fase 5: Verificación y Entrega
- [x] 5.1 Ejecutar la suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 5.2 Compilar el proyecto en modo depuración y verificar que no existan errores de compilación ni de UI.
- [x] 5.3 Compilar y enviar el APK ejecutable por Telegram ejecutando `bash ./scripts/send_apk_telegram.sh`.
