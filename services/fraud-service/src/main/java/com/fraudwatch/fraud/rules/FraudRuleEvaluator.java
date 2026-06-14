package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.util.Optional;

public interface FraudRuleEvaluator {

    String supportedRuleCode();

    Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context);
}

