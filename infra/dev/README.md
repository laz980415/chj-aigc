# 本地微服务启动脚本

这组脚本用于在 Windows 本地通过 `Nacos` 启动整套 Java 微服务。

## 前置条件

1. 先启动本地 Nacos
2. 本机已安装 JDK 21
3. Maven 本地仓库在 `E:\repository`

## 一键启动

```powershell
Set-Location E:\ai-workspaces\infra\dev
.\start-microservices-with-nacos.ps1
```

这会分别打开三个 PowerShell 窗口并启动：

- 平台服务 `backend-java`
- 租户服务 `backend-tenant-service`
- 网关服务 `backend-gateway-service`

## 单独启动

```powershell
.\start-platform-service.ps1
.\start-tenant-service.ps1
.\start-gateway-service.ps1
```

## 网关说明

网关默认还是本地直连路由。
只有通过 `start-gateway-service.ps1` 或显式设置：

```powershell
$env:SPRING_PROFILES_ACTIVE="discovery"
```

时，网关才会切到 `lb://` 服务发现路由。
