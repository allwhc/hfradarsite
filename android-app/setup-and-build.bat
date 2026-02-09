@echo off
REM HF Radar Android App - Quick Setup and Build Script (Windows)
REM This script sets up the project structure and builds the APK

echo ================================================
echo HF Radar Android App - Build Script
echo ================================================
echo.

REM Check if already set up
if exist "app\src" (
    echo Project already set up. Skipping structure creation...
    goto build
)

echo Step 1: Creating project structure...
mkdir app 2>nul

echo Step 2: Moving source files...
move src app\src >nul 2>&1
move build.gradle app\build.gradle >nul 2>&1
move build.gradle.root build.gradle >nul 2>&1

echo Structure created successfully!
echo.

:build
echo Step 3: Building APK (this may take 2-10 minutes on first build)...
echo.
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ================================================
    echo BUILD SUCCESSFUL!
    echo ================================================
    echo.
    echo APK Location:
    echo %CD%\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo File size:
    for %%A in (app\build\outputs\apk\debug\app-debug.apk) do echo %%~zA bytes
    echo.
    echo You can now install this APK on Android devices!
    echo.
) else (
    echo.
    echo ================================================
    echo BUILD FAILED!
    echo ================================================
    echo.
    echo Please check the error messages above.
    echo Common issues:
    echo - Java JDK not installed
    echo - Internet connection required for first build
    echo - Insufficient disk space
    echo.
)

pause
