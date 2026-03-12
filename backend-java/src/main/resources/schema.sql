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
