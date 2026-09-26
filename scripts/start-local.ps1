param(
    [ValidateSet('api', 'web')][string]$Service = 'api',
    [string]$EnvironmentFile
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $EnvironmentFile) {
    $localDirectory = Join-Path (Split-Path -Parent $projectRoot) ((Split-Path -Leaf $projectRoot) + '-local')
    $EnvironmentFile = Join-Path $localDirectory '.env'
}
if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw 'Local configuration is missing. Supply -EnvironmentFile with a private .env outside this repository.'
}
$allowed = @('PORT','DB_URL','DB_USERNAME','DB_PASSWORD','DB_INIT','STORAGE_ROOT','APP_ORIGIN',
    'SECURE_COOKIE','ADMIN_USERNAME','ADMIN_PASSWORD','AI_ENABLED','SILICONFLOW_API_KEY',
    'SILICONFLOW_BASE_URL','TEXT_MODEL','VISION_MODEL','OPENAI_API_KEY','OPENAI_BASE_URL',
    'IMAGE_MODEL','VITE_PROXY_TARGET')
foreach ($line in Get-Content -LiteralPath $EnvironmentFile -Encoding UTF8) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
    $pair = $line.Split(@('='), 2)
    if ($pair.Length -ne 2 -or $pair[0].Trim() -notin $allowed) {
        throw 'Invalid or unsupported setting in the local environment file.'
    }
    [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
}
if ($Service -eq 'api') {
    Push-Location (Join-Path $projectRoot 'backend')
    try { & mvn spring-boot:run; if ($LASTEXITCODE -ne 0) { throw 'API exited with an error.' } }
    finally { Pop-Location }
} else {
    Push-Location (Join-Path $projectRoot 'frontend')
    try {
        if (-not (Test-Path -LiteralPath 'node_modules' -PathType Container)) {
            & npm ci
            if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
        }
        & npm run dev
        if ($LASTEXITCODE -ne 0) { throw 'Frontend exited with an error.' }
    } finally { Pop-Location }
}
