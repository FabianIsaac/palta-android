# Propuesta: Catálogo de Suplementos Personalizados con IA, Reorganización del Diario y Barra de Navegación Simétrica

## 1. Problema y Justificación

En la iteración previa de suplementos (`daily-goals-streaks-and-supplements`), la funcionalidad quedó restringida de forma rígida a un único suplemento hardcodeado ("Omega 3 (2 cápsulas)"), sin permitir que el usuario gestione sus propios suplementos habituales (tales como creatina monohidrato, citrato de magnesio, proteína en polvo/whey, multivitamínicos, entre otros).

Además, se identificaron las siguientes oportunidades de mejora en la experiencia de usuario:
1. **Falta de catálogo persistente y dinámico de suplementos:** El usuario necesita registrar sus suplementos habituales, activar con un toque sugerencias preconfiguradas populares y crear suplementos personalizados con nombre y dosis.
2. **Fricción al ingresar información nutricional de suplementos:** El usuario no siempre conoce con precisión cuántas calorías o macronutrientes aporta un suplemento (o si aporta algo, como la creatina o el magnesio que aportan 0 kcal). Se requiere asistencia inteligente mediante IA (MiniMax) y respaldo heurístico local (offline) para estimar automáticamente dosis típica, calorías y macronutrientes a partir del nombre ingresado.
3. **Ubicación y flujo en el Diario (`DailySummaryScreen`):** El checklist de suplementos se ubicaba de forma antinatural antes de las comidas principales. Debe posicionarse al final de la jornada alimentaria (después de la última comida o colación), funcionando estrictamente como checklist diario de toma, sin botones ni diálogos de configuración que saturen el diario.
4. **Navegación limpia y equilibrada sin modales:** Siguiendo la preferencia de diseño, la gestión de suplementos debe contar con su propia pantalla completa dedicada (`SupplementsScreen`), accesible directamente desde la barra de navegación inferior. Para mantener la simetría de la barra con respecto al botón central flotante (FAB de Cámara), se incorpora además una pantalla de Estadísticas (`StatisticsScreen`) como vista informativa/placeholder para futuros reportes.

---

## 2. Alcance (In-Scope)

- **Persistencia en Room del Catálogo de Suplementos (`SupplementEntity` y DAO):**
  - Tabla de suplementos configurados por el usuario (`id`, `name`, `dosageDescription`, `calories`, `proteinGrams`, `carbsGrams`, `fatGrams`, `isActive`, `isCustom`).
  - Suplementos sugeridos preconfigurados listos para activar con un solo toque: *Creatina Monohidrato (5g)*, *Citrato de Magnesio (400mg)*, *Omega 3 (2 cápsulas)*, *Proteína Whey (1 scoop)* y *Multivitamínico (1 comprimido)*.
  - Conservación del historial existente en `SupplementLogEntity` (`date`, `supplementId`, `takenTimestamp`).

- **Estimación Inteligente de Suplementos con IA y Modo Offline:**
  - Caso de uso `EstimateSupplementNutritionUseCase` que consulta a MiniMax (vía API) para interpretar el nombre/descripción del suplemento e inferir dosis sugerida y macronutrientes.
  - Catálogo heurístico local de respaldo para operar sin conexión o sin clave de API.
  - Campos de entrada editables para que el usuario valide o ajuste los números antes de guardar.

- **Pantalla Completa de Suplementos (`SupplementsScreen` y ViewModel):**
  - Módulo dedicado accesible desde la barra inferior (sin modales ni bottom sheets).
  - Sección de suplementos populares sugeridos para activar con un toque.
  - Sección de creación asistida con IA.
  - Lista de suplementos activos en la rutina con opciones para editar o eliminar.

- **Reorganización del Diario (`DailySummaryScreen`):**
  - Reubicación del widget `DailySupplementsCard` inmediatamente **después** de la última comida y colaciones.
  - Eliminación de accesos o botones de administración dentro del diario: funciona únicamente como lista de verificación táctil (check) para registrar la toma del día.
  - Al marcar un suplemento, suma inmediatamente su aporte de calorías y macros al balance nutricional de la fecha seleccionada.

- **Barra de Navegación Inferior Simétrica de 5 Destinos (`AppBottomNavigationBar`):**
  - Estructura equilibrada alrededor del FAB central de cámara:
    - Izquierda: `Diario` (`SUMMARY`) y `Estadísticas` (`STATISTICS`).
    - Centro: `Cámara` (`CAMERA`).
    - Derecha: `Suplementos` (`SUPPLEMENTS`) y `Ajustes` (`SETTINGS`).
  - Nueva pantalla completa `StatisticsScreen` con interfaz informativa limpia (placeholder de futuras métricas).

---

## 3. Fuera de Alcance (Out-of-Scope)

- Motor completo de gráficos y analíticas de estadísticas (se implementará en un cambio posterior dedicado).
- Recordatorios mediante notificaciones push del sistema operativo.
- Escaneo fotográfico de etiquetas de suplementos con OCR (la entrada se realiza por texto asistido con IA).

---

## 4. Impacto en Base de Datos y Fórmulas Nutricionales

- **Base de Datos (Room):**
  - Incremento de la versión de la base de datos `AppDatabase` (v6 a v7) con migración para crear la tabla `supplements`.
  - Precarga inicial con los suplementos predeterminados del sistema.
- **Fórmulas Nutricionales:**
  - No se modifican las fórmulas de Mifflin-St Jeor ni TDEE.
  - Los suplementos marcados como tomados continúan sumando sus calorías y macronutrientes al total consumido del día en `DailySummaryViewModel`.
- **UI & Estados:**
  - `DailySummaryScreen` actualiza el orden de sus componentes en el `LazyColumn`.
  - Se añade `SupplementsScreen` y su `SupplementsViewModel`.
  - Se añade `StatisticsScreen`.
  - Se amplía el enum `AppDestination` con `STATISTICS` y `SUPPLEMENTS`.
