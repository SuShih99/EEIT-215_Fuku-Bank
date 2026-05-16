$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Start-Process powershell.exe -ArgumentList @(
    "-NoExit",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $ProjectRoot "scripts\Start-Backend.ps1")
)

Start-Process powershell.exe -ArgumentList @(
    "-NoExit",
    "-ExecutionPolicy", "Bypass",
    "-File", (Join-Path $ProjectRoot "scripts\Start-Frontend.ps1")
)
