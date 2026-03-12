param(
    [string]$NacosServer = "127.0.0.1:8848"
)

$env:NACOS_DISCOVERY_ENABLED = "true"
$env:NACOS_SERVER_ADDR = $NacosServer
$env:SPRING_PROFILES_ACTIVE = "discovery"

Set-Location E:\ai-workspaces\backend-gateway-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
