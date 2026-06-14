create table reason_codes (
    id bigserial primary key,
    code varchar(100) not null unique,
    description varchar(255) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table fraud_cases (
    id bigserial primary key,
    transaction_id bigint not null unique,
    transaction_reference varchar(100) not null,
    account_id bigint not null,
    risk_score integer not null,
    triggered_rules varchar(1000) not null,
    explanations varchar(3000) not null,
    status varchar(30) not null,
    assigned_to varchar(120),
    reason_code_id bigint,
    decision_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_fraud_cases_reason_code
        foreign key (reason_code_id) references reason_codes (id)
);

create table analyst_comments (
    id bigserial primary key,
    fraud_case_id bigint not null,
    analyst varchar(120) not null,
    comment varchar(1000) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_analyst_comments_case
        foreign key (fraud_case_id) references fraud_cases (id) on delete cascade
);

create table review_actions (
    id bigserial primary key,
    fraud_case_id bigint not null,
    action_type varchar(30) not null,
    analyst varchar(120) not null,
    reason_code_id bigint,
    details varchar(1000),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_review_actions_case
        foreign key (fraud_case_id) references fraud_cases (id) on delete cascade,
    constraint fk_review_actions_reason_code
        foreign key (reason_code_id) references reason_codes (id)
);

insert into reason_codes (code, description, active)
values
    ('LEGIT_ACTIVITY', 'Analyst confirmed legitimate customer activity', true),
    ('CONFIRMED_FRAUD', 'Analyst confirmed fraudulent behavior', true),
    ('HIGH_RISK_PATTERN', 'Risk pattern requires blocking decision', true);
