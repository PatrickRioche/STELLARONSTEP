$source = "..\STELLARPILOT\android\gradle\wrapper\gradle-wrapper.jar"
$target = ".\gradle\wrapper\gradle-wrapper.jar"
if (Test-Path $source) {
  Copy-Item $source $target -Force
  Write-Host "gradle-wrapper.jar copie depuis StellarPilot."
} else {
  Write-Host "Source introuvable: $source"
  Write-Host "Copiez gradle-wrapper.jar de STELLARPILOT/android/gradle/wrapper/ vers gradle/wrapper/."
  exit 1
}
