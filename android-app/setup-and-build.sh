#!/bin/bash
# HF Radar Android App - Quick Setup and Build Script (Linux/Mac)
# This script sets up the project structure and builds the APK

echo "================================================"
echo "HF Radar Android App - Build Script"
echo "================================================"
echo ""

# Check if already set up
if [ -d "app/src" ]; then
    echo "Project already set up. Skipping structure creation..."
else
    echo "Step 1: Creating project structure..."
    mkdir -p app

    echo "Step 2: Moving source files..."
    mv src app/src
    mv build.gradle app/build.gradle
    mv build.gradle.root build.gradle

    echo "Step 3: Making gradlew executable..."
    chmod +x gradlew

    echo "Structure created successfully!"
    echo ""
fi

echo "Step 4: Building APK (this may take 2-10 minutes on first build)..."
echo ""
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "================================================"
    echo "BUILD SUCCESSFUL!"
    echo "================================================"
    echo ""
    echo "APK Location:"
    echo "$PWD/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "File size:"
    ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}'
    echo ""
    echo "You can now install this APK on Android devices!"
    echo ""
else
    echo ""
    echo "================================================"
    echo "BUILD FAILED!"
    echo "================================================"
    echo ""
    echo "Please check the error messages above."
    echo "Common issues:"
    echo "- Java JDK not installed"
    echo "- Internet connection required for first build"
    echo "- Insufficient disk space"
    echo ""
    exit 1
fi
