insert into fraud_rules (code, name, description, weight, enabled)
values (
    'NEW_IP_DETECTION',
    'New IP detection',
    'Flag transactions from an unseen IP address for the account',
    15,
    true
);
