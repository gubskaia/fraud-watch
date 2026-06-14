create table audit_records (
    id bigserial primary key,
    event_id varchar(100) not null unique,
    event_type varchar(100) not null,
    aggregate_type varchar(100) not null,
    aggregate_id varchar(100) not null,
    correlation_id varchar(100),
    source varchar(120) not null,
    summary varchar(1000) not null,
    payload_json varchar(8000) not null,
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp
);

create index idx_audit_records_occurred_at on audit_records (occurred_at desc);
create index idx_audit_records_aggregate on audit_records (aggregate_type, aggregate_id);
