# Delta de Especificación: Registro de Alimentos (food-logging)

## Requisitos Agregados y Modificados

### Requirement: Descomposición Atómica de Ingredientes en Lenguaje Natural
El sistema SHALL analizar descripciones de comidas compuestas (ej. fajitas, tacos, sándwiches, ensaladas, bowls) y desglosar CADA ingrediente, base, verdura, proteína y aderezo mencionado explícitamente en ítems individuales con su respectiva porción casera, gramos estimados y macronutrientes.

#### Scenario: Descomposición exitosa de fajitas con ingredientes múltiples
- **GIVEN** una descripción en lenguaje natural: `"me comi 2 fajitas con choclo, carne molida, lechuga, tomate aderezado con yogurt griego y tajin"`
- **WHEN** el analizador de lenguaje natural procesa la solicitud con conexión a la IA
- **THEN** el sistema SHALL retornar exactamente 7 ítems individuales ("Tortillas de fajita", "Carne molida", "Choclo", "Lechuga", "Tomate picado", "Yogurt griego", "Tajín")
- **AND** la porción de "Tortillas de fajita" SHALL corresponder a 2 unidades (~80 gramos) con sus calorías y macronutrientes proporcionales.

---

### Requirement: Preservación de Borrador ante Errores de Red o IA
El sistema SHALL conservar intacto el texto descriptivo ingresado por el usuario (`lastRawDescription`) cuando la llamada al servicio de IA falle por desconexión, tiempo de espera o límite de cuota, permitiendo al usuario reintentar o editar sin tener que reescribir.

#### Scenario: Error de conexión durante el análisis por texto
- **GIVEN** que el usuario ingresó una descripción extensa de su comida y la conexión a la API de IA falló
- **WHEN** la pantalla de revisión recibe el error del servicio
- **THEN** el sistema SHALL mantener la descripción original accesible en la interfaz
- **AND** el sistema SHALL mostrar las opciones "Reintentar" y "Editar texto", cargando el texto previo en el campo de edición al seleccionar "Editar texto".

---

### Requirement: Registro con Estimado Local y Pendiente de IA
El sistema SHALL permitir guardar una comida analizada con el catálogo local offline cuando no haya conexión disponible, marcando el registro con la bandera `isPendingAiRefinement` y conservando el texto original para su refinamiento posterior.

#### Scenario: Guardado offline con refinamiento diferido
- **GIVEN** un dispositivo sin conexión a internet y una descripción ingresada por el usuario
- **WHEN** el analizador local genera un estimado con los alimentos disponibles del catálogo local y el usuario presiona "Confirmar y Guardar"
- **THEN** el sistema SHALL persistir la comida en la base de datos Room con `isPendingAiRefinement` en verdadero y `rawDescription` con el texto ingresado
- **AND** la pantalla de Resumen Diario SHALL presentar la comida con la etiqueta "Estimado local - Pendiente de IA".

---

### Requirement: Sincronización y Refinamiento Automático al Recuperar Conexión
El sistema SHALL detectar el restablecimiento de la conectividad a internet y reanalizar automáticamente las comidas marcadas con `isPendingAiRefinement = true`, actualizando sus ingredientes, cantidades y macronutrientes con el análisis en la nube.

#### Scenario: Reanálisis automático tras recuperar internet
- **GIVEN** al menos una comida persistida en la base de datos con `isPendingAiRefinement = true` y una descripción en texto válida
- **WHEN** el dispositivo detecta conexión a internet activa
- **THEN** el sistema SHALL invocar el analizador de IA con el texto guardado
- **AND** el sistema SHALL actualizar los ítems, calorías y macronutrientes en Room, cambiando `isPendingAiRefinement` a falso y sincronizando con Health Connect si corresponde.
