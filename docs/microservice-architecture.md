# 微服务改造说明

## 当前拆分方向

后端从原来的单体 Spring Boot，开始切换为 `Spring Cloud Alibaba` 微服务体系。

当前仓库里的服务职责先这样划分：

- `chj-aigc-platform-service`
  - 平台管理服务
  - 负责超管后台、认证、模型访问策略、租户总览、充值订单管理
- `backend-tenant-service`
  - 租户工作台服务
  - 负责项目、成员、素材、客户、品牌、额度等租户内部能力
- `backend-gateway-service`
  - 网关服务
  - 负责统一接入平台服务和租户服务，并为前端提供单入口
- `platform_core`
  - Python 模型编排层
  - 负责模型路由、品牌约束、生成任务和结算编排

## 服务注册与发现

采用 `Spring Cloud Alibaba + Nacos`。

当前服务默认配置：

- 平台管理服务：`chj-aigc-platform-service`
- 租户工作台服务：`chj-aigc-tenant-service`
- 网关服务：`chj-aigc-gateway-service`

公共环境变量：

- `NACOS_DISCOVERY_ENABLED`
- `NACOS_SERVER_ADDR`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`

为了不阻塞本地开发，默认把 `NACOS_DISCOVERY_ENABLED` 设为 `false`。本地没有启动 Nacos 时，服务也可以独立跑起来。

仓库里已经补了本地开发版 Nacos 编排：

- `infra/nacos/docker-compose.yml`
- `infra/nacos/README.md`
- `infra/nacos/download-nacos.ps1`
- `infra/nacos/start-nacos.ps1`
- `infra/nacos/stop-nacos.ps1`

无 Docker 环境可以直接执行：

```powershell
Set-Location E:\ai-workspaces\infra\nacos
.\download-nacos.ps1
.\start-nacos.ps1
```

如果本机有 Docker，也可以继续使用 `docker compose up -d`。
两种方式都会在本地启动 `Nacos 2.4.1` 单机开发服务。

仓库里也补了基于 Nacos 的本地微服务启动脚本：

- `infra/dev/start-platform-service.ps1`
- `infra/dev/start-tenant-service.ps1`
- `infra/dev/start-gateway-service.ps1`
- `infra/dev/start-microservices-with-nacos.ps1`

其中网关通过 `application-discovery.yml` 切到 `lb://` 服务发现路由。

## 官方兼容版本

这轮按官方兼容组合收口：

- `Spring Boot 3.2.4`
- `Spring Cloud 2023.0.1`
- `Spring Cloud Alibaba 2023.0.1.0`
- `JDK 21`

这样能避免把后端继续建立在未确认兼容的依赖版本上。

## 数据库分库建议

你现在的 PostgreSQL 连接可以继续共用：

- 主机：`36.150.108.207`
- 端口：`54312`
- 用户：`postgres`

后续建议按“同连接、不同库”拆分：

1. `chj-aigc-platform`
   - 平台超管
   - 登录认证
   - 模型访问策略
   - 充值订单
   - 平台级审计
2. `chj-aigc-tenant`
   - 租户项目
   - 成员
   - 客户与品牌
   - 素材库
   - 租户额度
   - 租户钱包流水

如果你想再进一步拆，可以后续新增：

3. `chj-aigc-analytics`
   - 报表
   - 任务追踪
   - 审计检索

## 当前迁移状态

这一步不是一次性把全部业务搬完，而是先完成三件事：

1. 把现有平台服务切到 `Spring Cloud Alibaba` 兼容版本
2. 把平台服务接入 Nacos 注册发现配置
3. 新增独立的租户微服务骨架，后续逐步搬迁租户内部接口

## 下一阶段迁移顺序

建议按这个顺序继续：

1. 先把租户成员、项目、额度接口迁到 `backend-tenant-service`
2. 再把素材、客户、品牌迁过去
3. 然后补一个 API 网关服务统一前端入口
4. 最后把前端 `/api` 代理改成只指向网关

## 网关当前路由约定

当前网关已经开始按业务边界分流：

- `/api/auth/**` -> `backend-tenant-service`
- `/api/tenant/**` -> `backend-tenant-service`
- `/api/**` -> `chj-aigc-platform-service`

也就是说，前端开发环境已经统一走网关，并且租户登录、项目、成员、额度接口会优先进租户服务。
平台服务已经移除 `/api/auth/**` 和 `/api/tenant/**` 控制器，只保留超管平台能力。

## 分库准备

当前已经开始为 PostgreSQL 分库做准备：

- 平台服务支持 `PLATFORM_DB_URL / PLATFORM_DB_USERNAME / PLATFORM_DB_PASSWORD`
- 租户服务支持 `TENANT_DB_URL / TENANT_DB_USERNAME / TENANT_DB_PASSWORD`
- 建库 SQL 在 [create-microservice-databases.sql](/e:/ai-workspaces/infra/sql/create-microservice-databases.sql)
- 详细拆分说明在 [database-split-plan.md](/e:/ai-workspaces/docs/database-split-plan.md)

现阶段平台服务的 `schema.sql` 已经只保留平台侧仍然依赖的表，不再创建项目、额度、客户、品牌、素材这些租户工作台表。

## 日志链路规范

当前网关、平台服务、租户服务已经统一采用 `X-Trace-Id` 作为链路追踪头：

- 网关负责生成或透传 `X-Trace-Id`
- 网关会把 `X-Trace-Id` 继续传给下游服务
- 平台服务和租户服务会把 `X-Trace-Id` 写回响应头
- 平台服务和租户服务日志会输出 `traceId`

后续新增服务必须遵守同一套规则，包括但不限于：

- Python 模型服务
- 后续独立身份认证服务
- 任何新建的 Java 微服务

也就是说，后续所有服务都要满足两点：

1. 能接收并透传 `X-Trace-Id`
2. 核心请求日志必须带 `traceId`
为了兼容联调和逐步迁移，仍保留显式实验入口：

- `/tenant-api/**` -> `backend-tenant-service`

默认本地直连地址：

- 平台服务：`http://127.0.0.1:8080`
- 网关服务：`http://127.0.0.1:8081`
- 租户服务：`http://127.0.0.1:8082`

当前网关配置既支持本地直连 URI，也为后续改成 `lb://` 服务发现路由预留了位置。
现在仓库已经提供 `discovery` profile，启用后会直接走：

- `lb://chj-aigc-platform-service`
- `lb://chj-aigc-tenant-service`

## 当前迁移进度

这一轮之后：

- 前端已通过网关访问后端
- 平台 API 已挂在网关统一入口 `/api/**`
- 租户登录、项目、成员、额度接口已开始通过 `/api/auth/**` 和 `/api/tenant/**` 切到租户服务
- 租户客户、品牌、素材接口也已迁到租户服务
- 租户服务仍保留独立实验入口 `/tenant-api/**`
- 仓库内已提供本地 Nacos 开发编排，后续可以切到 `lb://` 服务发现路由
- 下一步继续迁移租户钱包流水、支付订单等剩余租户域接口
