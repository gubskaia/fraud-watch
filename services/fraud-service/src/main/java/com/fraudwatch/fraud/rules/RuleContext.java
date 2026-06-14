package com.fraudwatch.fraud.rules;

import com.fraudwatch.events.transaction.TransactionCreatedPayload;

public record RuleContext(TransactionCreatedPayload transaction) {
}

