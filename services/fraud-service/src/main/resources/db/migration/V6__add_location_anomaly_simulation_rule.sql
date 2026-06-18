insert into fraud_rules (code, name, description, weight, enabled)
values (
    'LOCATION_ANOMALY_SIMULATION',
    'Location anomaly simulation',
    'Flag accounts whose transaction IP maps to a simulated new region within the retention window',
    20,
    true
);
