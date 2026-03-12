param(
    [string]$DbUrl = "jdbc:postgresql://36.150.108.207:54312/chj-aigc",
    [string]$DbUsername = "postgres",
    [string]$DbPassword = "Linten@2023!",
    [string]$NacosServer = "127.0.0.1:8848",
    [string]$AuthServiceUri = "http://127.0.0.1:8083",
    [string]$LogFile = ""
)

$env:TENANT_DB_URL = $DbUrl
$env:TENANT_DB_USERNAME = $DbUsername
$env:TENANT_DB_PASSWORD = $DbPassword
$env:APP_DB_URL = $DbUrl
$env:APP_DB_USERNAME = $DbUsername
$env:APP_DB_PASSWORD = $DbPassword
$env:NACOS_DISCOVERY_ENABLED = "true"
$env:NACOS_SERVER_ADDR = $NacosServer
$env:AUTH_SESSION_VALIDATION_MODE = "remote"
$env:AUTH_SERVICE_URI = $AuthServiceUri

Set-Location E:\ai-workspaces\backend-tenant-service
if ([string]::IsNullOrWhiteSpace($LogFile)) {
    mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
} else {
    New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($LogFile)) | Out-Null
    mvn spring-boot:run "-Dmaven.repo.local=E:\repository" *>&1 | Tee-Object -FilePath $LogFile
}
