#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# build.sh — Compile Twitch Bridge v2
#
# Usage:
#   chmod +x build.sh && ./build.sh
#
# Override install path:
#   STARSECTOR_DIR=/opt/starsector ./build.sh
# ---------------------------------------------------------------------------

set -e

STARSECTOR_DIR="${STARSECTOR_DIR:-/usr/share/java/starsector}"
STARFARER_API="$STARSECTOR_DIR/starfarer.api.jar"
CLASSPATH="$STARFARER_API:/usr/share/java/starsector/json.jar"
CLASSPATH="$CLASSPATH:/usr/share/java/starsector/log4j-1.2.9.jar"
CLASSPATH="$CLASSPATH:$HOME/.local/share/starsector/mods/LunaLib/jars/LunaLib.jar"

MOD_JAR="TwitchBridge.jar"
SRC_DIR="src"
OUT_DIR="out"

if [ ! -f "$STARFARER_API" ]; then
    echo "ERROR: starfarer.api.jar not found at: $STARFARER_API"
    echo "Set STARSECTOR_DIR to your Starsector install folder."
    exit 1
fi

echo "==> Cleaning..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "==> Compiling..."
find "$SRC_DIR" -name "*.java" | xargs javac \
    -source 8 -target 8 \
    -cp "$CLASSPATH" \
    -d "$OUT_DIR"

echo "==> Packaging $MOD_JAR..."
jar cf "$MOD_JAR" -C "$OUT_DIR" .

echo ""
echo "Done! Mod folder layout:"
echo "  mods/TwitchBridge/"
echo "    mod_info.json"
echo "    TwitchBridge.jar"
echo "    data/config/"
echo "      twitchbridge_settings.json   <- set your channel name here"
