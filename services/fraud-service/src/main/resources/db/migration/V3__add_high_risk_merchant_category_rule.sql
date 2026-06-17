insert into fraud_rules (code, name, description, weight, enabled)
values (
    'HIGH_RISK_MERCHANT_CATEGORY',
    'High-risk merchant category',
    'Flag transactions in categories associated with elevated fraud exposure',
    20,
    true
);
