package com.opspulse.risk.application;

import com.opspulse.risk.application.port.in.RiskRule;
import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskFinding;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RiskEngine {

    private final List<RiskRule> rules;

    public RiskEngine(List<RiskRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<RiskFinding> evaluate(List<RiskContext> contexts) {
        var findings = new ArrayList<RiskFinding>();
        for (var context : contexts) {
            for (var rule : rules) {
                rule.evaluate(context).ifPresent(findings::add);
            }
        }
        return List.copyOf(findings);
    }

    public int ruleCount() {
        return rules.size();
    }
}
