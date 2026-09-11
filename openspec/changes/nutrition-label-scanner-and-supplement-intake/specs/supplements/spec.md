# Delta Specification: Supplements (AI Label Scanning, Collapsible Suggestions & Intake Count)

## Requisitos MODIFICADOS

### Requisito: Supplements Catalog Management
El sistema DEBE proveer una interfaz a pantalla completa ("Mis Suplementos") que permita crear, activar, editar y eliminar suplementos nutricionales. La sección de sugerencias de suplementos preconfigurados DEBE presentarse colapsada por defecto para optimizar el espacio vertical, permitiendo expandirse a petición del usuario. Asimismo, cada tarjeta de suplemento en la rutina activa DEBE exhibir el contador total de tomas registradas en el historial.

#### Escenario: Sugerencias colapsadas por defecto
- **DADO** la pantalla "Mis Suplementos" al abrirse
- **CUANDO** el usuario visualiza la parte superior del contenido
- **ENTONCES** el sistema DEBE mostrar el bloque de sugerencias populares contraído, indicando la cantidad de sugerencias disponibles y un control para expandir.

#### Escenario: Expansión de sugerencias populares
- **DADO** la sección de sugerencias contraída
- **CUANDO** el usuario toca el encabezado o icono de expansión
- **ENTONCES** el sistema DEBE desplegar los chips de suplementos sugeridos para su activación directa con 1 toque.

#### Escenario: Visualización de tomas acumuladas en la rutina activa
- **DADO** un suplemento en la rutina que ha sido marcado como tomado en 12 fechas distintas
- **CUANDO** el usuario consulta su rutina en la pantalla "Mis Suplementos"
- **ENTONCES** la tarjeta de dicho suplemento DEBE mostrar "12 tomas registradas".

---

## Requisitos AÑADIDOS

### Requisito: AI-Powered Nutrition Label Image Scanning for Supplements
El sistema DEBE permitir escanear una tabla nutricional o envase de suplemento utilizando una fotografía capturada en el momento o seleccionada desde la galería del dispositivo. El analizador de visión con IA DEBE interpretar los valores nutricionales por porción (dosis/porción, calorías, proteínas, carbohidratos y grasas) y rellenar automáticamente los campos correspondientes del formulario de creación.

#### Escenario: Escaneo exitoso de tabla nutricional desde foto
- **DADO** el formulario de nuevo suplemento
- **CUANDO** el usuario selecciona una imagen de una etiqueta nutricional con porción "1 scoop (33g)", 110 kcal, 25g de proteína, 1g de carbohidratos y 1g de grasa
- **ENTONCES** el sistema DEBE poblar automáticamente los campos de dosis con "1 scoop (33g)", calorías con "110", proteínas con "25", carbohidratos con "1" y grasas con "1".

#### Escenario: Selección de origen de imagen (Cámara o Galería)
- **DADO** el formulario de nuevo suplemento
- **CUANDO** el usuario presiona el botón de escanear tabla nutricional
- **ENTONCES** el sistema DEBE ofrecer la opción de tomar una fotografía con la cámara o seleccionar un archivo desde la galería de imágenes del dispositivo.

#### Escenario: Manejo de etiqueta ilegible o error de análisis
- **DADO** una imagen borrosa o una falla de conectividad con el proveedor de IA
- **CUANDO** concluye el proceso de análisis
- **ENTONCES** el sistema DEBE notificar el inconveniente mediante un mensaje amigable en español de Chile, dejando los campos intactos para su ingreso manual sin interrumpir el flujo.
