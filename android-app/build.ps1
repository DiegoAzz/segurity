$ErrorActionPreference = "Stop"

$SDK = "C:\Users\Diego\AppData\Local\Android\Sdk"
$BUILD_TOOLS = "$SDK\build-tools\34.0.0"
$PLATFORM = "$SDK\platforms\android-35\android.jar"
$APP_DIR = "c:\REA\norton-parental-clone\android-app"
$PUBLIC_DIR = "c:\REA\norton-parental-clone\public"
$KEYSTORE = "$APP_DIR\build\debug.keystore"

Set-Location $APP_DIR

Write-Host "[1/6] Compiling resources with aapt2..."
if (Test-Path "build\obj") { Remove-Item "build\obj\*" -Recurse -Force -ErrorAction SilentlyContinue }
if (Test-Path "build\dex") { Remove-Item "build\dex\*" -Recurse -Force -ErrorAction SilentlyContinue }
New-Item -ItemType Directory -Path "build\obj" -Force | Out-Null
New-Item -ItemType Directory -Path "build\dex" -Force | Out-Null
New-Item -ItemType Directory -Path "build\bin" -Force | Out-Null

& "$BUILD_TOOLS\aapt2.exe" compile --dir res -o build\res.zip
& "$BUILD_TOOLS\aapt2.exe" link -I "$PLATFORM" --manifest AndroidManifest.xml --java build\gen -o build\linked.apk build\res.zip --auto-add-overlay

Write-Host "[2/6] Compiling Java classes with javac..."
$javaFiles = (Get-ChildItem -Path "src\com\norton\parental\companion\*.java").FullName
$rJava = "build\gen\com\norton\parental\companion\R.java"
javac -source 8 -target 8 -cp "$PLATFORM" -d build\obj $rJava $javaFiles

Write-Host "[3/6] Running D8 to create classes.dex..."
$classFiles = (Get-ChildItem -Path "build\obj" -Filter "*.class" -Recurse).FullName
& "$BUILD_TOOLS\d8.bat" --lib "$PLATFORM" --output build\dex $classFiles

Write-Host "[4/6] Adding classes.dex to APK..."
Set-Location "$APP_DIR\build\dex"
jar uf "$APP_DIR\build\linked.apk" classes.dex
Set-Location $APP_DIR

Write-Host "[5/6] Aligning APK..."
if (Test-Path "build\bin\norton-aligned.apk") { Remove-Item "build\bin\norton-aligned.apk" -Force }
& "$BUILD_TOOLS\zipalign.exe" -f -v 4 build\linked.apk build\bin\norton-aligned.apk

Write-Host "[6/6] Signing APK with apksigner..."
& "$BUILD_TOOLS\apksigner.bat" sign --ks "$KEYSTORE" --ks-pass pass:android --ks-key-alias androiddebugkey --out "$PUBLIC_DIR\norton-companion.apk" build\bin\norton-aligned.apk

Copy-Item "$PUBLIC_DIR\norton-companion.apk" "$PUBLIC_DIR\familyguard-companion.apk" -Force

& "$BUILD_TOOLS\apksigner.bat" verify "$PUBLIC_DIR\norton-companion.apk"
Write-Host ">>> COMPANION APK BUILD & SIGN SUCCESSFUL <<<" -ForegroundColor Green
