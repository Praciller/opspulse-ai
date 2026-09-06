package com.opspulse.risk.infrastructure.scheduling;

import com.opspulse.risk.application.RiskScanService;
import com.opspulse.risk.application.port.out.AppConfigProvider;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;

@Configuration
public class RiskSchedulerConfiguration implements SchedulingConfigurer {

    private final RiskScanService scans;
    private final AppConfigProvider configuration;
    private final Clock clock;

    public RiskSchedulerConfiguration(
            RiskScanService scans,
            AppConfigProvider configuration,
            Clock clock) {
        this.scans = scans;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(scans::runScan, trigger(configuration, clock));
    }

    private static Trigger trigger(AppConfigProvider configuration, Clock clock) {
        return context -> {
            var cron = CronExpression.parse(configuration.current().riskScanCron());
            var base = Optional.ofNullable(context.lastCompletion())
                    .orElse(clock.instant());
            var next = cron.next(base.atZone(ZoneOffset.UTC));
            return next == null ? null : next.toInstant();
        };
    }
}
