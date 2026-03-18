param(
    [string]$NacosServer = "127.0.0.1:8848",
    [string]$LogFile = ""
)

. "$PSScriptRoot\use-java21.ps1"

$env:NACOS_DISCOVERY_ENABLED = "true"
$env:NACOS_SERVER_ADDR = $NacosServer
$env:SPRING_PROFILES_ACTIVE = "discovery"

Set-Location E:\ai-workspaces\backend-gateway-service
if ([string]::IsNullOrWhiteSpace($LogFile)) {
    mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
} else {
    New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($LogFile)) | Out-Null
    mvn spring-boot:run "-Dmaven.repo.local=E:\repository" *>&1 | Tee-Object -FilePath $LogFile
}
