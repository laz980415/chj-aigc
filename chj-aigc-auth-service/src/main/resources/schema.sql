create table if not exists auth_users (
    id varchar(64) primary key,
    username varchar(128) not null unique,
    password varchar(128) not null,
    display_name varchar(128) not null,
    role_key varchar(64) not null,
    tenant_id varchar(64),
    active boolean not null default true
);

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

comment on table auth_users is '认证账号表';
comment on column auth_users.id is '账号主键';
comment on column auth_users.username is '登录用户名';
comment on column auth_users.password is '登录密码';
comment on column auth_users.display_name is '显示名称';
comment on column auth_users.role_key is '角色标识';
comment on column auth_users.tenant_id is '所属租户ID';
comment on column auth_users.active is '是否启用';

comment on table auth_sessions is '认证会话表';
comment on column auth_sessions.token is '会话令牌';
comment on column auth_sessions.user_id is '账号ID';
comment on column auth_sessions.username is '用户名';
comment on column auth_sessions.display_name is '显示名称';
comment on column auth_sessions.role_key is '角色标识';
comment on column auth_sessions.tenant_id is '所属租户ID';
comment on column auth_sessions.created_at is '会话创建时间';
comment on column auth_sessions.expires_at is '会话过期时间';
