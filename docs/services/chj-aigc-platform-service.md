# chj-aigc-platform-service 服务说明

## 1. 服务定位

`chj-aigc-platform-service` 是平台超管侧服务。  
它负责平台级配置和平台级管理，不直接承接租户工作台业务。

## 2. 当前职责

当前已经落地的职责包括：

- 超管账号视角的后台接口
- 平台模型访问策略管理
- 平台侧租户总览和租户详情查看
- 平台统一返回格式
- 平台侧请求链路日志与 `X-Trace-Id` 透传

## 3. 不负责的内容

以下能力已经不再由这个服务直接承接：

- `/api/auth/**`
- `/api/tenant/**`
- 租户项目管理
- 租户成员管理
- 租户素材库管理

这些能力现在归租户服务，后续账号认证再逐步迁到认证服务。

## 4. 服务名与端口

- 服务名：`chj-aigc-platform-service`
- 本地端口：`8080`
- 配置文件：[application.yml](/e:/ai-workspaces/chj-aigc-platform-service/src/main/resources/application.yml)

## 5. 当前主要接口

当前核心接口包括：

- `GET /api/health`
- `GET /api/db-info`
- `GET /api/admin/summary`
- `GET /api/admin/users`
- `POST /api/admin/users`
- `GET /api/admin/model-access-rules`
- `POST /api/admin/model-access-rules`
- `GET /api/admin/tenants`
- `GET /api/admin/tenants/{tenantId}`

## 6. 数据存储

当前平台服务使用 PostgreSQL。

默认配置：

- `PLATFORM_DB_URL`
- `PLATFORM_DB_USERNAME`
- `PLATFORM_DB_PASSWORD`

当前平台侧主要涉及这些表：

- `model_access_rules`
- `model_access_audit_events`
- `auth_users`
- `auth_sessions`
- `tenant_wallet_ledger`
- `tenant_payment_orders`

注意：

- `auth_*` 目前还是平台和租户共享依赖
- 这个问题后续通过独立认证服务继续拆分

## 7. 技术结构

当前后端结构按你要求保持：

- `controller`
- `service`
- `mapper`
- `xml`

并已接入：

- Spring Boot
- MyBatis-Plus
- Lombok
- Spring Cloud Alibaba Nacos Discovery

## 8. 启动方式

本地单独启动：

```powershell
Set-Location E:\ai-workspaces\chj-aigc-platform-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

推荐脚本：

```powershell
Set-Location E:\ai-workspaces\infra\dev
.\start-platform-service.ps1
```

## 9. 日志与链路追踪

平台服务已经接入链路追踪基础规范：

- 请求头：`X-Trace-Id`
- 响应头：`X-Trace-Id`
- 日志会打印请求开始、结束、耗时、用户和角色

关键实现：

- [TraceContextFilter.java](/e:/ai-workspaces/chj-aigc-platform-service/src/main/java/com/chj/aigc/web/TraceContextFilter.java)

## 10. 当前已知限制

- 平台服务还没有彻底摆脱对 `auth_*` 表的直接依赖
- 租户摘要中的部分身份数据后续应通过认证服务或租户服务远程获取
- 物理分库切换到 `chj-aigc-platform` 前，还需要进一步收敛账号域归属
