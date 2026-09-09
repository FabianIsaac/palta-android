# Especificación Delta: Suplementos (Margen y Presentación)

## Requisitos MODIFICADOS

### Requisito: Supplements Catalog Management
El sistema DEBE proveer una interfaz a pantalla completa ("Mis Suplementos") que permita crear, activar, editar y eliminar suplementos nutricionales, garantizando que el listado cuente con un margen inferior suficiente (`contentPadding` de al menos 112.dp) para evitar que la barra de navegación inferior flotante solape o impida la interacción con las últimas tarjetas y sus controles.

#### Escenario: Visualización completa de suplementos al final de la lista
- **DADO** que el usuario posee múltiples suplementos configurados en su rutina
- **CUANDO** se desplaza hasta el final de la pantalla de Suplementos
- **ENTONCES** el sistema DEBE mostrar el último elemento, su switch de activación y el botón de eliminar completamente visibles y clickeables por encima de la barra de navegación inferior.

#### Escenario: Visibilidad de mensajes informativos tipo Snackbar
- **DADO** que el usuario guarda un suplemento o realiza una estimación con IA
- **CUANDO** el sistema genera un mensaje de confirmación o error
- **ENTONCES** el mensaje de Snackbar DEBE presentarse elevado sobre la barra de navegación inferior, sin ser tapado ni recortado por esta.
