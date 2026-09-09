# Propuesta: Margen en Suplementos y Feedback Visual al Guardar Configuración

## 1. Contexto y Problema

Durante el uso de la aplicación en dispositivos móviles se detectaron dos inconvenientes de usabilidad que afectan la experiencia del usuario:

1. **Margen insuficiente en la pantalla de suplementos (`SupplementsScreen`):**
   - La pantalla de suplementos utiliza un `LazyColumn` con un espaciador inferior de solo `32.dp`.
   - Dado que la barra de navegación inferior flotante (`AppBottomNavigationBar`) tiene una altura de 80.dp más insets de navegación del sistema, los últimos suplementos de la lista y sus controles (botones de eliminar y switches de activación) quedan parcialmente ocultos o inaccesibles detrás de la barra al scrollear hasta el final.
   - Asimismo, el `SnackbarHost` de dicha pantalla se ubica al borde inferior de la ventana, quedando tapado por la barra de navegación cuando se muestran alertas de éxito o error.

2. **Ausencia de feedback perceptible al guardar las configuraciones (`SettingsScreen`):**
   - Cuando el usuario presiona el botón "Guardar configuración", la pantalla invoca `snackbarHostState.showSnackbar(savedSuccessMessage)`.
   - Sin embargo, el `SnackbarHost` de `SettingsScreen` está anclado a la base del `Scaffold` sin margen inferior. Al estar superpuesta la barra de navegación inferior flotante en `MainActivity`, el mensaje de Snackbar aparece detrás de la barra, haciendo que el usuario no vea ningún mensaje ni confirmación de guardado.
   - Además de elevar el `SnackbarHost` sobre la barra de navegación, es necesario proveer un aviso inmediato (mediante `Toast` nativo de Android y retroalimentación en el estado de la interfaz/botón) para que el usuario tenga certeza absoluta e instantánea de que sus cambios fueron persistidos con éxito.

---

## 2. Metas y Alcance

### In-Scope (Dentro del alcance)
* **Corrección de Márgenes y Padding en `SupplementsScreen`:**
  - Configurar `contentPadding` en el `LazyColumn` con `PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp)` alineándose con el estándar establecido en `DailySummaryScreen`.
  - Asegurar que el último elemento de la rutina de suplementos tenga holgura completa sobre la barra flotante.
  - Elevar el `SnackbarHost` de `SupplementsScreen` aplicando un margen inferior (`Modifier.padding(bottom = 96.dp)`) para que las notificaciones de estimación y guardado sean legibles.
* **Feedback Confiable e Inmediato en `SettingsScreen`:**
  - Elevar el `SnackbarHost` en `SettingsScreen` con un padding inferior adecuado (`Modifier.padding(bottom = 96.dp)`), permitiendo que cualquier Snackbar se dibuje flotando limpiamente sobre la barra de navegación.
  - Añadir notificación inmediata tipo `Toast` en español chileno (`"Configuración guardada correctamente."`) al presionar "Guardar configuración".
  - Proporcionar retroalimentación visual en el botón de guardado (por ejemplo, estado temporal de confirmación o indicador de éxito) o actualización reactiva a través del flujo de eventos y estado del ViewModel.
* **Consistencia Lingüística:**
  - Garantizar redacción 100% en español de Chile (`es-CL`) sin voseo en todos los mensajes y avisos creados o ajustados.

### Out-of-Scope (Fuera del alcance)
* Modificar la lógica de persistencia en `UserPreferencesRepository` o en la base de datos Room de suplementos.
* Modificar la estructura de navegación de destinos en `MainActivity`.
* Alterar las fórmulas de cálculo calórico o desglose de macronutrientes.

---

## 3. Criterios de Aceptación

1. En la pantalla de Suplementos, al desplazarse hasta el fondo de la lista, todas las tarjetas de suplementos y sus opciones (switch de activación y botón de eliminación) son 100% visibles y cómodamente clickeables por encima de la barra de navegación inferior.
2. Al presionar el botón "Guardar configuración" en la pantalla de Ajustes, el usuario recibe retroalimentación inmediata, visible y clara que confirma que la configuración fue guardada correctamente.
3. Las notificaciones tipo Snackbar en ambas pantallas (`SupplementsScreen` y `SettingsScreen`) no son solapadas ni obstruidas por la barra de navegación inferior.
4. Todo el texto y mensajes de retroalimentación respetan las normas de español chileno sin voseo.
