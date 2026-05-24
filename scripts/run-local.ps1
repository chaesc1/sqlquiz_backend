$ErrorActionPreference = 'Stop'

# Pin JAVA_HOME to a JDK 21 install (Gradle uses JAVA_HOME over PATH).
# Searches common Adoptium / Microsoft / Zulu locations and picks the highest 21.x.
function Resolve-Jdk21 {
    $roots = @(
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Zulu',
        'C:\Program Files\BellSoft',
        'C:\Program Files\Java'
    )
    foreach ($r in $roots) {
        if (-not (Test-Path $r)) { continue }
        $cand = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match 'jdk[-_]?21' } |
                Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
                Sort-Object Name -Descending |
                Select-Object -First 1
        if ($cand) { return $cand.FullName }
    }
    return $null
}

$jdk21 = Resolve-Jdk21
if (-not $jdk21) {
    Write-Error "JDK 21 not found. Install Temurin 21 (https://adoptium.net) or set JAVA_HOME manually."
}
$env:JAVA_HOME = $jdk21
$env:PATH = "$jdk21\bin;$env:PATH"
Write-Host "Using JAVA_HOME = $jdk21" -ForegroundColor Cyan

$envFile = Join-Path $PSScriptRoot '..\.env'
if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile. Copy .env.example and fill in values."
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name  = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

Push-Location (Join-Path $PSScriptRoot '..')
try {
    & .\gradlew.bat bootRun --args='--spring.profiles.active=local'
} finally {
    Pop-Location
}
