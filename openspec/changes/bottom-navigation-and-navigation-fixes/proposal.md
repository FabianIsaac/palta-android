# Propuesta: Barra de Navegación Inferior y Corrección de Navegación de Retorno

## 1. Contexto y Justificación

Con el analizador de alimentos por visión computacional e inteligencia artificial ya en funcionamiento, la interfaz actual requiere optimizaciones ergonómicas y de usabilidad para mejorar el flujo diario del usuario:

1. **Navegación descentralizada y poco accesible**:
   - El acceso a la pantalla de configuración (*Ajustes*) se encuentra actualmente confinado en la esquina superior derecha (`TopAppBar`) del Diario.
   - El acceso al escáner de alimentos se encuentra en un botón flotante (`FloatingActionButton`) en la esquina inferior derecha.
   - No existe una barra de navegación inferior persistente que permita alternar rápidamente entre las secciones principales de la aplicación (*Diario* y *Ajustes*) con una sola mano.

2. **Cierre involuntario de la aplicación al presionar "Volver"**:
   - Actualmente, la navegación interna de la aplicación se gestiona mediante un estado reactivo en Compose (`currentScreen: AppDestination`).
   - Sin embargo, ninguna pantalla secundaria (`CAMERA`, `REVIEW`, `EDIT_MEAL`, `SETTINGS`) cuenta con un `BackHandler` de Jetpack Compose configurado.
   - En consecuencia, cuando el usuario realiza el gesto de retroceso del sistema operativo o presiona el botón físico/virtual de atrás de Android mientras está en la cámara, el sistema no encuentra intercepciones y cierra por completo la aplicación.
   - Además, la pantalla de escaneo con cámara (`FoodCameraScreen`) no posee un botón visual explícito de retorno en su encabezado superior.

Esta propuesta introduce una barra de navegación inferior moderna basada en Material Design 3 con botón central destacado para escanear, simplifica la barra superior y soluciona de forma definitiva el manejo de retroceso en todas las pantallas.

---

## 2. Alcance (Scope)

### Dentro del alcance (In-Scope)
- **Barra de Navegación Inferior (`BottomNavigationBar`):**
  - Barra de navegación inferior anclada visible en las pantallas principales (`Diario` y `Ajustes`).
  - **Botón izquierdo ("Diario"):** Ícono de libro/diario (`Icons.Default.MenuBook`) con etiqueta de texto en tamaño pequeño ("Diario"). Al presionar, navega al Resumen Diario (`AppDestination.SUMMARY`).
  - **Botón central destacado ("Escanear"):** Botón circular prominente elevado tipo FAB con color de contenedor primario e ícono de cámara (`Icons.Default.CameraAlt`). Al presionar, abre la cámara de escaneo (`AppDestination.CAMERA`).
  - **Botón derecho ("Ajustes"):** Ícono de configuración (`Icons.Default.Settings`) con etiqueta de texto en tamaño pequeño ("Ajustes"). Al presionar, navega a la configuración (`AppDestination.SETTINGS`).
  - Ocultamiento dinámico de la barra inferior cuando el usuario entra en pantallas modales o de flujo secundario (`CAMERA`, `REVIEW`, `EDIT_MEAL`).
- **Simplificación de la Barra Superior (`TopAppBar` en `DailySummaryScreen`):**
  - Eliminación del botón de configuración de la esquina superior derecha (`actions`).
  - Mantención del título de forma limpia ("Calculadora de Calorías").
  - Eliminación del botón flotante (`FloatingActionButton`) inferior derecho del Diario, reemplazado por el botón central de la barra inferior.
- **Corrección de Navegación y Botón Volver:**
  - Inclusión de `BackHandler` en `FoodCameraScreen` para que el gesto o botón atrás del sistema regrese inmediatamente al Diario (`AppDestination.SUMMARY`) sin cerrar la app.
  - Inclusión de un botón visual de retorno (`ArrowBack`) en el encabezado superior de `FoodCameraScreen`.
  - Inclusión de `BackHandler` en `FoodScanReviewScreen`, `EditMealScreen` y `SettingsScreen` para garantizar navegación predecible hacia atrás.

### Fuera del alcance (Out-of-Scope)
- Reestructuración de la base de datos o almacenamiento local (Room y DataStore permanecen intactos).
- Alteración de fórmulas de cálculo metabólico o de macronutrientes.
- Nuevas pestañas de navegación (ej. Estadísticas o Perfil detallado, previstas para hitos posteriores).

---

## 3. Impacto en Componentes del Sistema

### 3.1. Impacto en Base de Datos (Room)
- **Ninguno**: No se modifican entidades, esquemas ni versiones de base de datos.

### 3.2. Impacto en Fórmulas Nutricionales
- **Ninguno**: No se modifican las fórmulas de Mifflin-St Jeor ni los cálculos de calorías/macronutrientes.

### 3.3. Impacto en Capa de Presentación (UI & Compose)
- **`MainActivity.kt` / `AppNavigation`**:
  - Estructuración de un contenedor principal con Scaffold que aloja la barra de navegación inferior.
  - Gestión de visibilidad de la barra inferior según el destino activo (`SUMMARY` y `SETTINGS` visibles; `CAMERA`, `REVIEW` y `EDIT_MEAL` invisibles).
  - Manejo de destinos activos y navegación fluida entre pestañas.
- **`DailySummaryScreen.kt`**:
  - Eliminación de la acción de configuración en `TopAppBar`.
  - Eliminación del `floatingActionButton` redundantemente superpuesto.
- **`FoodCameraScreen.kt`**:
  - Incorporación de `BackHandler { onNavigateBack() }`.
  - Adición de un `IconButton` con `Icons.AutoMirrored.Filled.ArrowBack` en el encabezado superior.
- **`SettingsScreen.kt`, `FoodScanReviewScreen.kt`, `EditMealScreen.kt`**:
  - Aseguramiento de `BackHandler` en cada pantalla para consistencia global del botón volver.
