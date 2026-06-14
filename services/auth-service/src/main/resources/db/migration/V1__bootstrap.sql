create table permissions (
    id bigserial primary key,
    code varchar(100) not null unique,
    description varchar(255) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table roles (
    id bigserial primary key,
    code varchar(50) not null unique,
    description varchar(255) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table users (
    id bigserial primary key,
    username varchar(120) not null unique,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    status varchar(20) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table role_permissions (
    role_id bigint not null,
    permission_id bigint not null,
    primary key (role_id, permission_id),
    constraint fk_role_permissions_role
        foreign key (role_id) references roles (id) on delete cascade,
    constraint fk_role_permissions_permission
        foreign key (permission_id) references permissions (id) on delete cascade
);

create table user_roles (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    constraint fk_user_roles_user
        foreign key (user_id) references users (id) on delete cascade,
    constraint fk_user_roles_role
        foreign key (role_id) references roles (id) on delete cascade
);

create table refresh_tokens (
    id bigserial primary key,
    user_id bigint not null,
    token varchar(255) not null unique,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    device_id varchar(255),
    ip_address varchar(64),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_refresh_tokens_user
        foreign key (user_id) references users (id) on delete cascade
);

create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
create index idx_refresh_tokens_expires_at on refresh_tokens (expires_at);

insert into permissions (code, description)
values
    ('AUTH_LOGIN', 'Allow login into the platform'),
    ('USER_SELF_READ', 'Read own user profile'),
    ('TRANSACTION_CREATE', 'Create transactions'),
    ('TRANSACTION_READ', 'Read transactions'),
    ('FRAUD_RULE_READ', 'Read fraud rules'),
    ('FRAUD_RULE_WRITE', 'Manage fraud rules'),
    ('REVIEW_CASE_READ', 'Read review cases'),
    ('REVIEW_CASE_DECIDE', 'Approve or block review cases'),
    ('AUDIT_READ', 'Read audit history'),
    ('NOTIFICATION_READ', 'Read notifications');

insert into roles (code, description)
values
    ('ROLE_ADMIN', 'Platform administrator'),
    ('ROLE_ANALYST', 'Fraud analyst'),
    ('ROLE_CUSTOMER', 'Bank customer');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in (
    'AUTH_LOGIN',
    'USER_SELF_READ',
    'TRANSACTION_CREATE',
    'TRANSACTION_READ',
    'FRAUD_RULE_READ',
    'FRAUD_RULE_WRITE',
    'REVIEW_CASE_READ',
    'REVIEW_CASE_DECIDE',
    'AUDIT_READ',
    'NOTIFICATION_READ'
)
where r.code = 'ROLE_ADMIN';

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in (
    'AUTH_LOGIN',
    'USER_SELF_READ',
    'TRANSACTION_READ',
    'FRAUD_RULE_READ',
    'REVIEW_CASE_READ',
    'REVIEW_CASE_DECIDE',
    'AUDIT_READ',
    'NOTIFICATION_READ'
)
where r.code = 'ROLE_ANALYST';

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in (
    'AUTH_LOGIN',
    'USER_SELF_READ',
    'TRANSACTION_CREATE',
    'TRANSACTION_READ',
    'NOTIFICATION_READ'
)
where r.code = 'ROLE_CUSTOMER';
