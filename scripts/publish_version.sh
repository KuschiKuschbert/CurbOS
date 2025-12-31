#!/bin/bash

# Usage: ./publish_version.sh <new_version>
# Example: ./publish_version.sh 0.2.4

NEW_VERSION=$1

# Ensure JAVA_HOME is set
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

if [ -z "$NEW_VERSION" ]; then
    echo "Usage: ./publish_version.sh <new_version>"
    echo "Example: ./publish_version.sh 0.2.4"
    exit 1
fi

echo "🚀 Publishing version $NEW_VERSION..."

# 1. Update build.gradle.kts
GRADLE_FILE="app/build.gradle.kts"
if [ ! -f "$GRADLE_FILE" ]; then
    # try full path if relative fails, or assume run from root of repo
    GRADLE_FILE="../app/build.gradle.kts"
fi
if [ ! -f "$GRADLE_FILE" ]; then
     # Try one more common location if script is in scripts/
     GRADLE_FILE="../CurbOS/app/build.gradle.kts"
fi
# Fallback to local relative check
if [ -f "app/build.gradle.kts" ]; then
    GRADLE_FILE="app/build.gradle.kts"
elif [ -f "../app/build.gradle.kts" ]; then
    GRADLE_FILE="../app/build.gradle.kts"
fi

if [ -f "$GRADLE_FILE" ]; then
    echo "Updating $GRADLE_FILE..."
    # Use sed to replace versionName
    # macOS sed requires empty string for -i
    sed -i '' "s/val baseVersionName = \".*\"/val baseVersionName = \"$NEW_VERSION\"/" "$GRADLE_FILE"
else
    echo "❌ Could not find app/build.gradle.kts"
    exit 1
fi

echo "🧹 Cleaning project to ensure fresh build..."
# Run from project root (assumed script is in scripts/ or root, handled by relative paths usually, but let's be safe)
# The script usually runs from root or scripts.
# Let's determine project root.
PROJECT_ROOT=$(dirname "$(dirname "$(realpath "$0")")")
cd "$PROJECT_ROOT"

if ./gradlew clean; then
    echo "✅ Project cleaned."
else
    echo "❌ Gradle clean failed."
    exit 1
fi

echo "🏗️  Building Release APK..."
# Changing to assembleRelease as this is a publish script.
if ./gradlew assembleRelease; then
    echo "✅ Release APK built successfully."
else
    echo "❌ Build failed. Aborting publish."
    exit 1
fi

# 2. Update Supabase
# We need SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY
if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_SERVICE_ROLE_KEY" ]; then
    echo "⚠️  Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY environment variables."
    echo "Skipping Supabase remote version update..."
    SKIP_SUPABASE=true
else
    SKIP_SUPABASE=false
fi

if [ "$SKIP_SUPABASE" = false ]; then
    echo "Updating Supabase..."
    RESPONSE=$(curl -s -X POST "$SUPABASE_URL/rest/v1/app_settings" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -H "Prefer: resolution=merge-duplicates" \
    -d "{ \"key\": \"android_version\", \"value\": \"\\\"$NEW_VERSION\\\"\" }")
else
    echo "⏭️  Skipped Supabase update."
fi

echo "✅ Version updated to $NEW_VERSION in Gradle and Supabase!"
