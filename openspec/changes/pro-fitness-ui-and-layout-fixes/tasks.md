# Tareas de Implementación: Rediseño Visual "Fitness Tracker Pro - Edición Palta"

## Fase 1: Sistema de Diseño y Tematización Material 3
- [x] 1.1 Expandir y refactorizar `Color.kt` con la paleta integral "Palta Pro" (tonos claros, oscuros y macros atléticos). <!-- id: 1.1 -->
- [x] 1.2 Configurar exhaustivamente `Theme.kt` tanto para `DarkColorScheme` como para `LightColorScheme` (asignando `primaryContainer`, `onPrimaryContainer`, `surfaceVariant`, `surfaceContainer`, `outline`, etc.), eliminando cualquier fallback al morado por defecto. <!-- id: 1.2 -->

## Fase 2: Corrección de la Barra de Navegación Inferior
- [x] 2.1 Reestructurar `AppBottomNavigationBar.kt` eliminando el conflicto entre `Surface.clipToBounds`, `height(68.dp)` fijo y `navigationBarsPadding()`. <!-- id: 2.1 -->
- [x] 2.2 Garantizar que el botón central de escaneo mantenga su forma circular íntegra de 56.dp y elevación limpia sin cortes en ningún tipo de navegación del sistema. <!-- id: 2.2 -->
- [x] 2.3 Probar y validar `AppBottomNavigationBarLightPreview` y `AppBottomNavigationBarDarkPreview`. <!-- id: 2.3 -->

## Fase 3: Reorganización y Limpieza del Escáner de Comida
- [x] 3.1 Reestructurar el encabezado de `FoodCameraScreen.kt` separando la fila de navegación (Volver, Título, Estado IA, Accesos) de la instrucción contextual para evitar solapamientos de texto. <!-- id: 3.1 -->
- [x] 3.2 Eliminar el botón duplicado de IA en la cabecera superior y consolidar la acción en la píldora flotante inferior ("✨ Describir con IA"). <!-- id: 3.2 -->
- [x] 3.3 Calibrar los márgenes de seguridad en los controles inferiores (Galería, Disparador y Flash) para una experiencia ergonómica y despejada. <!-- id: 3.3 -->

## Fase 4: Pulido de Pantallas (Ajustes y Resumen Diario)
- [x] 4.1 Ajustar la tarjeta de Google Health Connect en `SettingsScreen.kt` para que adopte el nuevo contenedor verde armonioso (`primaryContainer`). <!-- id: 4.1 -->
- [x] 4.2 Ajustar `DailySummaryScreen.kt` para utilizar los nuevos colores de macronutrientes atléticos y los contenedores de íconos de comida en verde orgánico. <!-- id: 4.2 -->

## Fase 5: Verificación, Compilación y Despliegue
- [x] 5.1 Ejecutar suite de pruebas unitarias (`./gradlew testDebugUnitTest`). <!-- id: 5.1 -->
- [x] 5.2 Compilar el APK debug (`./gradlew assembleDebug`). <!-- id: 5.2 -->
- [x] 5.3 Enviar el APK generado al usuario por Telegram ejecutando `bash ./scripts/send_apk_telegram.sh`. <!-- id: 5.3 -->
