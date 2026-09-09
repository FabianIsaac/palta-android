# AI Agent Guidelines & OpenSpec Workflow

Welcome to the **Calculadora de Calorías para Android** project repository. This project uses **OpenSpec** (Spec-Driven Development) to ensure clear specifications, predictable changes, and high code quality.

---

## 1. Reglas Generales para Asistentes de IA

1. **La especificación manda (Source of Truth):**
   - Antes de escribir cualquier línea de código de producción, consulta las especificaciones vigentes en `openspec/specs/` y el contexto en `openspec/project.md`.
   - Si se propone una nueva característica o un cambio en el comportamiento existente, debe pasar por el flujo de cambios en `openspec/changes/`.

2. **Stack y Arquitectura no negociables:**
   - **Plataforma:** Android Nativo en **Kotlin** con **Jetpack Compose** y **Material Design 3**.
   - **Clean Architecture + MVVM / UDF:**
     - La lógica de negocio y fórmulas matemáticas residen en la capa de `domain` pura (sin clases de `android.*`).
     - La persistencia se realiza exclusivamente mediante repositorios que encapsulan **Room Database** y **DataStore**.
     - La UI solo interactúa con ViewModels a través de `UiState` inmutables y eventos unidireccionales.
   - **Offline-First:** Toda la funcionalidad debe operar sin conexión y sin autenticación obligatoria en la nube.

3. **Precisión Numérica y Fórmulas:**
   - Fórmulas de gasto energético (Mifflin-St Jeor) y conversiones de macronutrientes deben estar cubiertas con pruebas unitarias deterministas.

4. **Idioma de Interfaz 100% Español Chileno (`es-CL`) sin voseo (ESTRICTO):**
   - Todo texto visible para el usuario (pantallas Compose, `strings.xml`, diálogos, placeholders, validaciones y previews) debe estar en **Español Latino de Chile**.
   - **PROHIBIDO EL VOSEO ARGENTINO:** No usar expresiones ni conjugaciones como *andá, creá, guardá, hacé, tenés, vos*.
   - Usar tuteo estándar / chileno (*crea, guarda, agrega, ingresa, revisa, tú*).
   - Adaptar comidas y nombres cotidianos a Chile: *Desayuno, Almuerzo, Once / Cena, Colaciones; Palta, Marraqueta, etc.*

5. **Envío automático de APK a Telegram tras agregar nuevas funcionalidades (OBLIGATORIO):**
   - Cada vez que se termine de implementar un cambio que agregue nuevas funcionalidades (por ejemplo, al completar la fase de implementación de `/opsx:apply` o antes de archivar), se debe compilar y enviar automáticamente el APK ejecutable al usuario por Telegram ejecutando:
     ```bash
     bash ./scripts/send_apk_telegram.sh
     ```

---

## 2. Flujo de Trabajo OpenSpec (`/opsx`)

Cuando trabajes en nuevas funcionalidades, mejoras o refactorizaciones, sigue el ciclo de vida de OpenSpec:

### Paso 1: Explorar (`/opsx:explore` o investigación)
- Analiza los archivos existentes en `openspec/specs/` y el código del proyecto.
- Aclara dudas de alcance o arquitectura antes de proponer cambios.

### Paso 2: Proponer (`/opsx:propose <nombre-de-cambio>`)
- Crea una carpeta en `openspec/changes/<nombre-de-cambio>/` con:
  - `proposal.md`: Justificación, problemas a resolver y alcance (in-scope / out-of-scope).
  - `design.md`: Decisiones arquitectónicas, modelos de datos, flujos de UI y contratos técnicos.
  - `specs/`: Delta specs detallando requisitos añadidos (`ADDED`), modificados (`MODIFIED`) o eliminados (`REMOVED`) con formato EARS y escenarios BDD.
  - `tasks.md`: Lista de tareas accionables con dependencias y fases de testing.

### Paso 3: Aplicar (`/opsx:apply`)
- Implementa las tareas definidas en `tasks.md` en orden:
  1. Modelos de dominio y casos de uso + pruebas unitarias.
  2. Entidades Room, DAOs y repositorios de datos.
  3. ViewModels y estados de Compose.
  4. Pantallas de UI y Compose Previews.

### Paso 4: Archivar (`/opsx:archive`)
- Tras completar y verificar todas las tareas, sincroniza los cambios en las especificaciones maestras de `openspec/specs/` y archiva la propuesta en `openspec/changes/archive/YYYY-MM-DD-<nombre-de-cambio>/`.

---

## 3. Estructura de Directorios

```text
├── openspec/
│   ├── config.yaml                    # Configuración del motor OpenSpec y reglas
│   ├── project.md                     # Constitución del proyecto, stack y fórmulas
│   ├── specs/                         # Especificaciones canónicas (Verdad del sistema)
│   │   ├── user-profile/spec.md
│   │   ├── food-logging/spec.md
│   │   └── daily-summary/spec.md
│   └── changes/                       # Cambios activos en desarrollo
│       └── archive/                   # Histórico de cambios completados
├── AGENTS.md                          # Esta guía de instrucciones para IA
└── .gitignore                         # Configuración de exclusiones de Git
```
