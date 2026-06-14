create table fraud_rules (
    id bigserial primary key,
    code varchar(100) not null unique,
    name varchar(200) not null,
    description varchar(500) not null,
    weight integer not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table fraud_decisions (
    id bigserial primary key,
    transaction_id bigint not null unique,
    transaction_reference varchar(100) not null,
    account_id bigint not null,
    risk_score integer not null,
    decision varchar(30) not null,
    triggered_rules varchar(1000) not null,
    explanations varchar(3000) not null,
    decided_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

insert into fraud_rules (code, name, description, weight, enabled)
values
    ('LARGE_AMOUNT_DEVIATION', 'Large amount deviation', 'Flag transactions above the large amount threshold', 45, true),
    ('RAPID_TRANSACTION_FREQUENCY', 'Rapid transaction frequency', 'Flag multiple transactions in a short period', 30, true),
    ('NEW_DEVICE_DETECTION', 'New device detection', 'Flag transactions from an unseen device', 25, true);
