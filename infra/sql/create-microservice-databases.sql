-- 微服务数据库物理拆分脚本
-- 执行顺序：
--   1. 以 postgres 超级用户连接 chj-aigc 执行本文件
--   2. 各服务 application.yml 已配置独立数据库 URL，重启服务后 schema.sql 自动建表
--   3. 确认数据迁移完成后可选择性删除旧库中已迁移的表

-- ============================================================
-- 第一步：创建三个独立数据库
-- ============================================================
create database "chj-aigc-auth"
    with owner = postgres encoding = 'UTF8';

create database "chj-aigc-platform"
    with owner = postgres encoding = 'UTF8';

create database "chj-aigc-tenant"
    with owner = postgres encoding = 'UTF8';
