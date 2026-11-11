param (
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9]+\.[0-9]+\.[0-9]+$')]
    [string]$Version
)

# Verification only: this helper must never stage, commit, push, tag or publish.
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    python .github/scripts/release.py validate $Version
    if ($LASTEXITCODE -ne 0) { throw 'Release metadata validation failed' }
    python -m unittest discover -s .github/scripts -p 'test_*.py' -v
    if ($LASTEXITCODE -ne 0) { throw 'Release tooling tests failed' }
    mvn --batch-mode --no-transfer-progress clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed' }
    python .github/scripts/release.py verify-jar $Version
    if ($LASTEXITCODE -ne 0) { throw 'Packaged JAR validation failed' }
    Write-Host "Verified v$Version locally. No Git refs or releases were changed."
    Write-Host 'Follow docs/releasing.md after the release-preparation PR is merged.'
} finally {
    Pop-Location
}
