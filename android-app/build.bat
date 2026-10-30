@echo off
set SDK=C:\Users\Diego\AppData\Local\Android\Sdk
set BUILD_TOOLS=%SDK%\build-tools\34.0.0
set PLATFORM=%SDK%\platforms\android-35\android.jar
set APP_DIR=c:\REA\norton-parental-clone\android-app
set PUBLIC_DIR=c:\REA\norton-parental-clone\public
set KEYSTORE=%APP_DIR%\build\debug.keystore

cd /d "%APP_DIR%"

echo [1/6] Compiling resources...
del /f /s /q build\obj\* 2>nul
del /f /s /q build\dex\* 2>nul
"%BUILD_TOOLS%\aapt2.exe" compile --dir res -o build\res.zip
"%BUILD_TOOLS%\aapt2.exe" link -I "%PLATFORM%" --manifest AndroidManifest.xml --java build\gen -o build\linked.apk build\res.zip --auto-add-overlay

echo [2/6] Compiling Java...
javac -source 8 -target 8 -cp "%PLATFORM%" -d build\obj build\gen\com\norton\parental\companion\R.java src\com\norton\parental\companion\*.java

echo [3/6] Running D8...
call "%BUILD_TOOLS%\d8.bat" --lib "%PLATFORM%" --output build\dex build\obj\com\norton\parental\companion\*.class

echo [4/6] Adding classes.dex to APK...
jar uf build\linked.apk -C build\dex classes.dex

echo [5/6] Aligning APK...
del /f /q build\bin\norton-aligned.apk 2>nul
"%BUILD_TOOLS%\zipalign.exe" -f -v 4 build\linked.apk build\bin\norton-aligned.apk

echo [6/6] Signing APK...
call "%BUILD_TOOLS%\apksigner.bat" sign --ks "%KEYSTORE%" --ks-pass pass:android --ks-key-alias androiddebugkey --out "%PUBLIC_DIR%\norton-companion.apk" build\bin\norton-aligned.apk

call "%BUILD_TOOLS%\apksigner.bat" verify "%PUBLIC_DIR%\norton-companion.apk"
echo BUILD SUCCESSFUL
