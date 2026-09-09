# Diseño Técnico: Márgenes de Suplementos y Feedback de Configuración

## 1. Visión General de Arquitectura

El cambio aborda dos áreas específicas de la capa de presentación (`presentation`) en Jetpack Compose:
1. **Espaciado y jerarquía visual en `SupplementsScreen`** para resolver la oclusión generada por la barra inferior flotante (`AppBottomNavigationBar`).
2. **Mecanismo de retroalimentación en `SettingsScreen`** al persistir las preferencias del usuario, asegurando visibilidad tanto en la capa Compose (`SnackbarHost` elevado) como a nivel de sistema (`Toast` nativo).

Ambas intervenciones respetan la arquitectura unidireccional (UDF) y las pautas de Material Design 3 del proyecto.

---

## 2. Decisiones de Diseño Técnico

### A. Geometría y Márgenes en `SupplementsScreen`

#### Problema
`SupplementsScreen` se encuentra contenida en el `Box` raíz de `MainActivity`, sobre el cual se superpone `AppBottomNavigationBar` fijada en `Alignment.BottomCenter`. La barra de navegación ocupa aproximadamente 80.dp de altura más insets del sistema (barra de navegación gestual o de tres botones de Android). Al usar solo `Spacer(modifier = Modifier.height(32.dp))` al final del `LazyColumn`, los últimos elementos de la lista quedan cubiertos.

#### Solución
- Reemplazar el padding directo del `LazyColumn` por `contentPadding`:
  ```kotlin
  LazyColumn(
      modifier = Modifier
          .fillMaxSize()
          .padding(top = paddingValues.calculateTopPadding()),
      contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = 112.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  )
  ```
- **Ventajas:**
  - El contenido puede scrollear libremente debajo de la barra inferior con efecto translúcido si existiese, y al alcanzar el final, los 112.dp de margen garantizan que la última tarjeta y sus botones de eliminar/switch queden completamente por encima de la barra.
  - La barra de scroll se renderiza en el borde del contenedor sin quedar recortada a 16.dp del margen lateral.
- Elevar el `SnackbarHost`:
  ```kotlin
  snackbarHost = {
      SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.padding(bottom = 96.dp)
      )
  }
  ```
  Esto evita que los mensajes de éxito o error al estimar o guardar suplementos se dibujen debajo de `AppBottomNavigationBar`.

---

### B. Feedback al Guardar en `SettingsScreen`

#### Problema
Al presionar el botón "Guardar configuración", `SettingsScreen` llama a `snackbarHostState.showSnackbar(savedSuccessMessage)`. Sin embargo:
1. El `SnackbarHost` actual no tiene margen inferior (`bottom padding`), posicionando el snackbar en la coordenada `Y` más baja de la pantalla, la cual está completamente tapada por `AppBottomNavigationBar`.
2. Si el teclado en pantalla está cerrándose o si el usuario sale de la vista, el snackbar de Compose puede perder visibilidad.

#### Solución Multicapa de Feedback
1. **Elevación del `SnackbarHost`:**
   ```kotlin
   snackbarHost = {
       SnackbarHost(
           hostState = snackbarHostState,
           modifier = Modifier.padding(bottom = 96.dp)
       )
   }
   ```
   Asegura que cualquier mensaje (`userMessage`, `errorMessage` o `savedSuccessMessage`) se muestre flotando holgadamente sobre la barra de navegación.

2. **Notificación Inmediata por `Toast`:**
   - En el callback del botón de guardado, invocar `Toast.makeText(context, savedSuccessMessage, Toast.LENGTH_SHORT).show()`.
   - El `Toast` de Android se dibuja en una ventana de sistema independiente, garantizando retroalimentación visible instantánea aun si hay animaciones o transiciones en curso.

3. **Estado Visual en el Botón de Guardado:**
   - Mantener o reforzar la respuesta visual del botón al hacer click (por ejemplo, mostrando el ícono de verificación o retroalimentación háptica leve) para brindar una experiencia pulida y responsiva.

---

## 3. Contratos de Interfaz y Recursos de Texto

- Se utiliza el recurso de texto existente en `strings.xml` y `values-es-rCL/strings.xml`:
  `R.string.msg_settings_saved` ("Configuración guardada correctamente.").
- En `SupplementsScreen`, los strings de retroalimentación existentes:
  `R.string.supplements_saved_success` ("Suplemento guardado correctamente.").

---

## 4. Pruebas y Validación

1. **Pruebas de Composición (Previews):**
   - Verificar `SupplementsScreenLightPreview` y `SupplementsScreenDarkPreview` con listas largas de suplementos para constatar que el padding inferior se aplique uniformemente.
   - Verificar `SettingsScreen` con el `SnackbarHost` elevado.
2. **Pruebas de Interacción:**
   - Simular pulsación de "Guardar configuración" y comprobar la aparición tanto del Toast como del Snackbar flotante sobre la barra.
   - Navegar hasta el fondo de `Mis Suplementos` con 5+ suplementos y validar que todos los botones de eliminar y switches sean accesibles sin solapamientos.
