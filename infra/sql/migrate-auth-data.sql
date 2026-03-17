-- 认证服务数据迁移脚本
-- 在源库 chj-aigc 执行，将 auth_users / auth_sessions 数据迁移到 chj-aigc-auth
-- 前提：已在 chj-aigc-auth 库中通过 chj-aigc-auth-service 启动建好表结构

-- 使用 postgres_fdw 跨库迁移（需要超级用户权限）
-- 或者直接用 pg_dump / psql 管道方式：
--   pg_dump -h 36.150.108.207 -p 54312 -U postgres -t auth_users -t auth_sessions chj-aigc \
--     | psql -h 36.150.108.207 -p 54312 -U postgres chj-aigc-auth

-- 如果在同一 PostgreSQL 实例内，也可以用 dblink：
-- insert into dblink('dbname=chj-aigc-auth host=36.150.108.207 port=54312 user=postgres password=Linten@2023!',
--   'select id,username,password,display_name,role_key,tenant_id,active from auth_users')
--   as t(id varchar,username varchar,password varchar,display_name varchar,role_key varchar,tenant_id varchar,active boolean)
-- on conflict (id) do nothing;

-- 推荐方式（本地 psql 执行）：
-- pg_dump -h 36.150.108.207 -p 54312 -U postgres \
--   --data-only -t auth_users -t auth_sessions chj-aigc \
--   | psql -h 36.150.108.207 -p 54312 -U postgres chj-aigc-auth
