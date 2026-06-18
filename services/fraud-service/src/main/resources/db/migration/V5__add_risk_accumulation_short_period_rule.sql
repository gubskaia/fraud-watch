insert into fraud_rules (code, name, description, weight, enabled)
values (
    'RISK_ACCUMULATION_SHORT_PERIOD',
    'Risk accumulation over short period',
    'Flag accounts whose cumulative transaction amount exceeds the short-period threshold',
    35,
    true
);
