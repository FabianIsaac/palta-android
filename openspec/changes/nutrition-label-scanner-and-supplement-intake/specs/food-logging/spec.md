# Delta Specification: Food Logging (Nutrition Label Scanning for Custom Ingredients)

## Requisitos AÑADIDOS

### Requisito: AI-Assisted Nutrition Label Scanning for Custom Ingredients
El sistema DEBE permitir escanear una tabla nutricional desde la cámara o galería al agregar un ingrediente personalizado en una comida (`AddIngredientSheet`), rellenando automáticamente el tamaño de la porción en gramos, las calorías y los macronutrientes correspondientes.

#### Escenario: Escaneo de etiqueta nutricional en ingrediente personalizado
- **DADO** la pestaña "Personalizado" en la hoja inferior para agregar ingredientes a una comida
- **CUANDO** el usuario presiona "Escanear etiqueta" y selecciona una foto de una tabla nutricional
- **ENTONCES** el sistema DEBE procesar la imagen con el analizador de visión y completar automáticamente los campos de gramos de porción, calorías y macronutrientes (proteínas, carbohidratos y grasas).

#### Escenario: Ajuste y confirmación de ingrediente escaneado
- **DADO** un ingrediente cuyos datos fueron completados a partir del escaneo de su tabla
- **CUANDO** el usuario modifica o valida los valores y presiona "Agregar a la comida"
- **ENTONCES** el sistema DEBE registrar el ingrediente en la comida con los valores validados y actualizar los totales del día inmediatamente.
