@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo [看了么] 开始编译 Debug APK...

if not exist "local.properties" (
    set "SDK_DIR=%ANDROID_HOME%"
    if "%SDK_DIR%"=="" set "SDK_DIR=%ANDROID_SDK_ROOT%"
    if not "%SDK_DIR%"=="" (
        set "SDK_DIR=!SDK_DIR:\=/!"
        > "local.properties" echo sdk.dir=!SDK_DIR!
        echo [OK] 已根据 Android SDK 环境变量创建 local.properties
    ) else (
        echo [INFO] 未检测到 ANDROID_HOME / ANDROID_SDK_ROOT；如果后续失败，请先在 Android Studio 中配置 SDK。
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
    echo [ERROR] 没有找到 gradlew.bat、系统 gradle 或 Android Studio 自带 Gradle。
    echo 解决方式：
    echo 1. 用 Android Studio 打开本项目，然后点击 Build APK；或
    echo 2. 在项目根目录生成 Gradle Wrapper；或
    echo 3. 安装 Gradle 并加入 PATH。
    echo.
    echo 成功后 APK 通常位于：app\build\outputs\apk\debug\app-debug.apk
    pause
    exit /b 1
)

echo [INFO] 使用 Gradle：%GRADLE_CMD%
call "%GRADLE_CMD%" --no-daemon :app:assembleDebug

if errorlevel 1 (
    echo.
    echo [FAILED] 编译失败，请查看上方错误日志。
    pause
    exit /b 1
)

echo.
echo [OK] Debug APK 编译成功：
echo app\build\outputs\apk\debug\app-debug.apk
pause
