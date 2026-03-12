# chj-aigc-auth-service 服务说明

## 1. 服务定位

`chj-aigc-auth-service` 是独立认证服务骨架。  
它的目标是成为账号、登录、会话、身份校验的唯一所有者。

## 2. 当前职责

当前已经完成的能力包括：

- Spring Boot 服务启动
- Nacos 注册发现接入
- 登录接口
- 当前会话查询接口
- 网关 `/api/auth/**` 统一入口
- MyBatis XML 持久化
- 统一返回结构
- 基础链路日志

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

当前主要接口：

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/auth/me`

## 6. 数据存储

当前认证服务已经接 PostgreSQL，并暂时复用：

- `auth_users`
- `auth_sessions`

后续再把这两张表的所有权彻底收敛到认证服务，并让其他服务通过远程鉴权或令牌解析访问。

## 7. 技术结构

当前使用：

- Spring Boot
- Spring Cloud Alibaba Nacos Discovery
- MyBatis-Plus
- Lombok
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

当前已经接入基础 `X-Trace-Id` 规范，网关透传后会写入认证服务日志。

## 10. 当前已知限制

- 目前只接管了 `/api/auth/**` 的登录和会话查询
- 平台和租户服务暂时还直接依赖原有账号表
- 租户服务内部仍保留成员管理所需的账号存储能力，后续还要继续下沉到认证服务
