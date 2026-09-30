@echo off
setlocal enabledelayedexpansion

:: ============================================================
::  JBT - Build Script
::  สร้าง .exe ด้วย jpackage (app-image, portable)
::  Usage: build_release.bat [version]
::  Example: build_release.bat 1.0.2
:: ============================================================

:: ── Version ──────────────────────────────────────────────────
set VERSION=%~1
if "%VERSION%"=="" set VERSION=1.0.1
:: NOTE: jpackage ต้องการ version แบบ x.y.z (ตัวเลขและจุดเท่านั้น)
::       เช่น: build_release.bat 1.0.2   (OK)
::             build_release.bat 1.0.2-beta  (ERROR!)

:: ── Paths ────────────────────────────────────────────────────
set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%src\main\java
set OUT_DIR=%PROJECT_DIR%build\RELEASE-v%VERSION%
set JAR_INPUT=%OUT_DIR%\jpackage-input
set TEMP_CLASSES=%PROJECT_DIR%temp_classes

echo.
echo ============================================================
echo  JBT Build Script ^| Version: %VERSION%
echo  Output: %OUT_DIR%
echo ============================================================
echo.

:: ── Clean up previous build artifacts ────────────────────────
if exist "%TEMP_CLASSES%" (
    echo [1/4] Cleaning temp_classes...
    rmdir /s /q "%TEMP_CLASSES%"
)
if exist "%JAR_INPUT%" (
    echo [1/4] Cleaning previous jpackage-input...
    rmdir /s /q "%JAR_INPUT%"
)

:: ── Create directories ───────────────────────────────────────
mkdir "%TEMP_CLASSES%" 2>nul
mkdir "%JAR_INPUT%"    2>nul

:: ── Step 1: Compile ──────────────────────────────────────────
echo [1/4] Compiling Java source files...
javac -d "%TEMP_CLASSES%" ^
    "%SRC_DIR%\JBTConfig.java" ^
    "%SRC_DIR%\Logger.java"    ^
    "%SRC_DIR%\FileManager.java" ^
    "%SRC_DIR%\Build.java"     ^
    "%SRC_DIR%\GUI.java"       ^
    "%SRC_DIR%\Main.java"

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed!
    rmdir /s /q "%TEMP_CLASSES%"
    pause
    exit /b 1
)
echo        OK

:: ── Step 2: Package JAR ──────────────────────────────────────
echo [2/4] Packaging into JBT.jar...

:: Write manifest to temp file
echo Manifest-Version: 1.0> "%TEMP_CLASSES%\MANIFEST.MF"
echo Main-Class: Main>> "%TEMP_CLASSES%\MANIFEST.MF"
echo.>> "%TEMP_CLASSES%\MANIFEST.MF"

jar --create ^
    --file="%JAR_INPUT%\JBT.jar" ^
    --manifest="%TEMP_CLASSES%\MANIFEST.MF" ^
    -C "%TEMP_CLASSES%" .

if errorlevel 1 (
    echo.
    echo [ERROR] JAR packaging failed!
    rmdir /s /q "%TEMP_CLASSES%"
    pause
    exit /b 1
)
echo        OK

:: ── Step 3: Cleanup temp classes ─────────────────────────────
echo [3/4] Cleaning up temp_classes...
rmdir /s /q "%TEMP_CLASSES%"
echo        OK

:: ── Step 4: jpackage → .exe ──────────────────────────────────
echo [4/4] Running jpackage (this may take a moment)...
jpackage ^
    --type app-image ^
    --name JBT ^
    --app-version %VERSION% ^
    --input "%JAR_INPUT%" ^
    --main-jar JBT.jar ^
    --main-class Main ^
    --dest "%OUT_DIR%"

if errorlevel 1 (
    echo.
    echo [ERROR] jpackage failed!
    pause
    exit /b 1
)
echo        OK

:: ── Done ─────────────────────────────────────────────────────
echo.
echo ============================================================
echo  BUILD SUCCESSFUL!
echo  EXE: %OUT_DIR%\JBT\JBT.exe
echo ============================================================
echo.
pause
