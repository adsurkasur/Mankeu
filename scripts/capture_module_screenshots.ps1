# Regenerates the user guide screenshots (docs/panduan/gambar/*.jpg) on a running emulator, by
# walking through the app as an owner (ModuleScreenshotTest). Run from the repo root:
#   powershell -ExecutionPolicy Bypass -File scripts\capture_module_screenshots.ps1
# In-memory database and restored display preferences: the device's own Mankeu data is not touched.
$ErrorActionPreference = "Stop"
$adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME "platform-tools\adb.exe" } else { "adb" }

.\gradlew.bat assembleDebug assembleDebugAndroidTest --console=plain -q
& $adb install -r app\build\outputs\apk\debug\app-debug.apk | Out-Null
& $adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk | Out-Null
& $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
# A walk through ~40 real screens on an emulator is occasionally flaky (a frame not yet settled):
# one retry, then fail loudly.
foreach ($attempt in 1..2) {
    $result = & $adb shell am instrument -w -e class com.mankeu.presentation.ModuleScreenshotTest -e moduleScreenshots true com.mankeu.test/androidx.test.runner.AndroidJUnitRunner
    if ($result -match "OK \(1 test\)") { break }
    "Attempt $attempt failed:"; $result | Select-String "Error in|at com.mankeu.presentation.Module" | Select-Object -First 3
}
$result | Select-Object -Last 3
if (-not ($result -match "OK \(1 test\)")) { throw "Module screenshot walk failed" }

$target = "docs\panduan\gambar"
New-Item -ItemType Directory -Force $target | Out-Null
$files = & $adb shell run-as com.mankeu ls files/module
foreach ($name in $files) {
    $name = $name.Trim()
    if (-not $name.EndsWith(".jpg")) { continue }
    $out = Join-Path $target $name
    cmd /c "`"$adb`" exec-out run-as com.mankeu cat files/module/$name > `"$out`""
    "{0,-36} {1,8:N0} bytes" -f $name, (Get-Item $out).Length
}
