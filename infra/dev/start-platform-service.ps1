param(
    [string]$DbUrl = "jdbc:postgresql://36.150.108.207:54312/chj-aigc",
    [string]$DbUsername = "postgres",
    [string]$DbPassword = "Linten@2023!",
    [string]$NacosServer = "127.0.0.1:8848"
)

$env:APP_DB_URL = $DbUrl
$env:APP_DB_USERNAME = $DbUsername
$env:APP_DB_PASSWORD = $DbPassword
$env:NACOS_DISCOVERY_ENABLED = "true"
$env:NACOS_SERVER_ADDR = $NacosServer

Set-Location E:\ai-workspaces\backend-java
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
