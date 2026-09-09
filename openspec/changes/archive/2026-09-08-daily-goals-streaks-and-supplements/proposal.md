# Propuesta: Metas Nutricionales Personalizadas, Suplementos Diarios y Registro de Rachas

## 1. Problema y Justificación

Para consolidar el uso diario de la aplicación y adaptarse a pautas nutricionales profesionales, se identificaron tres requerimientos fundamentales:

1. **Inflexibilidad en la meta diaria ante indicaciones profesionales (Nutricionista / Ejercicio):**
   Actualmente, el presupuesto calórico y de macronutrientes está fijado por defecto en 2000 kcal (150g P, 200g C, 65g G) o restringido a fórmulas estándar. Cuando un profesional de la salud o nutricionista indica aumentar o ajustar las calorías (por ejemplo, debido a un aumento en la carga de entrenamiento o cambio de fase deportiva), el usuario no tiene ninguna interfaz en la aplicación para configurar manualmente su meta diaria ni personalizar la distribución de gramos de proteína, carbohidratos y grasas.

2. **Falta de un sistema ágil para registrar complementos y suplementos (Omega 3):**
   Muchos usuarios consumen suplementos en su rutina diaria (como cápsulas de Omega 3, creatina, multivitamínicos o magnesio). Registrar estos complementos como si fuesen un plato de comida tradicional (creando una comida o buscando con cámara) es engorroso y genera fricción. Se requiere una vía rápida, tipo checklist diario, donde con un solo toque se pueda marcar que se consumió el suplemento del día y contabilizar su aporte nutricional si corresponde.

3. **Necesidad de motivación y seguimiento de constancia (Rachas de "Días Buenos"):**
   El éxito de un plan nutricional depende de la adherencia. El usuario desea registrar sus 3 comidas principales diarias (Desayuno, Almuerzo, Once / Cena). Cuando un día cuenta con las 3 comidas registradas en la aplicación, se considera un "Día bueno". Hace falta un indicador visual motivador de racha (días consecutivos cumplidos) que premie la constancia y recuerde activamente qué comida falta registrar para no perder la racha.

---

## 2. Alcance (In-Scope)

- **Configuración de Meta Nutricional en Ajustes:**
  - Nueva tarjeta en `SettingsScreen`: *"Meta Nutricional Diaria"*.
  - Campos editables para Calorías Objetivo (kcal), Proteína (g), Carbohidratos (g) y Grasas (g).
  - Herramienta rápida de cálculo/balance según porcentajes o aporte estándar (4 kcal/g proteína y carbos, 9 kcal/g grasa).
  - Persistencia reactiva en `UserPreferencesRepository` (DataStore), impactando inmediatamente en `DailySummaryScreen`.

- **Módulo y Widget de "Mis Suplementos" en el Resumen Diario:**
  - Definición y persistencia de suplementos activos en el perfil del usuario (con Omega 3 precargado por defecto: 2 cápsulas, ~18 kcal, 2g grasas).
  - Widget interactivo en `DailySummaryScreen` con checkboxes para marcar suplementos tomados en la fecha seleccionada.
  - Al marcar el suplemento, sumar opcionalmente sus calorías y macros al total del día.
  - Diálogo en Ajustes para gestionar qué suplementos toma habitualmente el usuario (agregar/editar dosis).

- **Sistema de Rachas ("Días Buenos" con las 3 comidas):**
  - Regla de dominio: Un día calendario se clasifica como "Día Bueno / Cumplido" si contiene al menos un registro en cada una de las 3 categorías principales: `Desayuno`, `Almuerzo` y `Once / Cena`.
  - Algoritmo de racha que calcula los días consecutivos cumplidos hasta la fecha actual.
  - Widget motivador en la cabecera de `DailySummaryScreen`:
    - Contador visual: `🔥 Racha: X días seguidos`.
    - Indicador de estado del día: `[✓] Desayuno  [✓] Almuerzo  [ ] Once / Cena`.
    - Mensaje proactivo: *"¡Te falta registrar Once / Cena para asegurar la racha de hoy!"* o *"¡Día completado con éxito!"*.
  - Distintivo visual en el selector de fecha semanal para identificar los días cumplidos.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Notificaciones push programadas en segundo plano o alarmas para tomar suplementos (se puede contemplar en un cambio posterior de recordatorios).
- Gamificación compleja con medallas o niveles de usuario.
- Sincronización en la nube con cuentas externas o redes sociales.
