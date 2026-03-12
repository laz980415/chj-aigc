# 认证服务拆分说明

## 目标

新增独立的 `chj-aigc-auth-service`，作为后续账号与会话的唯一所有者。

## 当前状态

当前已经完成：

- 新建认证服务骨架
- 接入 `Spring Cloud Alibaba Nacos Discovery`
- 提供 `/api/health` 健康检查
- 提供本地启动脚本 `infra/dev/start-auth-service.ps1`

## 后续要迁移的职责

- `auth_users`
- `auth_sessions`
- 登录
- 会话校验
- 平台超管账号查询
- 租户成员身份查询

## 迁移顺序建议

1. 先把登录和会话校验改成由 `chj-aigc-auth-service` 提供
2. 平台服务改成只通过远程接口获取超管和租户成员摘要
3. 租户服务改成只通过远程接口获取成员身份和会话信息
4. 再切 PostgreSQL 分库，去掉共享 `auth_*` 表
