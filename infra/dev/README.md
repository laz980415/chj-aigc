# 本地微服务启动说明

这个目录用于本地联调 `Spring Cloud Alibaba + Nacos` 微服务。

## 前置条件

- PostgreSQL 已可访问
- Nacos 已启动
- Maven 本地仓库在 `E:\repository`

## 推荐启动方式

```powershell
Set-Location E:\ai-workspaces\infra\dev
.\start-microservices-with-nacos.ps1
```

默认会拉起：

- 平台服务
- 认证服务
- 租户服务
- 网关服务

并把控制台输出同时写入：

- `logs/platform-service.log`
- `logs/auth-service.log`
- `logs/tenant-service.log`
- `logs/gateway-service.log`

## 单独启动

```powershell
.\start-platform-service.ps1 -LogFile E:\ai-workspaces\infra\dev\logs\platform-service.log
.\start-auth-service.ps1 -LogFile E:\ai-workspaces\infra\dev\logs\auth-service.log
.\start-tenant-service.ps1 -LogFile E:\ai-workspaces\infra\dev\logs\tenant-service.log
.\start-gateway-service.ps1 -LogFile E:\ai-workspaces\infra\dev\logs\gateway-service.log
```

## 关注点

- 每个请求都应该带 `X-Trace-Id`
- 网关、平台服务、认证服务、租户服务日志都应该能按同一个 `traceId` 串起来
- 如果注册发现异常，先看 `gateway-service.log` 和对应服务日志里的 Nacos 连接信息
