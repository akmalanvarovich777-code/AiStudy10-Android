@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title AiStudy10 Android APK yaratish
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not exist "%ANDROID_HOME%\platforms\android-35\android.jar" (
 echo [XATO] Android SDK API 35 topilmadi: %ANDROID_HOME%
 echo Avval Android Studio ni o'rnating va SDK Manager orqali Android API 35 ni yuklang.
 echo Android Studio: https://developer.android.com/studio
 pause
 exit /b 1
)
where java >nul 2>&1
if errorlevel 1 (
 if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
  set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
 ) else (
  echo [XATO] Java 17 yoki yangiroq talab qilinadi. Android Studio JBR topilmadi.
  pause
  exit /b 1
 )
)
set "GRADLE_EXE=%LOCALAPPDATA%\AiStudy10Build\gradle-8.9\bin\gradle.bat"
if not exist "%GRADLE_EXE%" (
 echo Gradle 8.9 yuklanmoqda. Internet kerak...
 powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $base=Join-Path $env:LOCALAPPDATA 'AiStudy10Build'; New-Item -ItemType Directory -Force -Path $base | Out-Null; $zip=Join-Path $base 'gradle-8.9-bin.zip'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $base -Force"
 if errorlevel 1 (
  echo [XATO] Gradle yuklab bo'lmadi. Internetni tekshiring.
  pause
  exit /b 1
 )
)
call "%GRADLE_EXE%" :app:assembleDebug --no-daemon
if errorlevel 1 (
 echo [XATO] APK yig'ishda xato chiqdi. Yuqoridagi xabarni saqlang.
 pause
 exit /b 1
)
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
 echo [XATO] APK yaratilmagan.
 pause
 exit /b 1
)
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "%~dp0AiStudy10_Android_debug.apk" >nul
 echo.
 echo [OK] APK tayyor: %~dp0AiStudy10_Android_debug.apk
 echo Telefoningizga ko'chirib o'rnatishingiz mumkin.
 pause
