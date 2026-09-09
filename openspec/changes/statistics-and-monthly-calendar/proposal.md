# Propuesta: Pantalla de Estadísticas con Rangos Configurables y Calendario Mensual de Hábitos

## 1. Contexto y Justificación

Actualmente, la aplicación cuenta con un flujo completo de registro de alimentos (por cámara con IA, texto y catálogo manual), cálculo metabólico personalizado, seguimiento de suplementos y un resumen diario interactivo con soporte de fechas pasadas y futuras. En la barra de navegación inferior ya existe el acceso a **Estadísticas**, pero actualmente dicha pantalla es solo un marcador de posición (*placeholder*) informativo.

Para consolidar el progreso y la adherencia del usuario a largo plazo, es fundamental ofrecer dos herramientas complementarias:
1. **Visualización de Estadísticas e Historial:** Permitir analizar el comportamiento nutricional a lo largo del tiempo seleccionando distintos rangos temporales (últimos 7 días, 14 días o 30 días). El foco principal está en el consumo calórico diario comparado contra la meta establecida, junto con el desglose de macronutrientes promedio y la adherencia en la toma de suplementos. Todo esto se presenta con un enfoque constructivo y positivo: las metas calóricas son guías de referencia y no mecanismos de castigo o juicio.
2. **Calendario Mensual Interactivo:** Brindar una visión global mensual accesible desde el botón superior de la barra de herramientas (*TopAppBar*). Este calendario destaca los días con el hábito cumplido (aquellos con las 3 comidas principales chilenas registradas: *Desayuno*, *Almuerzo* y *Once / Cena*) y los suplementos tomados. Además, permite tocar cualquier día para consultar su ficha resumida y saltar directamente al Diario de esa fecha para revisar o editar registros.

---

## 2. Alcance (Scope)

### Dentro del alcance (In-Scope)
- **Pantalla de Estadísticas (`StatisticsScreen` + `StatisticsViewModel`):**
  - Selector de rangos de análisis configurables:
    - *Últimos 7 días* (semana reciente).
    - *Últimos 14 días* (quincena).
    - *Últimos 30 días* (mes).
  - **Tarjeta de Resumen del Periodo:**
    - Promedio diario de calorías consumidas vs. calorías objetivo.
    - Total de días con hábito cumplido (3 comidas principales) en el periodo.
    - Racha actual de días consecutivos.
  - **Gráfica de Evolución Calórica Diaria:**
    - Barras de consumo calórico por día construidas de forma nativa y fluida en Compose (`Canvas`).
    - Línea de referencia horizontal con el objetivo calórico configurado en el perfil.
    - Interacción para inspeccionar el valor exacto de cada barra/día.
  - **Desglose de Macronutrientes:**
    - Promedios diarios de proteína (g), carbohidratos (g) y grasas (g).
    - Distribución porcentual calórica de los macros.
  - **Consistencia de Suplementos:**
    - Porcentaje de adherencia y dosis registradas en el periodo seleccionado.
  - Botón en la barra superior (*TopAppBar*) para abrir el Calendario Mensual.

- **Componente de Calendario Mensual (`MonthlyHabitsCalendarModal`):**
  - Diálogo o panel modal desplegable accesible desde el botón de calendario del *TopAppBar* tanto en el Diario (`DailySummaryScreen`) como en Estadísticas (`StatisticsScreen`).
  - Navegación mensual hacia atrás y adelante (`< Mes Anterior | Mes Siguiente >`).
  - Cuadrícula de días (Lunes a Domingo) con el número del día.
  - Insignia o indicador visual de **hábito cumplido** (fuego 🔥 o tilde) para días con registros en Desayuno, Almuerzo y Once / Cena.
  - Indicador sutil para días con suplementos tomados.
  - Selección de fecha interactiva: al tocar un día, se muestra una tarjeta inferior con el resumen rápido del día (calorías consumidas, macros, comidas registradas y estado de suplementos).
  - Acción directa: botón *"Ir al Diario de este día"*, que actualiza la fecha activa en el diario (`DailySummaryViewModel.onEvent(OnDateSelected(fecha))`) y conmuta a la vista del Diario.

- **Capa de Dominio y Datos (Clean Architecture):**
  - Modelo `PeriodStatistics` con totales, promedios y series temporales para el rango solicitado.
  - Modelo `DayHabitSummary` con estado de comidas chilenas, calorías y suplementos para cada celda del calendario.
  - Caso de uso `GetPeriodStatisticsUseCase`: agrupa comidas y suplementos para el rango seleccionado.
  - Caso de uso `GetMonthlyCalendarHabitsUseCase`: genera el estado de cada día del mes para el calendario.

### Fuera del alcance (Out-of-Scope)
- Exportación de estadísticas en formato PDF o CSV (posible mejora futura).
- Integración con servicios de nube externos distintos a los repositorios locales offline-first y Health Connect existentes.

---

## 3. Impacto en Componentes del Sistema

### 3.1. Impacto en Base de Datos (Room)
- **Entidades:** No se requieren modificaciones en las tablas existentes (`meal_entries`, `meal_food_items`, `supplements`, `supplement_logs`). No se requieren migraciones de Room.
- **Consultas DAO:**
  - `MealDao`: Ya cuenta con `getMealsWithItemsBetween(startTime: Long, endTime: Long): Flow<List<MealWithItems>>` que soporta cualquier intervalo temporal.
  - `SupplementLogDao`: Soporta consultas por fecha o rango para determinar las dosis tomadas en el periodo.

### 3.2. Impacto en UI y Compose
- Sustitución del marcador de posición en `StatisticsScreen.kt` por una pantalla completa reactiva impulsada por `StatisticsViewModel`.
- Nuevo componente `MonthlyHabitsCalendarDialog` (o `MonthlyHabitsCalendarSheet`) reutilizable desde `DailySummaryScreen` y `StatisticsScreen`.
- Integración del icono de calendario en el `TopAppBar` de `DailySummaryScreen`.
- Gráfica nativa en Compose para evolución calórica diaria con línea de meta.

### 3.3. Impacto en Fórmulas y Nutrición
- No se alteran las fórmulas metabólicas existentes (Mifflin-St Jeor / Harris-Benedict).
- Se calculan promedios aritméticos simples: $\text{Promedio} = \frac{\sum \text{Valores}}{\text{Días del periodo}}$.
- El cumplimiento del hábito diario se mantiene según el estándar chileno: al menos 1 registro en Desayuno, 1 en Almuerzo y 1 en Once / Cena.
- Enfoque libre de castigos: las barras de calorías reflejan consumo real sin penalizaciones en la racha de hábitos.

---

## 4. Localización e Idioma
- 100% Español de Chile (`es-CL`) sin voseo argentino (prohibido *guardá*, *andá*, *mirá*; usar *revisa*, *selecciona*, *agrega*, *ve*).
- Formato de fechas en español (ej. "Lunes", "Martes", "Septiembre 2026", "d 'de' MMMM").
