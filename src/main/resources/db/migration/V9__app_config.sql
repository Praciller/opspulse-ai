CREATE TABLE app_config (
    key VARCHAR(64) PRIMARY KEY,
    value JSONB NOT NULL
);

INSERT INTO app_config (key, value) VALUES
    ('slowMovingDays', '30'::jsonb),
    ('excessiveDaysThreshold', '90'::jsonb),
    ('lowMarginThresholdPct', '20'::jsonb),
    ('lateDeliveryRateThresholdPct', '20'::jsonb),
    ('orderDelayNearDays', '2'::jsonb),
    ('riskScanCron', '"0 0 7 * * *"'::jsonb),
    ('allowNegativeStock', 'false'::jsonb)
ON CONFLICT (key) DO NOTHING;
