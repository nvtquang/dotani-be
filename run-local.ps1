# Chay backend truc tiep (khong Docker): nap .env roi chay Spring Boot.
# Dung: powershell -ExecutionPolicy Bypass -File .\run-local.ps1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

Get-Content (Join-Path $root '.env') | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$' -and $Matches[2].Trim() -ne '') {
        Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2].Trim()
    }
}

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'D:\Tools\jdk-21' }
$env:Path = "$env:JAVA_HOME\bin;D:\Tools\maven\bin;$env:Path"

& mvn -f (Join-Path $root 'pom.xml') -pl dotani-monolith -am -DskipTests install
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& mvn -f (Join-Path $root 'dotani-monolith\pom.xml') spring-boot:run
