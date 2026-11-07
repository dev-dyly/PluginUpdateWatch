param (
    [Parameter(Mandatory=$true)]
    [string]$Version
)

Write-Host "Updating PluginUpdateWatch to v$Version..." -ForegroundColor Cyan

[xml]$pom = Get-Content "pom.xml"
$pom.project.version = $Version
$pom.Save((Resolve-Path "pom.xml").Path)

$pluginYml = "src\main\resources\plugin.yml"
if (Test-Path $pluginYml) {
    $content = Get-Content $pluginYml -Raw
    if ($content -notmatch '\$\{project\.version\}') {
        $content = $content -replace '(?m)^version:\s*.*$', "version: '$Version'"
        Set-Content -Path $pluginYml -Value $content -NoNewline
    }
}

git add .
git commit -m "Add automated release workflow and script"
git push origin main

Write-Host "Automation files created and pushed to GitHub!" -ForegroundColor Green
