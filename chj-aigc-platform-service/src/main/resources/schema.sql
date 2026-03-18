-- 平台服务数据库表定义
-- auth_users / auth_sessions 已迁移到 chj-aigc-auth-service，本服务不再创建这两张表。

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

create table if not exists tenant_quota_allocations (
    id varchar(128) primary key,
    tenant_id varchar(128) not null,
    scope_type varchar(32) not null,
    scope_id varchar(128) not null,
    dimension varchar(64) not null,
    limit_value numeric(18, 4) not null,
    used_value numeric(18, 4) not null
);

comment on table tenant_quota_allocations is '租户额度分配表，记录项目和成员的配额上限与已用量。';
comment on column tenant_quota_allocations.id is '分配记录主键 ID。';
comment on column tenant_quota_allocations.tenant_id is '所属租户 ID。';
comment on column tenant_quota_allocations.scope_type is '分配范围类型，例如 PROJECT、USER。';
comment on column tenant_quota_allocations.scope_id is '分配范围 ID。';
comment on column tenant_quota_allocations.dimension is '额度维度，例如 TOKEN、IMAGE。';
comment on column tenant_quota_allocations.limit_value is '额度上限。';
comment on column tenant_quota_allocations.used_value is '已使用量。';

-- 供应商配置表，超管在平台后台维护各模型供应商的 API Key 和接入地址
create table if not exists provider_configs (
    id varchar(128) primary key,
    provider_id varchar(64) not null unique,
    display_name varchar(128) not null,
    api_base_url varchar(512) not null,
    api_key_encrypted text not null,
    enabled boolean not null default true,
    updated_by varchar(128) not null,
    updated_at timestamp with time zone not null
);

comment on table provider_configs is '模型供应商配置表，超管维护各供应商 API Key 和接入地址。';
comment on column provider_configs.provider_id is '供应商标识，与 platform_core/models.py 中的 provider id 对应。';
comment on column provider_configs.api_key_encrypted is 'API Key，生产环境应加密存储。';
comment on column provider_configs.enabled is '是否启用该供应商。';
