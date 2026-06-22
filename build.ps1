$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$srcRoot = Join-Path $projectRoot "src\main\java"
$resources = Join-Path $projectRoot "src\main\resources"
$classes = Join-Path $projectRoot "build\classes"
$staging = Join-Path $projectRoot "build\staging"
$outJar = Join-Path $projectRoot "AthenaSorter-1.0.0.jar"

$serverJarCandidates = @(
    "c:\Users\trist\AppData\Roaming\Hytale\install\release\package\game\latest\Server\HytaleServer.jar",
    (Join-Path $projectRoot "HytaleServer.jar"),
    (Join-Path (Split-Path $projectRoot -Parent) "AutoSorter\HytaleServer.jar")
)

$serverJar = $null
foreach ($candidate in $serverJarCandidates) {
    if (Test-Path $candidate) {
        $serverJar = $candidate
        break
    }
}

if (-not $serverJar) {
    throw "Missing HytaleServer.jar. Expected at: $($serverJarCandidates -join ', ')"
}

Write-Host "Using HytaleServer.jar: $serverJar"

Remove-Item -Recurse -Force $classes, $staging -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes, $staging | Out-Null

$simpleClaimsCandidates = @(
    (Join-Path (Split-Path $projectRoot -Parent) "claimfly\libs\SimpleClaims-1.0.37.jar"),
    (Join-Path $projectRoot "libs\SimpleClaims.jar")
)
$compileClasspath = $serverJar
foreach ($candidate in $simpleClaimsCandidates) {
    if (Test-Path $candidate) {
        $compileClasspath = "$serverJar;$candidate"
        Write-Host "Using SimpleClaims.jar: $candidate"
        break
    }
}

$javaFiles = Get-ChildItem -Path $srcRoot -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
javac --release 25 -cp $compileClasspath -d $classes $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Copy-Item -Recurse "$classes\*" $staging
Copy-Item (Join-Path $resources "manifest.json") $staging -Force
Copy-Item -Recurse (Join-Path $resources "Common") (Join-Path $staging "Common") -Force

jar cf $outJar -C $staging .
Write-Host "Built $outJar ($((Get-Item $outJar).Length) bytes)"
