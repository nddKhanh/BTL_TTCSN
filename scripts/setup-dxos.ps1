$ErrorActionPreference = 'Stop'

$gitCommand = Get-Command git.exe -ErrorAction SilentlyContinue
$gitRoot = if ($gitCommand) {
    Split-Path -Parent (Split-Path -Parent $gitCommand.Source)
}
$bashCandidates = @(
    $(if ($gitRoot) { Join-Path $gitRoot 'bin\bash.exe' }),
    $(if ($gitRoot) { Join-Path $gitRoot 'usr\bin\bash.exe' }),
    (Join-Path $env:ProgramFiles 'Git\bin\bash.exe'),
    (Join-Path $env:LOCALAPPDATA 'Programs\Git\bin\bash.exe')
) | Where-Object { $_ }
$bash = $bashCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

if (-not $bash) {
    throw 'Git Bash is required. Install Git for Windows or run scripts/setup-dxos.sh from Bash.'
}

$root = Split-Path -Parent $PSScriptRoot
$script = Join-Path $PSScriptRoot 'setup-dxos.sh'
$env:MSYS_NO_PATHCONV = '1'

Push-Location $root
try {
    & $bash $script
    if ($LASTEXITCODE -ne 0) {
        throw "DX-OS setup failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
