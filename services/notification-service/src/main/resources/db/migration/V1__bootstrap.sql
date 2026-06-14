create table notification_templates (
    id bigserial primary key,
    code varchar(100) not null unique,
    subject_template varchar(255) not null,
    body_template varchar(2000) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table notifications (
    id bigserial primary key,
    event_id varchar(100) not null unique,
    recipient_ref varchar(120) not null,
    channel varchar(20) not null,
    subject varchar(255) not null,
    body varchar(2000) not null,
    status varchar(20) not null,
    related_entity_type varchar(100) not null,
    related_entity_id varchar(100) not null,
    last_attempt_at timestamp with time zone,
    template_id bigint,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_notifications_template
        foreign key (template_id) references notification_templates (id)
);

create table delivery_attempts (
    id bigserial primary key,
    notification_id bigint not null,
    attempt_number integer not null,
    status varchar(20) not null,
    details varchar(1000) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_delivery_attempts_notification
        foreign key (notification_id) references notifications (id) on delete cascade
);

insert into notification_templates (code, subject_template, body_template, active)
values
    ('TRANSACTION_APPROVED', 'Transaction {{transactionReference}} approved', 'Your transaction {{transactionReference}} was approved with risk score {{riskScore}}.', true),
    ('TRANSACTION_BLOCKED', 'Transaction {{transactionReference}} blocked', 'Your transaction {{transactionReference}} was blocked after fraud evaluation. Risk score: {{riskScore}}.', true),
    ('TRANSACTION_REVIEW_REQUIRED', 'Transaction {{transactionReference}} under review', 'Your transaction {{transactionReference}} requires analyst review. Current risk score: {{riskScore}}.', true),
    ('REVIEW_DECISION_MADE', 'Review completed for {{transactionReference}}', 'Manual review is complete. Final decision: {{decision}}. Reason: {{reasonCode}}. Analyst: {{analyst}}.', true);
