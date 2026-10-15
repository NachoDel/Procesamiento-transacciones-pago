# T5.1.2 - Validacion de portabilidad Windows PowerShell 5.1+.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location $root
try {
    foreach ($tool in @('java', 'javac', 'mvn', 'git')) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw "Falta $tool en PATH. Instalar JDK 17+ y Maven."
        }
    }

    $stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
    $report = Join-Path $root "results/portability/windows/$stamp"
    New-Item -ItemType Directory -Path $report -Force | Out-Null

    function Invoke-Checked {
        param([string]$Executable, [string[]]$Arguments, [string]$OutputFile)
        # Java y Maven pueden escribir mensajes normales en stderr.
        $previous = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            & $Executable @Arguments 2>&1 |
                ForEach-Object { $_.ToString() } |
                Tee-Object -FilePath $OutputFile
            $code = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $previous
        }
        if ($code -ne 0) {
            throw "$Executable fallo con exit code $code. Ver $OutputFile"
        }
    }

    Write-Host "Validacion de portabilidad Windows: $report"
    Invoke-Checked 'cmd.exe' @('/d', '/c', 'java -version 2>&1') (Join-Path $report 'java-version.txt')
    Invoke-Checked 'javac' @('-version') (Join-Path $report 'javac-version.txt')
    Invoke-Checked 'mvn' @('-version') (Join-Path $report 'maven-version.txt')
    $PSVersionTable.PSVersion.ToString() |
        Set-Content -Encoding UTF8 (Join-Path $report 'powershell-version.txt')
    Invoke-Checked 'git' @('rev-parse', 'HEAD') (Join-Path $report 'commit.txt')

    Write-Host '[1/4] Maven: compilacion limpia y tests'
    Invoke-Checked 'mvn' @('clean', 'test') (Join-Path $report 'maven-test.txt')
    Write-Host '[2/4] Maven: package'
    Invoke-Checked 'mvn' @('-DskipTests', 'package') (Join-Path $report 'maven-package.txt')
    Write-Host '[3/4] Main: --help'
    $help = Join-Path $report 'main-help.txt'
    Invoke-Checked 'java' @('-cp', 'target/classes', 'app.Main', '--help') $help
    if (-not ((Get-Content $help -Raw).Contains('--policy=RANDOM'))) {
        throw 'La ayuda de Main no incluye --policy=RANDOM'
    }

    Write-Host '[4/4] Main: RANDOM y PRIORITY con target=12'
    foreach ($policy in @('RANDOM', 'PRIORITY')) {
        $output = Join-Path $report "main-$policy.txt"
        $logfile = Join-Path $report "transactions-$policy.log"
        Invoke-Checked 'java' @(
            '-cp', 'target/classes', 'app.Main', "--policy=$policy",
            '--seed=2026', '--target=12', '--scale=0.01',
            '--timeout-seconds=20', "--log=$logfile"
        ) $output
        $lines = @(Get-Content $output)
        foreach ($required in @(
            'Admisiones T0: 12', 'Finalizaciones T9: 12',
            'Total T-invariantes: 12', 'P-invariantes validos: true',
            'T-invariantes validos: true', 'Workers terminados: true',
            'Corrida exitosa: true'
        )) {
            if ($lines -cnotcontains $required) {
                throw "Falta '$required' para $policy. Ver $output"
            }
        }
        if (-not (Test-Path $logfile) -or (Get-Item $logfile).Length -eq 0) {
            throw "Log vacio o inexistente para ${policy}: $logfile"
        }
    }
    'SUCCESS' | Set-Content -Encoding UTF8 (Join-Path $report 'status.txt')
    Write-Host "VALIDACION EXITOSA: $report"
}
finally {
    Pop-Location
}
