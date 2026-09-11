#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Cargar variables de entorno si existen en .env
if [ -f "$ROOT_DIR/.env" ]; then
    export $(grep -v '^#' "$ROOT_DIR/.env" | xargs)
fi

BOT_TOKEN="${TELEGRAM_BOT_TOKEN:-8802595675:AAFrDSSteYtO6fkgbEPhwmEV3EAS5YfZw40}"
CHAT_ID="${TELEGRAM_CHAT_ID:-87183064}"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "Compilando APK actualizado..."
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$PATH"
"$ROOT_DIR/gradlew" assembleDebug

echo "Enviando APK a Telegram (Chat ID: $CHAT_ID)..."

RESPONSE=$(curl -s -X POST "https://api.telegram.org/bot$BOT_TOKEN/sendDocument" \
    -F chat_id="$CHAT_ID" \
    -F document=@"$APK_PATH" \
    -F caption="✅ <b>Calculadora de Calorías para Android</b>%0A📦 Versión: Debug APK%0A⚖️ Tamaño: $(ls -lh "$APK_PATH" | awk '{print $5}')" \
    -F parse_mode="HTML")

if echo "$RESPONSE" | grep -q '"ok":true'; then
    echo "¡APK enviado exitosamente a tu chat de Telegram!"
else
    echo "Error al enviar a Telegram: $RESPONSE"
    exit 1
fi
