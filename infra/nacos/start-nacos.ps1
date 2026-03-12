param(
    [string]$Version = "2.4.1",
    [string]$TargetDir = "E:\ai-workspaces\infra\nacos\local"
)

$ErrorActionPreference = "Stop"
$nacosHome = Join-Path $TargetDir "nacos-server-$Version\nacos\bin"
$startup = Join-Path $nacosHome "startup.cmd"

if (-not (Test-Path $startup)) {
    Write-Error "未找到 Nacos 启动文件，请先运行 download-nacos.ps1"
}

Start-Process -FilePath $startup -ArgumentList "-m standalone" -WorkingDirectory $nacosHome
Write-Host "Nacos 已尝试启动，请稍后访问 http://127.0.0.1:8848/nacos"
