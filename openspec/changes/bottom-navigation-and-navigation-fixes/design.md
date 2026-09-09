# Diseño: Barra de Navegación Inferior y Corrección de Retorno

## 1. Arquitectura de Navegación y UI

### 1.1. Flujo de Navegación Global (Unidirectional Data Flow)

Actualmente la navegación se gestiona en `AppNavigation` en `MainActivity.kt` con un estado mutable `currentScreen: AppDestination`.

El diseño integra una estructura de `Scaffold` contenedora para las pantallas de nivel superior (`SUMMARY` y `SETTINGS`), donde reside la barra de navegación inferior.

```
       +-------------------------------------------------------+
       |               AppNavigation (Scaffold)               |
       |                                                       |
       |  +-------------------------------------------------+  |
       |  | Contenido según currentScreen:                  |  |
       |  |  - SUMMARY  -> DailySummaryScreen               |  |
       |  |  - SETTINGS -> SettingsScreen                   |  |
       |  +-------------------------------------------------+  |
       |                                                       |
       |  +-------------------------------------------------+  |
       |  | AppBottomNavigationBar (Visible si SUMMARY/SETT) |  |
       |  |  [Diario]        (( Escanear ))        [Ajustes] |  |
       |  +-------------------------------------------------+  |
       +-------------------------------------------------------+
                                  |
                                  | Toca "Escanear" o botón "+"
                                  v
       +-------------------------------------------------------+
       | FoodCameraScreen (Inmersiva / Pantalla Completa)      |
       |                                                       |
       |  [ <- Volver ]                                        |
       |  BackHandler { onNavigateBack() -> SUMMARY }          |
       +-------------------------------------------------------+
                                  |
                                  | Captura o Selección
                                  v
       +-------------------------------------------------------+
       | FoodScanReviewScreen (Inmersiva)                      |
       |                                                       |
       |  BackHandler { onNavigateBack() -> SUMMARY / CAMERA } |
       +-------------------------------------------------------+
```

---

## 2. Componentes de UI y Especificación Visual

### 2.1. Componente `AppBottomNavigationBar`

Se implementa como un componente componible reutilizable `AppBottomNavigationBar`:
- **Contenedor**: `Surface` con elevación tonal de Material 3 y color de fondo acorde a `colorScheme.surface` / `surfaceContainer`.
- **Botón Izquierdo ("Diario")**:
  - Ícono: `Icons.Default.MenuBook` (representación gráfica de diario/bitácora).
  - Texto: "Diario", tipografía `labelSmall` de Material 3.
  - Estado activo: Resaltado con color primario cuando `currentScreen == AppDestination.SUMMARY`.
  - Acción: Invoca `onNavigateToDestination(AppDestination.SUMMARY)`.
- **Botón Central ("Escanear")**:
  - Diseño: Botón circular prominente elevado (tipo FAB central de 56.dp).
  - Color de contenedor: `colorScheme.primary`, color de contenido: `colorScheme.onPrimary`.
  - Ícono: `Icons.Default.CameraAlt` (tamaño 28.dp).
  - Acción: Invoca `onNavigateToDestination(AppDestination.CAMERA)`.
- **Botón Derecho ("Ajustes")**:
  - Ícono: `Icons.Default.Settings`.
  - Texto: "Ajustes", tipografía `labelSmall` de Material 3.
  - Estado activo: Resaltado con color primario cuando `currentScreen == AppDestination.SETTINGS`.
  - Acción: Invoca `onNavigateToDestination(AppDestination.SETTINGS)`.

### 2.2. Barra Superior Simplificada en `DailySummaryScreen`

- **Título**: Se mantiene `Text(stringResource(R.string.title_daily_summary))` con estilo `MaterialTheme.typography.titleLarge` y peso `FontWeight.Bold`.
- **Acciones**: Se eliminan las acciones `actions` de la `TopAppBar` (anteriormente contenían el acceso a configuración).
- **FloatingActionButton**: Se remueve el `floatingActionButton` de `DailySummaryScreen` para evitar redundancia visual con el botón central de escaneo.

### 2.3. Corrección de Retroceso en `FoodCameraScreen`

- **Botón visible en encabezado**:
  - Se añade un `IconButton` con `Icons.AutoMirrored.Filled.ArrowBack` en el encabezado superior izquierdo de la cámara.
  - Al presionarlo, invoca `onNavigateBack()` que redirige a `AppDestination.SUMMARY`.
- **Manejo de BackHandler**:
  - Se invoca `BackHandler { onNavigateBack() }` al inicio de `FoodCameraScreen`.
  - Esto intercepta el gesto de deslizamiento del borde de la pantalla de Android y el botón atrás del sistema operativo, regresando al Diario de forma fluida y evitando el cierre de la aplicación.

### 2.4. Resguardo de BackHandler en Pantallas Secundarias

- `FoodScanReviewScreen`: Cuenta con `BackHandler { onNavigateBack() }` para retornar al Diario o a la Cámara.
- `EditMealScreen`: Cuenta con `BackHandler { onNavigateBack() }` para regresar al Diario sin pérdida ni cierre abrupto.
- `SettingsScreen`: Cuenta con `BackHandler { onNavigateBack() }` para volver a la pestaña del Diario cuando el usuario usa el gesto atrás del sistema.

---

## 3. Decisiones Técnicas y Contratos

| Elemento | Decisión | Justificación |
| :--- | :--- | :--- |
| **Arquitectura de Barra** | Composable `AppBottomNavigationBar` integrado en `AppNavigation` | Permite alternar entre pestañas raíz (`SUMMARY`, `SETTINGS`) manteniendo la navegación limpia y centralizada. |
| **Visibilidad Condicional** | Mostrar barra sólo en `SUMMARY` y `SETTINGS` | La cámara y las pantallas de edición/revisión requieren espacio completo e inmersivo sin elementos que estorben la interacción. |
| **Ícono del Diario** | `Icons.Default.MenuBook` | Comunica con claridad el concepto de bitácora/diario personal de nutrición. |
| **Gesto Atrás (Back)** | `BackHandler` en cada pantalla secundaria | Resuelve el cierre abrupto de la aplicación en gestos de retroceso de Android. |
