insert into fraud_rules (code, name, description, weight, enabled)
values (
    'HIGH_RISK_ACCOUNT_BEHAVIOR',
    'High-risk account behavior',
    'Flag accounts that accumulate multiple risky behavior signals within a rolling short-term window',
    40,
    true
);
