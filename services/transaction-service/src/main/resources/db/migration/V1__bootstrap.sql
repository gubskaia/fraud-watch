create table accounts (
    id bigserial primary key,
    account_number varchar(50) not null unique,
    customer_id varchar(100) not null,
    owner_name varchar(200) not null,
    currency varchar(3) not null,
    balance numeric(19,4) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table transactions (
    id bigserial primary key,
    transaction_reference varchar(100) not null unique,
    account_id bigint not null,
    amount numeric(19,4) not null,
    currency varchar(3) not null,
    direction varchar(20) not null,
    status varchar(30) not null,
    merchant_name varchar(200) not null,
    merchant_category varchar(100) not null,
    device_id varchar(255),
    ip_address varchar(64),
    description varchar(500),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_transactions_account
        foreign key (account_id) references accounts (id)
);

create table idempotency_records (
    id bigserial primary key,
    idempotency_key varchar(255) not null unique,
    request_hash varchar(64) not null,
    transaction_id bigint not null,
    completed_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_idempotency_transaction
        foreign key (transaction_id) references transactions (id) on delete cascade
);

create index idx_transactions_account_id on transactions (account_id);
create index idx_transactions_created_at on transactions (created_at);

insert into accounts (account_number, customer_id, owner_name, currency, balance, status)
values
    ('FW-ACC-100001', 'customer-100001', 'Alice Carter', 'USD', 25000.0000, 'ACTIVE'),
    ('FW-ACC-100002', 'customer-100002', 'Bob Turner', 'USD', 1500.0000, 'ACTIVE');
