@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo [看了么] 开始编译 Release APK...

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
    echo 建议先运行 scripts\windows\build_debug.bat 验证环境，再用 Android Studio 或 Gradle Wrapper 编译 Release。
    echo.
    pause
    exit /b 1
)

echo [INFO] 使用 Gradle：%GRADLE_CMD%
call "%GRADLE_CMD%" --no-daemon :app:assembleRelease

if errorlevel 1 (
    echo.
    echo [FAILED] Release 编译失败，请查看上方错误日志。
    echo 如果失败位置在 R8，请优先检查重复函数、重复类名或资源混淆规则。
    pause
    exit /b 1
)

echo.
echo [OK] Release APK 编译成功：
echo app\build\outputs\apk\release\app-release-unsigned.apk
pause
