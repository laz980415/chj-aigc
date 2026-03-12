# chj-aigc-tenant-service 服务说明

## 1. 服务定位

`chj-aigc-tenant-service` 是租户工作台服务。  
它负责租户管理员和租户成员视角下的业务能力。

## 2. 当前职责

当前已经落地的职责包括：

- 租户项目管理
- 租户成员管理
- 项目/成员额度分配
- 租户钱包与充值订单
- 模拟微信支付充值
- 客户、品牌、素材库管理

## 3. 不负责的内容

以下内容不应放在租户服务里：

- 平台超管模型策略管理
- 平台侧全局租户总览
- 网关统一路由

## 4. 服务名与端口

- 服务名：`chj-aigc-tenant-service`
- 本地端口：`8082`
- 配置文件：[application.yml](/e:/ai-workspaces/backend-tenant-service/src/main/resources/application.yml)

## 5. 当前主要接口

当前主要接口包括：

- `GET /api/tenant/projects`
- `POST /api/tenant/projects`
- `GET /api/tenant/members`
- `POST /api/tenant/members`
- `POST /api/tenant/quotas`
- `GET /api/tenant/quota-allocations`
- `GET /api/tenant/wallet`
- `GET /api/tenant/wallet/payment-orders`
- `POST /api/tenant/wallet/payment-orders/wechat`
- `POST /api/tenant/wallet/payment-orders/{orderId}/mock-paid`
- `GET /api/tenant/clients`
- `POST /api/tenant/clients`
- `GET /api/tenant/brands`
- `POST /api/tenant/brands`
- `GET /api/tenant/assets`

## 6. 数据存储

当前租户服务使用 PostgreSQL。

默认配置：

- `TENANT_DB_URL`
- `TENANT_DB_USERNAME`
- `TENANT_DB_PASSWORD`

当前主要涉及这些表：

- `auth_users`
- `auth_sessions`
- `tenant_projects`
- `tenant_quota_allocations`
- `tenant_wallet_ledger`
- `tenant_payment_orders`
- `tenant_clients`
- `tenant_brands`
- `tenant_assets`

## 7. 技术结构

当前结构按你的要求实现：

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

```powershell
Set-Location E:\ai-workspaces\backend-tenant-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

推荐脚本：

```powershell
Set-Location E:\ai-workspaces\infra\dev
.\start-tenant-service.ps1
```

## 9. 日志与链路追踪

租户服务已经接入统一链路追踪规范：

- 请求头：`X-Trace-Id`
- 响应头：`X-Trace-Id`
- 日志会输出请求开始、请求结束、耗时、用户、角色

关键实现：

- [TraceContextFilter.java](/e:/ai-workspaces/backend-tenant-service/src/main/java/com/chj/aigc/tenantservice/web/TraceContextFilter.java)

## 10. 当前已知限制

- 登录接口已经迁到认证服务，租户服务已支持通过认证服务远程校验令牌
- 当前默认仍保留本地校验模式，便于测试和离线开发
- 微信充值目前还是 mock 流程
- 物理分库前仍然和平台服务共享 `auth_*` 表依赖
