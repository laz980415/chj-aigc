-- 为微服务拆分准备独立数据库。
-- 请在 PostgreSQL 超级用户或具备建库权限的账号下执行。

create database "chj-aigc-platform"
    with owner = postgres
    encoding = 'UTF8';

create database "chj-aigc-tenant"
    with owner = postgres
    encoding = 'UTF8';
