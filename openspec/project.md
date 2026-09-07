# Calculadora de Calorías para Android

## 1. Visión y Propósito

Aplicación móvil nativa para Android diseñada para permitir a los usuarios calcular su gasto energético diario (BMR/TDEE), establecer objetivos nutricionales personalizados (pérdida de peso, mantenimiento o ganancia muscular) y registrar su ingesta diaria de alimentos y macronutrientes de forma rápida, privada y 100% offline.

---

## 2. Stack Tecnológico

- **Plataforma:** Android Nativo
- **Lenguaje:** Kotlin (versión 2.0+)
- **SDK:** `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 35` (Android 15)
- **UI Toolkit:** Jetpack Compose con Material Design 3 (Material You, soporte para tema claro/oscuro dinámico)
- **Patrón de Arquitectura:** Clean Architecture con MVVM y Unidirectional Data Flow (UDF)
- **Gestión Asíncrona:** Kotlin Coroutines y StateFlow / SharedFlow
- **Persistencia de Datos:** Room Database (SQLite) + Jetpack DataStore para preferencias de usuario
- **Inyección de Dependencias:** Hilt (recomendado) o Koin
- **Testing:**
  - Pruebas unitarias: JUnit 5, MockK, Turbine (para testear Flows)
  - Pruebas de UI: Compose Testing Library, Espresso

---

## 3. Arquitectura del Proyecto

El código sigue estrictamente la separación de responsabilidades en tres capas:

```text
┌─────────────────────────────────────────────────────────┐
│                    Capa UI / Presentación               │
│  - Composables (Screens, Components, Theme)             │
│  - ViewModels (exponen UiState vía StateFlow)           │
│  - MVI/UDF: UiEvents -> ViewModel -> UiState            │
└────────────────────────────┬────────────────────────────┘
                             │ consume
┌────────────────────────────▼────────────────────────────┐
│                    Capa de Dominio (Domain)             │
│  - Pure Kotlin (cero dependencias de Android framework) │
│  - Modelos de dominio (UserProfile, FoodItem, DailyLog) │
│  - Casos de Uso (CalculateTdeeUseCase, LogMealUseCase)  │
│  - Interfaces de Repositorios                           │
└────────────────────────────┬────────────────────────────┘
                             │ implementa
┌────────────────────────────▼────────────────────────────┐
│                    Capa de Datos (Data)                 │
│  - Implementaciones de Repositorios                     │
│  - Room Database (Entities, DAOs, TypeConverters)       │
│  - DataSources (locales, pre-población de alimentos)    │
│  - DataStore para configuración y preferencias          │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Fundamentos de Dominio y Nutrición

### 4.1. Cálculo de Tasa Metabólica Basal (BMR)
Se utiliza la ecuación de **Mifflin-St Jeor** como estándar clínico:

$$\text{BMR}_{\text{hombres}} = (10 \times \text{peso en kg}) + (6.25 \times \text{altura en cm}) - (5 \times \text{edad en años}) + 5$$
$$\text{BMR}_{\text{mujeres}} = (10 \times \text{peso en kg}) + (6.25 \times \text{altura en cm}) - (5 \times \text{edad en años}) - 161$$

### 4.2. Gasto Energético Diario Total (TDEE)
$$\text{TDEE} = \text{BMR} \times \text{Factor de Actividad}$$

| Nivel de Actividad | Factor | Descripción |
| :--- | :--- | :--- |
| Sedentario | 1.200 | Poco o ningún ejercicio, trabajo de escritorio |
| Actividad Ligera | 1.375 | Ejercicio ligero 1 a 3 días por semana |
| Actividad Moderada | 1.550 | Ejercicio moderado 3 a 5 días por semana |
| Alta Actividad | 1.725 | Ejercicio intenso 6 a 7 días por semana |
| Actividad Extrema | 1.900 | Entrenamiento atlético muy intenso o trabajo físico pesado |

### 4.3. Ajuste por Objetivo Calórico
- **Déficit calórico (Pérdida de grasa):** TDEE - (300 a 500 kcal)
- **Mantenimiento:** TDEE
- **Superávit calórico (Aumento de masa muscular):** TDEE + (250 a 500 kcal)

### 4.4. Equivalencias de Macronutrientes
- **Proteína:** $4\text{ kcal por gramo}$
- **Carbohidratos:** $4\text{ kcal por gramo}$
- **Grasas:** $9\text{ kcal por gramo}$

---

## 5. Módulos y Dominios Principales

1. **`user-profile`:** Gestión de biometría del usuario, historial de peso, nivel de actividad y cálculo de requerimientos calóricos diarios.
2. **`food-logging`:** Base de datos de alimentos, creación de alimentos personalizados, registro por categorías de comida (Desayuno, Almuerzo, Once / Cena, Colaciones) y cálculo de macros según la porción consumida en gramos/ml.
3. **`daily-summary`:** Tablero principal en tiempo real con gráfico de progreso circular, calorías consumidas, calorías restantes y balance de macronutrientes frente a los objetivos diarios.
4. **`history-analytics`:** Histórico de consumo diario, tendencias de calorías semanales/mensuales y correlación con el peso corporal.

---

## 6. Convenciones y Reglas de Desarrollo

- **Inmutabilidad:** Todos los modelos de datos deben ser `data class` inmutables con propiedades `val`.
- **Estados de UI:** Cada pantalla debe representar su estado completo mediante una única clase inmutable `UiState` (ej. `sealed interface DashboardUiState`).
- **Compose Previews:** Todos los componentes reutilizables y pantallas completas deben contar con `@Preview` que contemple estados de carga, contenido y error.
- **Offline-First:** No debe requerirse conexión a internet ni login obligatorio para el funcionamiento completo de la aplicación.
- **Formato OpenSpec:** Cada nueva característica o modificación debe seguir el ciclo `/opsx:propose` -> `/opsx:apply` -> `/opsx:archive`.

---

## 7. Idioma, Localización y Tono de la Interfaz (UI)

- **Región e Idioma:** Español Latinoamericano, específicamente **Español de Chile (`es-CL`)**.
- **Prohibición de Voseo Rioplatense/Argentino:** Queda estrictamente prohibido el uso de voseo (no usar formas como *andá, creá, hacé, guardá, mirá, tenés, querés, vos*).
- **Tratamiento y Conjugación:** Se utiliza el tuteo estándar / chileno con imperativos naturales (*crea, agrega, guarda, ingresa, selecciona, revisa, tú*).
- **Terminología y Cultura Culinaria Chilena:**
  - Categorías de comida: **Desayuno**, **Almuerzo**, **Once / Cena**, **Colación / Colaciones**.
  - Alimentos locales: Utilizar términos locales como **Palta** (no aguacate), **Marraqueta**, **Hallulla**, **Frutillas** (no fresas), **Porotos** (no frijoles), etc.
- **Strings y Recursos Android:** Todos los textos visibles para el usuario deben centralizarse en `values/strings.xml` o `values-es-rCL/strings.xml` y ser validados bajo este criterio.

