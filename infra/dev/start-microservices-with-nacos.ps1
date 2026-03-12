param(
    [string]$DbUrl = "jdbc:postgresql://36.150.108.207:54312/chj-aigc",
    [string]$DbUsername = "postgres",
    [string]$DbPassword = "Linten@2023!",
    [string]$NacosServer = "127.0.0.1:8848",
    [string]$LogDir = "E:\ai-workspaces\infra\dev\logs"
)

$scriptRoot = "E:\ai-workspaces\infra\dev"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "$scriptRoot\start-platform-service.ps1", "-DbUrl", $DbUrl, "-DbUsername", $DbUsername, "-DbPassword", $DbPassword, "-NacosServer", $NacosServer, "-LogFile", "$LogDir\platform-service.log"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "$scriptRoot\start-tenant-service.ps1", "-DbUrl", $DbUrl, "-DbUsername", $DbUsername, "-DbPassword", $DbPassword, "-NacosServer", $NacosServer, "-LogFile", "$LogDir\tenant-service.log"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "$scriptRoot\start-gateway-service.ps1", "-NacosServer", $NacosServer, "-LogFile", "$LogDir\gateway-service.log"

Write-Host "已拉起平台服务、租户服务、网关服务。日志目录：$LogDir。请确认 Nacos 已启动。"
