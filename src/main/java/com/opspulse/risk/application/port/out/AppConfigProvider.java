package com.opspulse.risk.application.port.out;

import com.opspulse.risk.domain.RiskConfig;

/**
 * Typed access to the scalar values stored in {@code app_config}.
 *
 * <p>Keeping this port independent from JDBC lets the risk engine and its
 * scheduler consume configuration without knowing how the values are stored.
 */
public interface AppConfigProvider {

    RiskConfig current();
}
