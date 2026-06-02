@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo [Kanleme] Compiling Release APK...

if not exist "local.properties" (
    set "SDK_DIR=%ANDROID_HOME%"
    if "%SDK_DIR%"=="" set "SDK_DIR=%ANDROID_SDK_ROOT%"
    if not "%SDK_DIR%"=="" (
        set "SDK_DIR=!SDK_DIR:\=/!"
        > "local.properties" echo sdk.dir=!SDK_DIR!
        echo [OK] Created local.properties from ANDROID_HOME.
    ) else (
        echo [INFO] ANDROID_HOME / ANDROID_SDK_ROOT not detected. Please configure SDK in Android Studio.
    )
)

set "GRADLE_CMD="

if exist "gradlew.bat" (
    set "GRADLE_CMD=gradlew.bat"
) else (
    where gradle >nul 2>nul
    if not errorlevel 1 set "GRADLE_CMD=gradle"
)

if "%GRADLE_CMD%"=="" (
    for /f "delims=" %%G in ('dir /b /s "C:\Program Files\Android\Android Studio\gradle\gradle-*\bin\gradle.bat" 2^>nul') do (
        if "!GRADLE_CMD!"=="" set "GRADLE_CMD=%%G"
    )
)

if "%GRADLE_CMD%"=="" (
    for /f "delims=" %%G in ('dir /b /s "%LOCALAPPDATA%\Programs\Android Studio\gradle\gradle-*\bin\gradle.bat" 2^>nul') do (
        if "!GRADLE_CMD!"=="" set "GRADLE_CMD=%%G"
    )
)

if "%GRADLE_CMD%"=="" (
    echo.
    echo [ERROR] gradlew.bat, system gradle, or Android Studio Gradle not found.
    echo Try running build_debug.bat first to validate the environment.
    echo.
    pause
    exit /b 1
)

echo [INFO] Using Gradle: %GRADLE_CMD%
call "%GRADLE_CMD%" --no-daemon :app:assembleRelease

if errorlevel 1 (
    echo.
    echo [FAILED] Release build failed. Check the log above.
    echo If R8 failed, check for duplicate functions, class names, or resource shrinker rules.
    pause
    exit /b 1
)

echo.
echo [OK] Release APK built:
echo app\build\outputs\apk\release\app-release-unsigned.apk
echo.
echo To sign the APK, run apksigner:
echo apksigner sign --ks keepix-release.p12 --ks-key-alias keepix-release --out app-release-signed.apk app-release-unsigned.apk
pause
