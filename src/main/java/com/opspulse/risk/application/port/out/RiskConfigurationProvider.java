package com.opspulse.risk.application.port.out;

/**
 * Backward-compatible name for the typed application configuration port.
 * New callers should depend on {@link AppConfigProvider}.
 */
public interface RiskConfigurationProvider extends AppConfigProvider {
}
