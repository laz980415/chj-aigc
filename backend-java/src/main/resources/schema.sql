create table if not exists model_access_rules (
    id varchar(128) primary key,
    platform_model_alias varchar(128) not null,
    scope_type varchar(32) not null,
    scope_value varchar(128) not null,
    effect varchar(32) not null,
    active boolean not null,
    created_by varchar(128) not null,
    created_at timestamp with time zone not null,
    reason text not null
);

create table if not exists model_access_audit_events (
    id varchar(128) primary key,
    actor_id varchar(128) not null,
    action varchar(64) not null,
    target_model_alias varchar(128) not null,
    target_scope_type varchar(32) not null,
    target_scope_value varchar(128) not null,
    detail text not null,
    created_at timestamp with time zone not null
);

create table if not exists auth_users (
    id varchar(128) primary key,
    username varchar(128) not null unique,
    password varchar(255) not null,
    display_name varchar(128) not null,
    role_key varchar(64) not null,
    tenant_id varchar(128),
    active boolean not null
);

create table if not exists auth_sessions (
    token varchar(128) primary key,
    user_id varchar(128) not null,
    username varchar(128) not null,
    display_name varchar(128) not null,
    role_key varchar(64) not null,
    tenant_id varchar(128),
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null
);

create table if not exists tenant_wallet_ledger (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    entry_type varchar(32) not null,
    amount numeric(18, 4) not null,
    description text not null,
    reference_id varchar(128) not null,
    created_at timestamp with time zone not null
);

create table if not exists tenant_payment_orders (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    channel varchar(64) not null,
    status varchar(64) not null,
    amount numeric(18, 4) not null,
    description varchar(512) not null,
    reference_id varchar(128) not null,
    qr_code varchar(512) not null,
    created_at timestamp with time zone not null,
    paid_at timestamp with time zone
);

create table if not exists tenant_quota_allocations (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    scope_type varchar(32) not null,
    scope_id varchar(128) not null,
    dimension varchar(32) not null,
    limit_value numeric(18, 4) not null,
    used_value numeric(18, 4) not null
);

create table if not exists tenant_projects (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    name varchar(255) not null,
    active boolean not null
);

create table if not exists tenant_clients (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    name varchar(255) not null,
    active boolean not null
);

create table if not exists tenant_brands (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    client_id varchar(128) not null,
    name varchar(255) not null,
    summary text not null,
    active boolean not null
);

create table if not exists tenant_assets (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    project_id varchar(128) not null,
    client_id varchar(128) not null,
    brand_id varchar(128) not null,
    name varchar(255) not null,
    kind varchar(32) not null,
    uri text not null,
    tags text not null,
    active boolean not null
);

comment on table model_access_rules is '平台模型访问规则表，记录超管给租户或角色配置的模型访问策略。';
comment on column model_access_rules.id is '规则主键 ID。';
comment on column model_access_rules.platform_model_alias is '平台统一模型别名。';
comment on column model_access_rules.scope_type is '规则作用范围类型，例如 TENANT、PROJECT、ROLE。';
comment on column model_access_rules.scope_value is '规则作用范围值，例如租户 ID、项目 ID 或角色编码。';
comment on column model_access_rules.effect is '规则效果，允许或拒绝。';
comment on column model_access_rules.active is '规则是否启用。';
comment on column model_access_rules.created_by is '创建规则的操作人标识。';
comment on column model_access_rules.created_at is '规则创建时间。';
comment on column model_access_rules.reason is '规则创建原因说明。';

comment on table model_access_audit_events is '模型访问策略审计事件表，记录超管侧策略变更行为。';
comment on column model_access_audit_events.id is '审计事件主键 ID。';
comment on column model_access_audit_events.actor_id is '执行操作的人员标识。';
comment on column model_access_audit_events.action is '审计动作类型。';
comment on column model_access_audit_events.target_model_alias is '被操作的平台模型别名。';
comment on column model_access_audit_events.target_scope_type is '被操作规则的范围类型。';
comment on column model_access_audit_events.target_scope_value is '被操作规则的范围值。';
comment on column model_access_audit_events.detail is '审计详情说明。';
comment on column model_access_audit_events.created_at is '事件发生时间。';

comment on table auth_users is '登录账号表，保存平台超管和租户账号。';
comment on column auth_users.id is '账号主键 ID。';
comment on column auth_users.username is '登录用户名。';
comment on column auth_users.password is '登录密码，当前为演示明文存储。';
comment on column auth_users.display_name is '页面展示名称。';
comment on column auth_users.role_key is '角色编码。';
comment on column auth_users.tenant_id is '所属租户 ID，平台超管为空。';
comment on column auth_users.active is '账号是否启用。';

