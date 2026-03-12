create table if not exists auth_users (
    id varchar(64) primary key,
    username varchar(128) not null unique,
    password varchar(255) not null,
    display_name varchar(128) not null,
    role_key varchar(64) not null,
    tenant_id varchar(64),
    active boolean not null default true
);
comment on table auth_users is '登录账号表';
comment on column auth_users.id is '账号主键';
comment on column auth_users.username is '登录用户名';
comment on column auth_users.password is '登录密码';
comment on column auth_users.display_name is '显示名称';
comment on column auth_users.role_key is '角色编码';
comment on column auth_users.tenant_id is '所属租户标识';
comment on column auth_users.active is '是否启用';

alter table if exists auth_users add column if not exists display_name varchar(128);
alter table if exists auth_users add column if not exists role_key varchar(64);
alter table if exists auth_users add column if not exists tenant_id varchar(64);
alter table if exists auth_users add column if not exists active boolean default true;
update auth_users
set display_name = coalesce(display_name, username),
    role_key = coalesce(role_key, 'tenant_member'),
    active = coalesce(active, true);
alter table if exists auth_users alter column display_name set not null;
alter table if exists auth_users alter column role_key set not null;
alter table if exists auth_users alter column active set not null;

create table if not exists auth_sessions (
    token varchar(128) primary key,
    user_id varchar(64) not null,
    username varchar(128) not null,
    display_name varchar(128) not null,
    role_key varchar(64) not null,
    tenant_id varchar(64),
    created_at timestamp not null,
    expires_at timestamp not null
);
comment on table auth_sessions is '登录会话表';
comment on column auth_sessions.token is '会话令牌';
comment on column auth_sessions.user_id is '账号主键';
comment on column auth_sessions.username is '登录用户名';
comment on column auth_sessions.display_name is '显示名称';
comment on column auth_sessions.role_key is '角色编码';
comment on column auth_sessions.tenant_id is '所属租户标识';
comment on column auth_sessions.created_at is '创建时间';
comment on column auth_sessions.expires_at is '过期时间';

alter table if exists auth_sessions add column if not exists display_name varchar(128);
alter table if exists auth_sessions add column if not exists role_key varchar(64);
alter table if exists auth_sessions add column if not exists tenant_id varchar(64);

create table if not exists tenant_projects (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    name varchar(128) not null,
    active boolean not null default true
);
comment on table tenant_projects is '租户项目表';
comment on column tenant_projects.id is '项目主键';
comment on column tenant_projects.tenant_id is '所属租户标识';
comment on column tenant_projects.name is '项目名称';
comment on column tenant_projects.active is '是否启用';

alter table if exists tenant_projects add column if not exists active boolean default true;
update tenant_projects set active = coalesce(active, true);
alter table if exists tenant_projects alter column active set not null;

create table if not exists tenant_quota_allocations (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    scope_type varchar(32) not null,
    scope_id varchar(64) not null,
    dimension varchar(32) not null,
    limit_value numeric(18, 4) not null,
    used_value numeric(18, 4) not null
);
comment on table tenant_quota_allocations is '租户额度分配表';
comment on column tenant_quota_allocations.id is '额度分配主键';
comment on column tenant_quota_allocations.tenant_id is '所属租户标识';
comment on column tenant_quota_allocations.scope_type is '额度范围类型';
comment on column tenant_quota_allocations.scope_id is '额度范围对象标识';
comment on column tenant_quota_allocations.dimension is '额度维度';
comment on column tenant_quota_allocations.limit_value is '总额度上限';
comment on column tenant_quota_allocations.used_value is '已使用额度';

create table if not exists tenant_wallet_ledger (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    entry_type varchar(32) not null,
    amount numeric(18, 4) not null,
    description varchar(255) not null,
    reference_id varchar(64) not null,
    created_at timestamp not null
);
comment on table tenant_wallet_ledger is '租户钱包流水表';
comment on column tenant_wallet_ledger.id is '钱包流水主键';
comment on column tenant_wallet_ledger.tenant_id is '所属租户标识';
comment on column tenant_wallet_ledger.entry_type is '流水类型';
comment on column tenant_wallet_ledger.amount is '变动金额';
comment on column tenant_wallet_ledger.description is '流水说明';
comment on column tenant_wallet_ledger.reference_id is '业务引用标识';
comment on column tenant_wallet_ledger.created_at is '创建时间';

