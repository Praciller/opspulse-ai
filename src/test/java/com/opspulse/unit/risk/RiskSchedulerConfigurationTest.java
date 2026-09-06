package com.opspulse.unit.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspulse.risk.application.RiskScanService;
import com.opspulse.risk.application.port.out.AppConfigProvider;
import com.opspulse.risk.domain.RiskConfig;
import com.opspulse.risk.infrastructure.scheduling.RiskSchedulerConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.SimpleTriggerContext;

class RiskSchedulerConfigurationTest {

    @Test
    void schedulesNextExecutionFromTypedAppConfigCronInUtc() {
        var config = mock(AppConfigProvider.class);
        when(config.current()).thenReturn(new RiskConfig(
                30, 90, new java.math.BigDecimal("20"), new java.math.BigDecimal("20"), 2,
                "0 0 7 * * *"));
        var scan = mock(RiskScanService.class);
        var clock = Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC);
        var scheduler = new RiskSchedulerConfiguration(scan, config, clock);
        var registrar = new ScheduledTaskRegistrar();

        scheduler.configureTasks(registrar);

        TriggerTask task = registrar.getTriggerTaskList().getFirst();
        assertThat(task.getTrigger().nextExecution(new SimpleTriggerContext()))
                .isEqualTo(Instant.parse("2026-07-17T07:00:00Z"));
        task.getRunnable().run();
        verify(scan).runScan();
    }
}
