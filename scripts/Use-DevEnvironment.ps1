param(
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
)

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$FrontendRoot = Join-Path $ProjectRoot "frontend"

function Add-PathEntry {
    param([Parameter(Mandatory = $true)][string]$PathEntry)

    if (-not (Test-Path $PathEntry)) {
        return
    }

    $entries = $env:Path -split ';' | Where-Object { $_ }
    if ($entries -notcontains $PathEntry) {
        $env:Path = "$PathEntry;$env:Path"
    }
}

function Test-DevCommand {
    param([Parameter(Mandatory = $true)][string]$Name)

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    return $null -ne $command
}

if (-not (Test-Path $JavaHome)) {
    throw "JDK 21 path not found: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
Add-PathEntry (Join-Path $env:JAVA_HOME "bin")
Add-PathEntry "C:\Program Files\nodejs"

Set-Alias npm npm.cmd -Scope Global
Set-Alias npx npx.cmd -Scope Global

function Invoke-BackendDev {
    Set-Location $ProjectRoot
    & "$ProjectRoot\mvnw.cmd" spring-boot:run
}

function Invoke-FrontendDev {
    Set-Location $FrontendRoot
    & npm.cmd run dev
}

function Show-DevEnvironment {
    Write-Host "Project: $ProjectRoot"
    Write-Host "Frontend: $FrontendRoot"
    Write-Host "JAVA_HOME: $env:JAVA_HOME"

    Write-Host ""
    Write-Host "Java:"
    & java -version

    Write-Host ""
    Write-Host "Maven:"
    & "$ProjectRoot\mvnw.cmd" -version

    Write-Host ""
    Write-Host "Node:"
    & node -v

    Write-Host ""
    Write-Host "npm:"
    & npm.cmd -v

    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  Invoke-BackendDev   # run Spring Boot backend on port 8080"
    Write-Host "  Invoke-FrontendDev  # run Vite frontend on port 5173"
}

Show-DevEnvironment
