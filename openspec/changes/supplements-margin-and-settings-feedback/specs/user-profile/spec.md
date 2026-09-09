# Especificación Delta: Perfil de Usuario y Ajustes (Feedback al Guardar)

## Requisitos MODIFICADOS

### Requisito: Persistencia de Ajustes y Feedback al Usuario
El sistema DEBE proporcionar retroalimentación perceptible, inmediata y consistente al usuario cada vez que guarde sus configuraciones (metas calóricas, macronutrientes, ventanas horarias, proveedor de IA y sincronizaciones), garantizando que las notificaciones no queden ocultas detrás de componentes flotantes de navegación.

#### Escenario: Guardado exitoso con feedback múltiple (Snackbar y Toast)
- **DADO** que el usuario se encuentra en la pantalla de Ajustes y modifica sus metas o parámetros
- **CUANDO** el usuario presiona el botón "Guardar configuración"
- **ENTONCES** el sistema DEBE persistir los cambios en las preferencias locales y mostrar de inmediato un aviso visible (Toast y Snackbar elevado sobre la barra de navegación) indicando "Configuración guardada correctamente." en español chileno.

#### Escenario: Notificaciones de error o restauración sobre la barra de navegación
- **DADO** que se produce una notificación informativa, de restauración o de error en la pantalla de Ajustes
- **CUANDO** el mensaje se emite a través de `SnackbarHost`
- **ENTONCES** el mensaje DEBE flotar con un margen inferior de al menos 96.dp, evitando cualquier solapamiento con la barra de navegación inferior.
