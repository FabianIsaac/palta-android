# Propuesta: Horarios Inteligentes Configurables y Mejoras de Experiencia en Revisión de Comida

## 1. Problema y Justificación

Durante el uso continuo de la aplicación y las pruebas en dispositivo físico se identificaron cinco oportunidades clave de mejora en la experiencia de usuario (UX) y usabilidad:

1. **Botones deformados en la pantalla de "Revisa tu comida":**
   En la cabecera de la sección de *Alimentos detectados*, el título comparte una única fila con dos botones (`Describir con IA` y `Agregar alimento`). En pantallas angostas o con fuentes ampliadas por accesibilidad, el botón de agregar alimento se comprime horizontalmente en forma circular, forzando un quiebre de texto antiestético: *"Agrega / r / alimento"*.

2. **Selector de tipo de comida poco compacto y desordenado:**
   Las categorías de comida se muestran actualmente como chips dispersos en un `FlowRow`. Al deslizar la pantalla o según el ancho del dispositivo, quedan chips aislados flotando en la parte superior, restando elegancia y ocupando valioso espacio vertical.

3. **Asignación horaria rígida sin detección inteligente:**
   Al iniciar una captura por cámara, texto o registro manual, el sistema asigna invariablemente la categoría `ALMUERZO` como valor por defecto, obligando al usuario a cambiarla manualmente en el desayuno o la once.

4. **Falta de personalización en las ventanas horarias de comidas:**
   Cada persona tiene rutinas diferentes (ej. trabajo por turnos, entrenamientos temprano en la mañana o cenas tardías). El sistema debe permitir personalizar desde Ajustes los rangos horarios de Desayuno, Almuerzo y Once / Cena.

5. **Sensación de congelamiento al ingresar comida por descripción de texto:**
   Al escribir una descripción libre (ej. *"marraqueta con palta y té"*) y presionar *Interpretar con IA*, la aplicación navega a la pantalla de revisión pero no activa la pantalla de carga (`isLoading = false`), dejando la vista vacía y estática durante varios segundos hasta recibir la respuesta del analizador, dando la impresión errónea de que la app se quedó pegada.

6. **Botón de ajustes redundante en el visor de la cámara:**
   En la barra superior de la cámara de escaneo existe un botón de acceso directo a Ajustes que distrae del objetivo principal (capturar el alimento) y genera inconsistencia con la navegación global inferior.

---

## 2. Alcance (In-Scope)

- **Rediseño de acciones en "Alimentos detectados":**
  - Reorganizar la cabecera de la sección dejando el título limpio y accesible.
  - Ubicar las acciones de agregar alimento y describir con IA en una barra de acciones dedicada y espaciosa que nunca colapse el texto en ningún dispositivo.

- **Selector compacto y moderno de tipo de comida:**
  - Implementar un selector seleccionable compacto (`ExposedDropdownMenuBox` o `SegmentedButton` Material 3) que muestre la categoría activa con su icono chileno representativo (Desayuno, Almuerzo, Once / Cena, Colaciones).

- **Detección horaria automática y contextual:**
  - Al abrir la cámara, el diálogo de texto o la entrada manual, evaluar la hora local del dispositivo (`LocalTime.now()`) contra las ventanas horarias configuradas para sugerir automáticamente la categoría correspondiente.
  - Mostrar visualmente en el selector que la categoría fue sugerida por la hora actual, permitiendo cambiarla con un solo toque.

- **Ventanas horarias configurables en Ajustes:**
  - Almacenar en `UserPreferencesRepository` (DataStore) los rangos horarios de inicio y fin para:
    - Desayuno (por defecto: 06:00 – 11:30)
    - Almuerzo (por defecto: 11:30 – 16:00)
    - Once / Cena (por defecto: 16:00 – 22:00)
    - Colaciones (restante o intermedio)
  - Agregar en `SettingsScreen` una sección de *"Horarios de Comidas"* donde el usuario pueda ajustar estos rangos con selectores de hora (`TimePicker`).

- **Feedback de carga inmediato en entrada por texto:**
  - Asegurar que al confirmar el texto en `QuickNaturalLanguageEntrySheet`, `FoodScanReviewViewModel` active un estado de carga explícito y visible (`isLoading = true` o visualización inmediata en `FoodScanReviewScreen`) con animación, indicador de progreso y mensaje de acompañamiento (*"Interpretando tu comida con IA..."*).

- **Limpieza del visor de la cámara:**
  - Remover el `IconButton` de Ajustes de la barra superior de `FoodCameraScreen`.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Edición de nombres de alimentos e incorporación de ingredientes faltantes en comidas ya guardadas (se abordará en la **Propuesta 2: `meal-editing-and-customization`**).
- Configuración de metas calóricas diarias, widget de suplementos y sistema de rachas (se abordará en la **Propuesta 3: `daily-goals-streaks-and-supplements`**).
