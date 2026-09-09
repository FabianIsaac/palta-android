# Diseño Técnico: Rediseño Visual "Fitness Tracker Pro - Edición Palta"

## 1. Arquitectura de Diseño y Tematización (Material Design 3)

### 1.1 Paleta Cromática "Palta Pro"
Se abandona la especificación incompleta de 4 colores y se pasa a una paleta completa de tokens M3 con contraste accesible (WCAG AA):

| Token M3 | Modo Oscuro (Valor Hex) | Modo Claro (Valor Hex) | Función en la App |
| :--- | :--- | :--- | :--- |
| `primary` | `#8CE593` (Verde Pulpa Fresca) | `#256E34` (Verde Bosque) | Botón de disparo, FAB, estados activos y progreso |
| `onPrimary` | `#043912` (Verde Profundo) | `#FFFFFF` | Texto/íconos sobre botones primarios |
| `primaryContainer` | `#1D4724` (Verde Musgo Sutil) | `#B7F2BD` (Verde Pastel) | Fondos de tarjetas destacadas (Health Connect, badges activos) |
| `onPrimaryContainer` | `#A8F7AF` | `#002107` | Texto sobre tarjetas destacadas |
| `secondary` | `#A1CCA5` (Salvia Suave) | `#4E654F` | Acciones secundarias y chips informativos |
| `background` | `#101410` (Grafito Orgánico) | `#F8FAF3` (Blanco Hueso Cálido) | Lienzo principal de la aplicación |
| `surface` | `#171D17` (Superficie Nivel 1) | `#FFFFFF` | Tarjetas de comidas y paneles |
| `surfaceVariant` | `#232B23` (Superficie Nivel 2) | `#E0E6DC` | Campos de texto, divisores e inputs inactivos |
| `surfaceContainer` | `#1A201A` | `#F0F4EC` | Barra de navegación inferior y hojas modales |
| `surfaceContainerHigh` | `#202820` | `#EAEFE6` | Tarjetas elevadas y diálogos |
| `outline` | `#394439` | `#727970` | Bordes sutiles para tarjetas sin sombra |
| `outlineVariant` | `#273027` | `#C2C9BE` | Líneas divisorias y pistas de progreso |

### 1.2 Paleta Especializada para Macronutrientes
Para evitar el uso de colores de advertencia (rojos de error) o saturaciones excesivas:
* **Proteína:** `#4EA8DE` (Azul cian deportivo, transmite energía y síntesis muscular).
* **Carbohidratos:** `#F4A261` (Ámbar dorado tostado, energía primaria).
* **Grasas:** `#E76F51` (Coral terracota suave, balance lipídico sin sensación de alarma).

---

## 2. Corrección de la Barra de Navegación (`AppBottomNavigationBar`)

### 2.1 Diagnóstico del Problema de Geometría
Anteriormente:
```
+--------------------------------------------------+
|  Surface (clipToBounds = true)                   |
|   Box(height = 68.dp + navigationBarsPadding)   |
|     FAB(size = 56.dp, offset = -12.dp)          |  <-- El offset y el padding fuerzan
+--------------------------------------------------+      el FAB fuera del viewport de Surface
```

### 2.2 Nueva Estructura Técnica
* Se utilizará un diseño de barra con un contenedor de altura total automática que encapsula correctamente `WindowInsets.navigationBars`:
* El botón central FAB se ubica centrado verticalmente con una elevación adecuada y sin offsets negativos arbitrarios que traspasen los límites de corte (`clip`).
* Si se desea un efecto elevado, el contenedor externo de la barra dejará suficiente margen superior (`padding(top = 8.dp)`) o se desacoplará el botón de acción central mediante un `Box` contenedor global en `Scaffold`, permitiendo que el botón flote naturalmente sobre la barra sin sufrir recortes por parte del `Surface`.

---

## 3. Rediseño del Visor de Cámara (`FoodCameraScreen`)

### 3.1 Nueva Jerarquía Visual

```
+-------------------------------------------------------------------+
|  [<- Volver]          🥑 Palta  [• IA Nube]         [📝]   [⚙️]   |  <-- Fila de navegación limpia
+-------------------------------------------------------------------+
|               "Enfoca tu plato para escanear"                     |  <-- Subtítulo centrado / banner sutil
+-------------------------------------------------------------------+
|                                                                   |
|                                                                   |
|                         [ VISOR CÁMARA ]                          |
|                                                                   |
|                                                                   |
+-------------------------------------------------------------------+
|                 [ ✨ Describir plato con IA ]                      |  <-- Único botón central destacado
+-------------------------------------------------------------------+
|       [ 🖼️ Galería ]            (  📷  )            [ ⚡ Flash ]   |  <-- Controles inferiores cómodos
+-------------------------------------------------------------------+
```

### 3.2 Decisiones Clave:
1. **Separación de Niveles:** La fila superior solo contiene navegación y acciones (Volver a la izquierda, título y estado al medio/izquierda, y accesos directos a Registro Manual y Ajustes a la derecha). El texto instructivo se ubica en un subtítulo debajo o en una tarjeta flotante translúcida no invasiva.
2. **Eliminación de Redundancia:** Se remueve el ícono de IA que estaba en la barra superior. La acción de describir por voz/texto se mantiene exclusivamente en la píldora flotante inferior, ubicada estratégicamente cerca de los pulgares del usuario.
3. **Márgenes de Seguridad Inferiores:** Los botones de Galería, Captura y Flash respetan el área de navegación gestual para no quedar pegados a la base de la pantalla.

---

## 4. Rediseño de la Pantalla de Ajustes (`SettingsScreen`)

1. **Tarjeta de Google Health Connect:**
   * Utilizará el nuevo `primaryContainer` verde musgo (`#1D4724`) cuando esté conectado, con textos en `#A8F7AF` y switch con acentos en `#8CE593`.
   * Esto erradica el color morado por defecto y genera una sensación armónica de éxito y salud.
2. **Campos y Selectores:**
   * Los radio buttons de selección de motor de visión utilizarán los colores temáticos de la paleta Palta.
   * Las tarjetas tendrán bordes definidos con `MaterialTheme.colorScheme.outlineVariant` para dar un acabado nítido y moderno.

---

## 5. Estrategia de Pruebas y Verificación
* **Compose Previews:** Se actualizarán y crearán previews tanto en modo claro como en modo oscuro con configuraciones de pantalla pequeña (ej. Nexus 4 / pantalla angosta) y grande.
* **Pruebas de Insets:** Verificar que la barra inferior no se corte con botones de navegación de 3 botones de Android ni con la barra de gestos moderna.
* **Compilación y APK:** Validar compilación limpia mediante `./gradlew assembleDebug` y envío a Telegram.
