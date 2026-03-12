param(
    [string]$Version = "2.4.1",
    [string]$TargetDir = "E:\ai-workspaces\infra\nacos\local"
)

$ErrorActionPreference = "Stop"
$nacosHome = Join-Path $TargetDir "nacos-server-$Version\nacos\bin"
$shutdown = Join-Path $nacosHome "shutdown.cmd"

if (-not (Test-Path $shutdown)) {
    Write-Error "未找到 Nacos 停止文件，请确认已经下载并解压 Nacos"
}

Start-Process -FilePath $shutdown -WorkingDirectory $nacosHome -Wait
Write-Host "Nacos 已执行停止命令"
