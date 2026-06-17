insert into fraud_rules (code, name, description, weight, enabled)
values (
    'UNUSUAL_TRANSACTION_HOUR',
    'Unusual transaction hour',
    'Flag transactions that occur during unusual overnight hours',
    10,
    true
);
