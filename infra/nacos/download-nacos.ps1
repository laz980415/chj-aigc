param(
    [string]$Version = "2.4.1",
    [string]$TargetDir = "E:\ai-workspaces\infra\nacos\local"
)

$ErrorActionPreference = "Stop"

$downloadUrl = "https://github.com/alibaba/nacos/releases/download/$Version/nacos-server-$Version.zip"
$zipPath = Join-Path $TargetDir "nacos-server-$Version.zip"
$extractDir = Join-Path $TargetDir "nacos-server-$Version"

New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

if (-not (Test-Path $zipPath)) {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath
}

if (-not (Test-Path $extractDir)) {
    Expand-Archive -Path $zipPath -DestinationPath $extractDir
}

Write-Host "Nacos 已下载并解压到: $extractDir"