comment on table auth_sessions is '登录会话表，保存已签发的访问令牌。';
comment on column auth_sessions.token is '访问令牌。';
comment on column auth_sessions.user_id is '会话所属账号 ID。';
comment on column auth_sessions.username is '会话所属登录名。';
comment on column auth_sessions.display_name is '会话展示名称。';
comment on column auth_sessions.role_key is '会话角色编码。';
comment on column auth_sessions.tenant_id is '会话所属租户 ID。';
comment on column auth_sessions.created_at is '会话创建时间。';
comment on column auth_sessions.expires_at is '会话过期时间。';

comment on table tenant_wallet_ledger is '租户钱包流水表，记录充值和后续扣费账本。';
comment on column tenant_wallet_ledger.id is '流水主键 ID。';
comment on column tenant_wallet_ledger.tenant_id is '所属租户 ID。';
comment on column tenant_wallet_ledger.entry_type is '流水类型，例如 RECHARGE、CONSUME。';
comment on column tenant_wallet_ledger.amount is '本次流水金额。';
comment on column tenant_wallet_ledger.description is '流水说明。';
comment on column tenant_wallet_ledger.reference_id is '业务引用 ID，例如订单号或页面操作号。';
comment on column tenant_wallet_ledger.created_at is '流水创建时间。';

comment on table tenant_payment_orders is '租户支付订单表，记录微信支付下单和模拟支付状态。';
comment on column tenant_payment_orders.id is '支付订单主键 ID。';
comment on column tenant_payment_orders.tenant_id is '所属租户 ID。';
comment on column tenant_payment_orders.channel is '支付渠道，当前为微信原生支付。';
comment on column tenant_payment_orders.status is '支付订单状态，例如 PENDING、PAID。';
comment on column tenant_payment_orders.amount is '订单支付金额。';
comment on column tenant_payment_orders.description is '支付说明。';
comment on column tenant_payment_orders.reference_id is '支付业务引用 ID。';
comment on column tenant_payment_orders.qr_code is '模拟支付二维码或拉起链接。';
comment on column tenant_payment_orders.created_at is '支付订单创建时间。';
comment on column tenant_payment_orders.paid_at is '支付完成时间，未支付时为空。';

comment on table tenant_quota_allocations is '租户额度分配表，保存项目或成员的额度上限和已用量。';
comment on column tenant_quota_allocations.id is '额度分配主键 ID。';
comment on column tenant_quota_allocations.tenant_id is '所属租户 ID。';
comment on column tenant_quota_allocations.scope_type is '额度作用范围类型，例如 PROJECT 或 USER。';
comment on column tenant_quota_allocations.scope_id is '额度作用范围对象 ID，例如项目 ID 或成员 ID。';
comment on column tenant_quota_allocations.dimension is '额度维度，例如图片数、Token 或视频秒数。';
comment on column tenant_quota_allocations.limit_value is '额度上限值。';
comment on column tenant_quota_allocations.used_value is '当前已使用值。';

comment on table tenant_projects is '租户项目表，记录租户内部可投放或可协作的项目。';
comment on column tenant_projects.id is '项目主键 ID。';
comment on column tenant_projects.tenant_id is '所属租户 ID。';
comment on column tenant_projects.name is '项目名称。';
comment on column tenant_projects.active is '项目是否启用。';

comment on table tenant_clients is '租户客户表，记录广告商或品牌方客户。';
comment on column tenant_clients.id is '客户主键 ID。';
comment on column tenant_clients.tenant_id is '所属租户 ID。';
comment on column tenant_clients.name is '客户名称。';
comment on column tenant_clients.active is '客户是否启用。';

comment on table tenant_brands is '租户品牌表，记录客户下的品牌主体。';
comment on column tenant_brands.id is '品牌主键 ID。';
comment on column tenant_brands.tenant_id is '所属租户 ID。';
comment on column tenant_brands.client_id is '所属客户 ID。';
comment on column tenant_brands.name is '品牌名称。';
comment on column tenant_brands.summary is '品牌简介或品牌约束摘要。';
comment on column tenant_brands.active is '品牌是否启用。';

comment on table tenant_assets is '租户素材表，记录品牌生成时可引用的素材资产。';
comment on column tenant_assets.id is '素材主键 ID。';
comment on column tenant_assets.tenant_id is '所属租户 ID。';
comment on column tenant_assets.project_id is '关联项目 ID。';
comment on column tenant_assets.client_id is '关联客户 ID。';
comment on column tenant_assets.brand_id is '关联品牌 ID。';
comment on column tenant_assets.name is '素材名称。';
comment on column tenant_assets.kind is '素材类型，例如 IMAGE、VIDEO、DOCUMENT。';
comment on column tenant_assets.uri is '素材存储地址。';
comment on column tenant_assets.tags is '素材标签列表，当前以文本形式保存。';
comment on column tenant_assets.active is '素材是否启用。';
