$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pomPath = Join-Path $projectRoot 'pom.xml'
$applicationPath = Join-Path $projectRoot 'src/main/java/com/fashion/FashionApplication.java'
$controllerPath = Join-Path $projectRoot 'src/main/java/com/fashion/event/controller/AdminCampaignEventController.java'
$envFilePath = Join-Path $projectRoot '.env'

if (Test-Path -LiteralPath $envFilePath) {
    foreach ($line in Get-Content -LiteralPath $envFilePath) {
        $entry = $line.Trim().TrimStart([char]0xFEFF)
        if ($entry -match '^\s*#' -or $entry -notmatch '^\s*(MAIL_[A-Z_]+)\s*=\s*(.*?)\s*$') {
            continue
        }

        $name = $Matches[1]
        $value = $Matches[2].Trim().Trim('"').Trim("'")
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

if (-not (Test-Path -LiteralPath $pomPath) -or -not (Test-Path -LiteralPath $applicationPath)) {
    throw "Fashion backend project not found: $projectRoot"
}

if (-not (Test-Path -LiteralPath $controllerPath)) {
    throw "Campaign Event API controller is missing: $controllerPath. Update this checkout first."
}

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($maven) {
    Push-Location $projectRoot
    try {
        & $maven.Source spring-boot:run
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    finally {
        Pop-Location
    }
    exit 0
}

$wrapper = Join-Path $projectRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $wrapper)) {
    throw 'Maven and mvnw.cmd were not found. Install Maven or restore the project wrapper.'
}

Push-Location $projectRoot
try {
    & $wrapper spring-boot:run
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
finally {
    Pop-Location
}
