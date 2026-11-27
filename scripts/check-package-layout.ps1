#Requires -Version 7.0

param(
    [string]$RepositoryRoot = (Join-Path $PSScriptRoot '..')
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$basePackage = 'com.ygoular.keepit'
$violations = [System.Collections.Generic.List[string]]::new()

foreach ($module in @('app/shared', 'app/androidApp')) {
    $sourceSets = Get-ChildItem -LiteralPath (Join-Path $root $module) -Directory |
        Where-Object Name -Match '^(src|test)(@[^/]+)?$'

    foreach ($sourceSet in $sourceSets) {
        foreach ($file in Get-ChildItem -LiteralPath $sourceSet.FullName -Filter '*.kt' -File -Recurse) {
            $relativeFile = [IO.Path]::GetRelativePath($root, $file.FullName)
            $packagePath = [IO.Path]::GetRelativePath($sourceSet.FullName, $file.DirectoryName).Replace('\', '/')
            $expectedPackage = $basePackage
            if ($packagePath -ne '.') {
                $expectedPackage += '.' + $packagePath.Replace('/', '.')
            }

            [string]$source = Get-Content -LiteralPath $file.FullName -Raw
            $declaration = [regex]::Match($source, '(?m)^\s*package\s+([\w.]+)')
            if ($declaration.Groups[1].Value -cne $expectedPackage) {
                $violations.Add("${relativeFile}: expected package $expectedPackage")
            }

            if ($packagePath -eq 'reminder') {
                $violations.Add("${relativeFile}: place reminder code in a responsibility-specific package")
            }

            if ($module -eq 'app/shared' -and $sourceSet.Name -eq 'src') {
                if ($packagePath -match '^reminder/domain(/|$)' -and
                    $source -match '(?m)^\s*import\s+(android[.]|androidx[.]|com[.]ygoular[.]keepit[.](design|reminder[.](presentation|storage|platform))[.])') {
                    $violations.Add("${relativeFile}: shared domain cannot import UI, storage, or platform APIs")
                }

                if ($packagePath -match '^design/theme(/|$)' -and
                    $source -match '(?m)^\s*import\s+com[.]ygoular[.]keepit[.]reminder[.]') {
                    $violations.Add("${relativeFile}: generic theme cannot import reminder features")
                }
            }
        }
    }
}

$manifestPath = 'app/androidApp/src/AndroidManifest.xml'
[xml]$manifest = Get-Content -LiteralPath (Join-Path $root $manifestPath) -Raw
$androidNamespace = 'http://schemas.android.com/apk/res/android'
$stableReceiver = "$basePackage.ReminderReceiver"
$receiverNames = $manifest.SelectNodes('/manifest/application/receiver') | ForEach-Object {
    $name = $_.GetAttribute('name', $androidNamespace)
    if ($name.StartsWith('.')) { "$basePackage$name" } else { $name }
}
# The receiver component is part of the identity of alarms already registered with Android.
if ($receiverNames -cnotcontains $stableReceiver) {
    $violations.Add(('{0}: keep {1} registered to preserve scheduled alarm identity across updates' -f $manifestPath, $stableReceiver))
}

if ($violations.Count -gt 0) {
    foreach ($violation in $violations) {
        [Console]::Error.WriteLine($violation)
    }
    exit 1
}

Write-Host 'KeepIt package layout verified.'
