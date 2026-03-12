# chj-aigc-gateway-service 服务说明

## 1. 服务定位

`chj-aigc-gateway-service` 是统一入口网关服务。  
前端应优先通过它访问平台服务和租户服务。

## 2. 当前职责

当前已经落地的职责包括：

- 前端统一入口
- 路由平台 API
- 路由租户 API
- 路由认证 API
- 网关层链路追踪

## 3. 服务名与端口

- 服务名：`chj-aigc-gateway-service`
- 本地端口：`8081`
- 配置文件：[application.yml](/e:/ai-workspaces/backend-gateway-service/src/main/resources/application.yml)

## 4. 当前路由规则

当前主要路由如下：

- `/api/auth/**` -> 租户服务
- `/api/tenant/**` -> 租户服务
- `/api/**` -> 平台服务
- `/tenant-api/**` -> 租户服务

后续认证服务成熟后，`/api/auth/**` 会切到认证服务。

## 5. 服务发现

当前网关支持两种方式：

1. 固定地址方式  
   适合本地快速联调。
2. Nacos 注册发现方式  
   适合微服务联调。

当启用 `discovery` profile 时，网关会走 `lb://` 路由。

## 6. 启动方式

```powershell
Set-Location E:\ai-workspaces\backend-gateway-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

推荐脚本：

```powershell
Set-Location E:\ai-workspaces\infra\dev
.\start-gateway-service.ps1
```

## 7. 日志与链路追踪

网关已接入链路追踪基础能力：

- 如果请求没有 `X-Trace-Id`，网关会自动生成
- 网关会把 `X-Trace-Id` 透传给下游服务
- 网关自身控制器请求也会补链路头

关键实现：

- [GatewayTraceFilter.java](/e:/ai-workspaces/backend-gateway-service/src/main/java/com/chj/aigc/gateway/logging/GatewayTraceFilter.java)
- [GatewayTraceWebFilter.java](/e:/ai-workspaces/backend-gateway-service/src/main/java/com/chj/aigc/gateway/logging/GatewayTraceWebFilter.java)

## 8. 当前已知限制

- `/api/auth/**` 目前还没有切到独立认证服务
- 生产环境还没有补限流、鉴权前置校验、熔断和灰度策略
- 后续其他服务接入时，需要保持同样的 `X-Trace-Id` 规范
