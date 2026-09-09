# Tareas: Barra de Navegación Inferior y Corrección de Navegación de Retorno

## Fase 1: Componente de Barra de Navegación Inferior
- [x] 1.1 Crear el componente componible `AppBottomNavigationBar` en `app/src/main/java/com/calculadoracalorias/app/presentation/navigation/AppBottomNavigationBar.kt`:
  - Botón izquierdo "Diario" con ícono de libro (`Icons.Default.MenuBook`) y texto pequeño.
  - Botón central elevado destacado "Escanear" con ícono de cámara (`Icons.Default.CameraAlt`) y fondo primario.
  - Botón derecho "Ajustes" con ícono de tuerca (`Icons.Default.Settings`) y texto pequeño.
  - Vistas previas (`@Preview`) en modo claro y oscuro.
- [x] 1.2 Agregar strings localizados en `app/src/main/res/values/strings.xml` para las etiquetas de navegación si no existen ("Diario", "Escanear", "Ajustes").

## Fase 2: Integración de Navegación en MainActivity
- [x] 2.1 Envolver las pantallas principales en un `Scaffold` con `AppBottomNavigationBar` en `AppNavigation` (`MainActivity.kt`):
  - Mostrar la barra inferior únicamente cuando `currentScreen == AppDestination.SUMMARY` o `currentScreen == AppDestination.SETTINGS`.
  - Ocultar la barra inferior en `CAMERA`, `REVIEW` y `EDIT_MEAL`.
- [x] 2.2 Configurar las transiciones de navegación entre `SUMMARY`, `SETTINGS` y `CAMERA`.

## Fase 3: Simplificación de DailySummaryScreen
- [x] 3.1 Eliminar el botón de ajustes de la `TopAppBar` en `DailySummaryScreen.kt` para dejar la barra superior limpia solo con el título.
- [x] 3.2 Eliminar el `FloatingActionButton` redundante de la esquina inferior derecha en `DailySummaryScreen.kt`.

## Fase 4: Corrección Integral de Navegación de Retorno (BackHandler)
- [x] 4.1 En `FoodCameraScreen.kt`:
  - Incorporar `BackHandler { onNavigateBack() }` para que los gestos y botón atrás del sistema regresen al Diario sin cerrar la app.
  - Agregar botón visual de retorno (`ArrowBack`) en el encabezado superior izquierdo.
- [x] 4.2 En `FoodScanReviewScreen.kt`:
  - Agregar `BackHandler { onNavigateBack() }` para navegación segura hacia atrás.
- [x] 4.3 En `EditMealScreen.kt`:
  - Agregar `BackHandler { onNavigateBack() }` para regresar al Diario de forma controlada.
- [x] 4.4 En `SettingsScreen.kt`:
  - Agregar `BackHandler { onNavigateBack() }` para regresar a la pestaña del Diario cuando se use el gesto atrás del sistema.

## Fase 5: Pruebas, Verificación y Generación de APK
- [x] 5.1 Ejecutar compilación del proyecto y verificar ausencia de errores de sintaxis o linter.
- [x] 5.2 Ejecutar la suite de pruebas unitarias (`./gradlew testDebugUnitTest`).
- [x] 5.3 Compilar y enviar automáticamente el APK a Telegram mediante `bash ./scripts/send_apk_telegram.sh`.
