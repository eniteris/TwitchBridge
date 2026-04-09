#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# build.sh — Compile the Hello World Starsector mod into a .jar
#
# Usage:
#   chmod +x build.sh
#   ./build.sh
#
# Adjust STARSECTOR_DIR if your Starsector install is elsewhere.
# ---------------------------------------------------------------------------

set -e

STARSECTOR_DIR="${STARSECTOR_DIR:-/usr/share/java/starsector}"
STARFARER_API="$STARSECTOR_DIR/starfarer.api.jar"
CLASSPATH="$STARFARER_API:$HOME/.local/share/starsector/mods/TwitchBridge/TwitchBridge.jar"
CLASSPATH="$CLASSPATH:$HOME/.local/share/starsector/mods/LunaLib/jars/LunaLib.jar"
MOD_JAR="ChattersCrew.jar"
SRC_DIR="src"
OUT_DIR="out"

# Verify the API jar exists
if [ ! -f "$STARFARER_API" ]; then
    echo "ERROR: Could not find starfarer.api.jar at:"
    echo "  $STARFARER_API"
    echo ""
    echo "Set STARSECTOR_DIR to your Starsector install folder, e.g.:"
    echo "  STARSECTOR_DIR=/opt/starsector ./build.sh"
    exit 1
fi

echo "==> Cleaning output directory..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "==> Compiling Java sources..."
find "$SRC_DIR" -name "*.java" | xargs javac \
    -source 8 -target 8 \
    -cp "$CLASSPATH" \
    -d "$OUT_DIR"

echo "==> Packaging into $MOD_JAR..."
jar cf "$MOD_JAR" -C "$OUT_DIR" .

