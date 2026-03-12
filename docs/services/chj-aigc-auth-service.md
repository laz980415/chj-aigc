# chj-aigc-auth-service 服务说明

## 1. 服务定位

`chj-aigc-auth-service` 是独立认证服务骨架。  
它的目标是成为账号、登录、会话、身份校验的唯一所有者。

## 2. 当前职责

当前已经完成的能力不多，主要是骨架层：

- Spring Boot 服务启动
- Nacos 注册发现接入
- 健康检查接口
- 独立服务目录与独立 Maven 模块

## 3. 后续职责

这个服务后续要承接的核心职责包括：

- 登录接口
- 会话签发
- 会话校验
- 超管账号查询
- 租户成员身份查询
- `auth_users` 表写入和读取
- `auth_sessions` 表写入和读取

## 4. 服务名与端口

- 服务名：`chj-aigc-auth-service`
- 本地端口：`8083`
- 配置文件：[application.yml](/e:/ai-workspaces/chj-aigc-auth-service/src/main/resources/application.yml)

## 5. 当前主要接口

当前只有健康检查：

- `GET /api/health`

## 6. 数据存储

当前还没有接数据库。  
这一步是故意的，因为这轮先把认证服务骨架建出来，后续再把 `auth_*` 表和接口从平台/租户服务迁过来。

## 7. 技术结构

当前使用：

- Spring Boot
- Spring Cloud Alibaba Nacos Discovery
- Lombok

后续会补：

- MyBatis-Plus
- `controller/service/mapper/xml`
- 统一返回结构
- 链路追踪日志

## 8. 启动方式

```powershell
Set-Location E:\ai-workspaces\chj-aigc-auth-service
mvn spring-boot:run "-Dmaven.repo.local=E:\repository"
```

推荐脚本：

```powershell
Set-Location E:\ai-workspaces\infra\dev
.\start-auth-service.ps1
```

## 9. 日志与链路追踪

当前只有基础控制台日志。  
后续需要接入和平台、租户、网关一致的 `X-Trace-Id` 规范。

## 10. 当前已知限制

- 目前只是骨架，不承担真实登录能力
- 网关路由还没有把 `/api/auth/**` 切到这个服务
- 平台和租户服务暂时还直接依赖原有账号表
