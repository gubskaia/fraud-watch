insert into fraud_rules (code, name, description, weight, enabled)
values (
    'REPEATED_FAILED_ATTEMPTS',
    'Repeated failed attempts',
    'Flag accounts that recently accumulated multiple blocked transaction attempts',
    20,
    true
);
