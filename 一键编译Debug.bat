@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo [Kanleme] Compiling Debug APK...

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
    echo Solutions:
    echo 1. Open the project in Android Studio and click Build APK.
    echo 2. Generate Gradle Wrapper in the project root.
    echo 3. Install Gradle and add it to PATH.
    echo.
    echo After success, the APK is at: app\build\outputs\apk\debug\app-debug.apk
    pause
    exit /b 1
)

echo [INFO] Using Gradle: %GRADLE_CMD%
call "%GRADLE_CMD%" --no-daemon :app:assembleDebug

if errorlevel 1 (
    echo.
    echo [FAILED] Build failed. Check the log above.
    pause
    exit /b 1
)

echo.
echo [OK] Debug APK built:
echo app\build\outputs\apk\debug\app-debug.apk
pause
