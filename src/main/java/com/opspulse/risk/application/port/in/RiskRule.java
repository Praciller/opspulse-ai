package com.opspulse.risk.application.port.in;

import com.opspulse.risk.domain.RiskContext;
import com.opspulse.risk.domain.RiskFinding;
import com.opspulse.risk.domain.RiskType;
import java.util.Optional;

public interface RiskRule {

    RiskType type();

    Optional<RiskFinding> evaluate(RiskContext context);
}
