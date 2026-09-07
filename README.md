# 🥑 Palta (palta-android)

**Palta** es una aplicación móvil nativa para Android diseñada para el cálculo y seguimiento inteligente de calorías y macronutrientes, adaptada cultural y gastronómicamente a **Chile** (`es-CL`), con arquitectura **Offline-First**, escaneo visual de comidas asistido por **Inteligencia Artificial** y sincronización con **Google Health Connect**.

---

## ✨ Características Principales

- 📸 **Escaneo Visual de Comidas con IA:**
  - Captura fotos de tu plato con **CameraX** o selecciónalas desde tu galería.
  - Análisis multimodal mediante la API de **MiniMax Vision** o motor local en el dispositivo (**LiteRT / MediaPipe**).
  - Reconocimiento de platos e ingredientes cotidianos en Chile (*Marraqueta, Palta hass, Cazuela, Porotos granados, Pastel de choclo, etc.*).
- ⚖️ **Revisión y Ajuste Interactivo (Human-in-the-Loop):**
  - Ajuste reactivo de porciones en gramos con recálculo determinista inmediato de calorías y macronutrientes.
  - Categorías estándar chilenas: *Desayuno*, *Almuerzo*, *Once / Cena*, *Colaciones*.
- 💓 **Sincronización con Google Health Connect:**
  - Exportación de registros de nutrición (`NutritionRecord`) para interoperabilidad con Google Fit, Samsung Health y otras aplicaciones.
  - Degradación agraciada (*graceful degradation*): si Health Connect no está disponible o no tiene permisos, los datos se guardan localmente sin interrumpir al usuario.
- 📱 **100% Offline-First y Privado:**
  - Base de datos local en **Room** y preferencias en **Jetpack DataStore**.
  - No requiere login obligatorio ni conexión a la nube para su funcionamiento básico.
- 🇨🇱 **Localización 100% Chilena (`es-CL`):**
  - Tuteo natural chileno sin voseo argentino.

---

## 🛠️ Stack Tecnológico

- **Plataforma:** Android Nativo (`minSdk = 26`, `targetSdk = 35` - Android 15)
- **Lenguaje:** Kotlin 2.1
- **UI Toolkit:** Jetpack Compose + Material Design 3 (Material You)
- **Arquitectura:** Clean Architecture + MVVM / Unidirectional Data Flow (UDF)
- **Cámara:** CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Salud:** AndroidX Health Connect Client (`connect-client:1.1.0-alpha11`)
- **Persistencia:** Room Database 2.6.1 + Jetpack DataStore Preferences
- **Red:** Ktor Client 3.0 + Kotlinx Serialization JSON
- **Testing:** JUnit 5, MockK, Turbine, Coroutines Test

---

## 🚀 Compilación y Ejecución

### Desde Android Studio
1. Clona el repositorio y abre la carpeta en Android Studio.
2. Espera a que finalice el Gradle Sync.
3. Ejecuta la app en tu emulador o dispositivo físico con Android 8.0+.

### Desde la Terminal
```bash
# Compilar el APK Debug
./gradlew assembleDebug

# Ejecutar las pruebas unitarias
./gradlew testDebugUnitTest
```
El APK se generará en:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 Envío Automático a Telegram

El proyecto incluye un script para compilar y enviar el APK directamente a tu teléfono por Telegram:

```bash
./scripts/send_apk_telegram.sh
```
*(Requiere configurar `TELEGRAM_BOT_TOKEN` y `TELEGRAM_CHAT_ID` en un archivo `.env` local).*

---

## 📄 Metodología de Desarrollo: OpenSpec

Este repositorio utiliza **OpenSpec** (Spec-Driven Development) para garantizar alta calidad, especificaciones vivas y trazabilidad de cambios:
- `openspec/project.md`: Constitución técnica y nutricional del sistema.
- `openspec/specs/`: Especificaciones maestras.
- `openspec/changes/`: Historial y propuestas activas de cambio.
