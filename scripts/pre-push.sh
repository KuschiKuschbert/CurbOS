#!/bin/sh

# Ensure script fails on error
set -e

# Ensure JAVA_HOME is set (fallback to Android Studio JBR)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo "🚀 Running pre-push checks for Android..."

# Run Lint (Static Analysis)
# echo "running lintDebug..."
# ./gradlew lintDebug

# Run Compilation (Fastest check for syntax)
# using --daemon for speed
echo "running compileDebugKotlin..."
./gradlew compileDebugKotlin --daemon

if [ $? -ne 0 ]; then
    echo "❌ Android Build Failed! Push rejected."
    echo "Please fix the build errors before pushing."
    exit 1
fi

echo "✅ Android Build Passed!"
