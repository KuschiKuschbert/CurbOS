#!/bin/bash

# Navigate to the project root (assuming script is in scripts/)
cd "$(dirname "$0")/.."

echo "🧹 Cleaning project..."
./gradlew clean

echo "🏗️  Building Debug APK..."
./gradlew assembleDebug

echo "✅ Build complete!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
