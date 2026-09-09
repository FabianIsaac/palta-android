# Especificación Delta: UI Layout & Fitness Tracker Theme

## Requisitos

### Requisito: Paleta Completa de Diseño Material 3 "Palta Pro"
El sistema DEBE proveer una paleta de colores cohesiva y completa tanto en modo claro como en modo oscuro basada en la temática de Palta (verde orgánico, salvia y bosque), definiendo explícitamente todos los tokens de contenedores y superficies para evitar la aparición de colores violetas o morados predeterminados de Material 3.

#### Escenario: Renderizado del modo oscuro sin colores morados residuales
- **DADO** que el usuario utiliza la aplicación en modo oscuro
- **CUANDO** se renderizan componentes que utilizan `primaryContainer`, `surfaceVariant` o `surfaceContainer` (como la tarjeta de Health Connect o la barra inferior)
- **ENTONCES** el sistema DEBE mostrar tonos verde bosque o grafito orgánico (`#1D4724`, `#1A201A`, etc.) sin ningún matiz púrpura o violeta.

#### Escenario: Visualización armónica de macronutrientes
- **DADO** que el usuario visualiza el desglose de macronutrientes en el resumen diario
- **CUANDO** se pintan las barras de Proteína, Carbohidratos y Grasas
- **ENTONCES** el sistema DEBE aplicar respectivamente azul cian atlético (`#4EA8DE`), ámbar cálido (`#F4A261`) y coral suave (`#E76F51`).

---

### Requisito: Barra de Navegación Inferior sin Cortes Visuales
El sistema DEBE renderizar el botón central de escaneo flotante en la barra de navegación inferior de forma íntegra y circular, respetando las dimensiones del componente y los insets de navegación del sistema operativo sin recortar su silueta ni su sombra.

#### Escenario: Renderizado con navegación por gestos o botones de sistema
- **DADO** cualquier modo de navegación del sistema Android (barra gestual o tres botones)
- **CUANDO** el usuario visualiza la barra de navegación inferior en `Diario` o `Ajustes`
- **ENTONCES** el botón de escaneo central DEBE mostrar su círculo completo de 56.dp con elevación intacta y sin recortes en la parte inferior o superior.

---

### Requisito: Interfaz Despejada y sin Solapamiento en el Escáner
El sistema DEBE presentar los controles e información del visor de cámara (`FoodCameraScreen`) en capas jerárquicas no competitivas, garantizando que el texto de instrucción nunca colisione con botones de acción y que no existan accesos duplicados para la descripción por IA.

#### Escenario: Visualización del encabezado en pantallas compactas
- **DADO** un dispositivo con ancho de pantalla reducido (menor a 360.dp)
- **CUANDO** el usuario abre la pantalla de escaneo de alimentos
- **ENTONCES** el texto de orientación y la insignia del motor IA DEBEN permanecer perfectamente legibles sin encimarse sobre los botones de Volver, Registro Manual o Ajustes.

#### Escenario: Punto único de interacción para describir con IA
- **DADO** que el usuario se encuentra en el visor de cámara
- **CUANDO** el usuario desea registrar su comida mediante lenguaje natural (texto o voz)
- **ENTONCES** el sistema DEBE ofrecer una única acción flotante ("✨ Describir con IA") ubicada en la parte inferior sobre los botones de captura, sin duplicar este botón en la barra superior.

---

### Requisito: Coherencia Temática en la Pantalla de Configuración
El sistema DEBE presentar la pantalla de `Ajustes` con la misma jerarquía y tokens cromáticos del tema Palta Pro, presentando la tarjeta de Google Health Connect en tonos verdes armoniosos que indiquen su estado de sincronización.

#### Escenario: Visualización de Health Connect conectado
- **DADO** que la app cuenta con permisos activos de Health Connect
- **CUANDO** el usuario abre la pantalla de Ajustes
- **ENTONCES** la tarjeta de Health Connect DEBE presentarse en contenedor verde bosque armónico (`primaryContainer`) con texto de alto contraste, eliminando el fondo púrpura previo.
