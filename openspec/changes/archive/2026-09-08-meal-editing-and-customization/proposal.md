# Propuesta: Edición de Nombres de Alimentos y Adición de Ingredientes Faltantes

## 1. Problema y Justificación

Al registrar y gestionar comidas en el día a día, surgen dos necesidades habituales de ajuste fino que actualmente la aplicación no soporta:

1. **Nombres de alimentos genéricos o imprecisos de la IA:**
   Tanto los modelos de visión como los analizadores de texto pueden devolver nombres técnicos o genéricos (por ejemplo, *"Carne vacuna picada con vegetales"* en vez de *"Charquicán"*, o *"Pan blanco de molde"* en lugar de *"Media marraqueta tostada"*). En las pantallas de revisión (`FoodScanReviewScreen`) y de edición de comidas guardadas (`EditMealScreen`), el nombre del alimento se presenta como texto estático de solo lectura. El usuario no puede personalizar ni corregir el nombre para reflejar con exactitud lo que comió.

2. **Imposibilidad de agregar ingredientes faltantes a una comida ya guardada:**
   En la vida cotidiana es muy común olvidar un ingrediente al momento de guardar (por ejemplo, el aceite de oliva de la ensalada, una cucharada de mayonesa, una fruta de postre o un huevo duro adicional). Actualmente, la pantalla `EditMealScreen` únicamente permite modificar los gramos de los alimentos ya existentes o eliminarlos; no existe ningún botón ni flujo para incorporar ingredientes faltantes. Esto obliga al usuario a borrar toda la comida y volver a escanearla, o a crear un registro suelto redundante.

---

## 2. Alcance (In-Scope)

- **Edición del nombre de alimentos detectados:**
  - Permitir tocar el nombre o presionar un botón de edición rápido junto a cada alimento tanto en `FoodScanReviewScreen` como en `EditMealScreen`.
  - Desplegar un diálogo limpio y accesible (*"Editar nombre del alimento"*) con validación de texto no vacío.
  - Actualizar de inmediato el modelo `ScannedFoodItem` en el estado de la UI y persistir el nombre editado en la base de datos Room.

- **Incorporación de ingredientes faltantes en `EditMealScreen`:**
  - Agregar un botón prominente y accesible `+ Agregar ingrediente` en la lista de alimentos de `EditMealScreen`.
  - Proporcionar un diálogo modal con dos alternativas de ingreso:
    1. **Búsqueda en catálogo local chileno:** Selector con buscador reactivo de alimentos predeterminados (palta, huevo, arroz, marraqueta, pechuga de pollo, aceite, etc.) que asigna automáticamente porción y macronutrientes sin necesidad de internet ni consumo de IA.
    2. **Entrada rápida personalizada o con IA:** Campo para ingresar un alimento personalizado (nombre, gramos y macros opcionales) o descripción rápida.
  - Recalcular de forma reactiva e instantánea los totales nutricionales (`totalCalories`, `totalProtein`, `totalCarbs`, `totalFat`) de la comida.
  - Al guardar los cambios, actualizar la entidad `MealEntryEntity` y sus ítems en Room, sincronizando el cambio en Health Connect si está habilitado.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Configuración de metas calóricas diarias en Ajustes (forma parte de la **Propuesta 3: `daily-goals-streaks-and-supplements`**).
- Módulo de hábitos/suplementos diarios y sistema de rachas (forma parte de la **Propuesta 3: `daily-goals-streaks-and-supplements`**).
- Creación de un editor de recetas complejas de múltiples pasos culinarios.
