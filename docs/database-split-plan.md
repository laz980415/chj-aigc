# 数据库拆分计划

## 目标库

当前建议在同一台 PostgreSQL 实例 `36.150.108.207:54312` 上创建两个业务库：

- `chj-aigc-platform`
- `chj-aigc-tenant`

建库 SQL 已放在 [create-microservice-databases.sql](/e:/ai-workspaces/infra/sql/create-microservice-databases.sql)。

## 当前服务归属

### 平台服务 `backend-java`

平台服务当前只负责平台超管视角，因此它的初始化脚本 [schema.sql](/e:/ai-workspaces/backend-java/src/main/resources/schema.sql) 只保留这些表：

- `model_access_rules`
- `model_access_audit_events`
- `auth_users`
- `auth_sessions`
- `tenant_wallet_ledger`
- `tenant_payment_orders`

其中：
- `model_access_*` 是平台独占表
- `tenant_wallet_ledger`、`tenant_payment_orders` 当前由平台侧负责租户充值和订单审计
- `auth_*` 目前仍被平台侧用于超管查看账号和租户摘要

### 租户服务 `backend-tenant-service`

租户服务当前负责租户工作台，初始化脚本在 [schema.sql](/e:/ai-workspaces/backend-tenant-service/src/main/resources/schema.sql)，主要覆盖：

- `auth_users`
- `auth_sessions`
- `tenant_projects`
- `tenant_quota_allocations`
- `tenant_wallet_ledger`
- `tenant_payment_orders`
- `tenant_clients`
- `tenant_brands`
- `tenant_assets`

## 当前拆分状态

代码层边界已经完成两件关键事：

- 平台服务不再直接暴露 `/api/auth/**` 和 `/api/tenant/**`
- 前端统一通过网关进入平台服务和租户服务

但数据库层还存在一个未完全拆开的点：

- `auth_users`
- `auth_sessions`

这两张表当前仍然同时被平台侧和租户侧依赖。  
所以“真正切到两个物理数据库”之前，还需要继续处理账号域归属。

## 下一阶段建议

推荐继续按下面顺序推进：

1. 抽离独立身份认证服务，或者明确 `auth_*` 只归租户服务
2. 让平台服务通过远程调用获取租户成员摘要，而不是直接查 `auth_users`
3. 再把 `PLATFORM_DB_URL` 和 `TENANT_DB_URL` 切到两个独立库
4. 最后执行一次历史数据迁移

## 配置项

平台服务已经支持单独配置数据库连接：

- `PLATFORM_DB_URL`
- `PLATFORM_DB_USERNAME`
- `PLATFORM_DB_PASSWORD`

租户服务已经支持单独配置数据库连接：

- `TENANT_DB_URL`
- `TENANT_DB_USERNAME`
- `TENANT_DB_PASSWORD`

如果你要提前试跑分库，建议连接串如下：

```text
jdbc:postgresql://36.150.108.207:54312/chj-aigc-platform
jdbc:postgresql://36.150.108.207:54312/chj-aigc-tenant
```
