# Propuesta: Rediseño Visual "Fitness Tracker Pro - Edición Palta" y Corrección de Layouts

## 1. Contexto y Problema

Durante las pruebas visuales en dispositivo real se detectaron inconsistencias críticas en la interfaz gráfica que degradan la experiencia de usuario y dan una apariencia poco profesional:

1. **Barra de navegación inferior con corte visual:** El botón central flotante para escanear comidas aparece cercenado en su parte inferior debido a un conflicto entre la máscara de recorte de `Surface`, la altura fija de 68.dp y el cálculo de insets del sistema (`navigationBarsPadding`).
2. **Congestión y solapamiento en el visor de cámara:** En `FoodCameraScreen`, la cabecera intenta ubicar en una sola fila horizontal el botón de retroceso, el título "🥑 Palta", la insignia de motor IA, el texto descriptivo de instrucción y tres botones de acción. Esto provoca que en resoluciones convencionales el texto colisione y se sobreponga a los botones de la derecha. Además, la función "Describir con IA" se encuentra duplicada tanto en la barra superior como en una píldora flotante inferior.
3. **Incoherencia cromática y presencia de morados residuales:** El tema `CalculadoraCaloriasTheme` solo definió 4 tokens en `DarkColorScheme` (`primary`, `secondary`, `background`, `surface`), dejando que Material 3 asigne los valores por defecto a los más de 20 tokens restantes. Esto causa que elementos como la tarjeta de Google Health Connect (`primaryContainer`) y los contenedores de íconos se muestren en color violeta/morado estridente, rompiendo por completo la identidad visual del proyecto.

## 2. Metas y Alcance

### In-Scope (Dentro del alcance)
* **Sistema de Diseño "Fitness Tracker Pro - Palta":**
  * Definir una paleta completa de tokens Material 3 para modo oscuro y claro inspirada en una palta (tonos grafito orgánico `#111411`, verde pulpa `#8CE593`, verde cáscara `#2E7D32` y verde bosque profundo `#1E3A24` para contenedores).
  * Asignar explícitamente todos los tokens de contenedor (`primaryContainer`, `surfaceVariant`, `surfaceContainer`, `surfaceContainerHigh`, `outline`, etc.) eliminando cualquier rastro de morado predeterminado de Material 3.
  * Armonizar los colores de los tres macronutrientes (Proteína: azul atlético suave; Carbohidratos: ámbar dorado; Grasas: salmón/coral cálido).
* **Corrección de la Barra de Navegación Inferior:**
  * Reestructurar `AppBottomNavigationBar` para garantizar que el botón central de escaneo se dibuje completo, respetando los insets de navegación segura de Android sin sufrir recortes en los bordes.
* **Rediseño del Visor de Cámara:**
  * Separar la barra superior en niveles jerárquicos limpios: barra de navegación superior (retroceso, marca y estado IA, accesos secundarios ordenados) y una zona de guía no invasiva.
  * Eliminar la duplicación del botón de IA, dejando una única píldora de interacción destacada ("✨ Describir con IA") sobre los controles principales de captura.
  * Asegurar un espaciado óptimo en los controles inferiores (Galería, Obturador y Flash).
* **Refactorización de Pantallas Existentes:**
  * Actualizar `SettingsScreen` (especialmente la tarjeta de Health Connect) para que adopte los contenedores y contrastes del nuevo tema.
  * Actualizar `DailySummaryScreen` para que las tarjetas de comidas y métricas reflejen la jerarquía de un fitness tracker de alto nivel.

### Out-of-Scope (Fuera del alcance)
* Cambiar la arquitectura base (se mantiene 100% Kotlin con Jetpack Compose y Clean Architecture).
* Modificar la lógica interna de reconocimiento visual (LiteRT y MiniMax permanecen idénticos en dominio y datos).
* Modificar la base de datos Room o los contratos de Health Connect.

## 3. Criterios de Aceptación
1. El botón de escaneo en la barra inferior se visualiza con forma circular completa, sombra consistente y sin cortes en cualquier altura de navegación del sistema.
2. En la pantalla del escáner, ningún texto colisiona ni se superpone con los botones de acción en resoluciones pequeñas o grandes.
3. La acción "Describir con IA" tiene un punto único de entrada claro y destacado en el visor de cámara.
4. En modo oscuro, ningún componente de la app muestra el color morado por defecto de Material 3. La tarjeta de Health Connect se visualiza en un contenedor verde armonioso.
5. El diseño transmite una estética sobria, moderna y deportiva acorde a la identidad "Palta".
