#!/bin/bash
echo "🚀 Starting CurbOS Comprehensive Test Suite..."
cd "$(dirname "$0")"

# --- JAVA_HOME Auto-Detection ---
if [ -z "$JAVA_HOME" ]; then
    echo "🔍 JAVA_HOME not set. Attempting to locate..."
    
    # Try system java_home first
    if /usr/libexec/java_home &> /dev/null; then
        export JAVA_HOME=$(/usr/libexec/java_home)
        echo "✅ Found System Java: $JAVA_HOME"
    
    # Fallback to Android Studio (Standard Mac Install)
    elif [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
        export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
        echo "✅ Found Android Studio Java: $JAVA_HOME"
    
    else
        echo "⚠️  Could not locate Java. Tests may fail."
    fi
fi
# --------------------------------


echo "\n🧹 Cleaning Build Environment..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "⚠️ Clean failed, but attempting to proceed..."
fi

echo "\n📦 Running Unit Tests (Logic & ViewModels)..."
./gradlew testDebugUnitTest --info
if [ $? -ne 0 ]; then
    echo "❌ Unit Tests Failed!"
    exit 1
fi
echo "✅ Unit Tests Passed."

echo "\n📱 Running E2E / UI Tests on Connected Device..."
echo "Ensure your phone is connected and ADB is authorized."
./gradlew connectedDebugAndroidTest
if [ $? -ne 0 ]; then
    echo "❌ E2E Tests Failed!"
    exit 1
fi
echo "✅ E2E Tests Passed."

echo "\n🎉 All Systems Go!"