create table if not exists tenant_payment_orders (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    channel varchar(32) not null,
    status varchar(32) not null,
    amount numeric(18, 4) not null,
    description varchar(255) not null,
    reference_id varchar(64) not null,
    qr_code varchar(255) not null,
    created_at timestamp not null,
    paid_at timestamp
);
comment on table tenant_payment_orders is '租户支付订单表';
comment on column tenant_payment_orders.id is '支付订单主键';
comment on column tenant_payment_orders.tenant_id is '所属租户标识';
comment on column tenant_payment_orders.channel is '支付通道';
comment on column tenant_payment_orders.status is '订单状态';
comment on column tenant_payment_orders.amount is '订单金额';
comment on column tenant_payment_orders.description is '订单描述';
comment on column tenant_payment_orders.reference_id is '业务引用标识';
comment on column tenant_payment_orders.qr_code is '支付二维码或拉起链接';
comment on column tenant_payment_orders.created_at is '创建时间';
comment on column tenant_payment_orders.paid_at is '支付完成时间';

create table if not exists tenant_clients (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    name varchar(128) not null,
    active boolean not null default true
);
comment on table tenant_clients is '租户客户表';
comment on column tenant_clients.id is '客户主键';
comment on column tenant_clients.tenant_id is '所属租户标识';
comment on column tenant_clients.name is '客户名称';
comment on column tenant_clients.active is '是否启用';

alter table if exists tenant_clients add column if not exists active boolean default true;
update tenant_clients set active = coalesce(active, true);
alter table if exists tenant_clients alter column active set not null;

create table if not exists tenant_brands (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    client_id varchar(64) not null,
    name varchar(128) not null,
    summary varchar(255) not null default '',
    active boolean not null default true
);
comment on table tenant_brands is '租户品牌表';
comment on column tenant_brands.id is '品牌主键';
comment on column tenant_brands.tenant_id is '所属租户标识';
comment on column tenant_brands.client_id is '所属客户标识';
comment on column tenant_brands.name is '品牌名称';
comment on column tenant_brands.summary is '品牌摘要';
comment on column tenant_brands.active is '是否启用';

alter table if exists tenant_brands add column if not exists active boolean default true;
alter table if exists tenant_brands add column if not exists summary varchar(255) default '';
update tenant_brands
set active = coalesce(active, true),
    summary = coalesce(summary, '');
alter table if exists tenant_brands alter column active set not null;
alter table if exists tenant_brands alter column summary set not null;

create table if not exists tenant_assets (
    id varchar(64) primary key,
    tenant_id varchar(64) not null,
    project_id varchar(64) not null,
    client_id varchar(64) not null,
    brand_id varchar(64) not null,
    name varchar(128) not null,
    kind varchar(32) not null,
    uri varchar(255) not null,
    tags varchar(255),
    active boolean not null default true
);
comment on table tenant_assets is '租户素材表';
comment on column tenant_assets.id is '素材主键';
comment on column tenant_assets.tenant_id is '所属租户标识';
comment on column tenant_assets.project_id is '所属项目标识';
comment on column tenant_assets.client_id is '所属客户标识';
comment on column tenant_assets.brand_id is '所属品牌标识';
comment on column tenant_assets.name is '素材名称';
comment on column tenant_assets.kind is '素材类型';
comment on column tenant_assets.uri is '素材存储地址';
comment on column tenant_assets.tags is '素材标签，使用逗号分隔';
comment on column tenant_assets.active is '是否启用';

alter table if exists tenant_assets add column if not exists active boolean default true;
alter table if exists tenant_assets add column if not exists tags varchar(255);
update tenant_assets set active = coalesce(active, true);
alter table if exists tenant_assets alter column active set not null;
