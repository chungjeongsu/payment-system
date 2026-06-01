package com.v_payment.pay.payment.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MeterConfig {
    @Bean
    public MeterFilter dashboardMetricsOnlyFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return isDashboardMetric(id.getName()) ? MeterFilterReply.NEUTRAL : MeterFilterReply.DENY;
            }

            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .serviceLevelObjectives(
                                    Duration.ofMillis(100).toNanos(),
                                    Duration.ofMillis(300).toNanos(),
                                    Duration.ofMillis(500).toNanos(),
                                    Duration.ofSeconds(1).toNanos(),
                                    Duration.ofSeconds(2).toNanos()
                            ).build().merge(config);
                }
                return config;
            }
        };
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    private static boolean isDashboardMetric(String name) {
        return name.equals("http.server.requests")
                || name.equals("http.server.requests.active")
                || name.equals("pay.api")
                || name.startsWith("hikaricp.connections")
                || name.equals("jvm.memory.used")
                || name.equals("jvm.memory.committed")
                || name.equals("jvm.gc.pause")
                || name.equals("jvm.threads.live")
                || name.equals("jvm.threads.peak")
                || name.equals("jvm.threads.daemon")
                || name.equals("process.cpu.usage")
                || name.equals("system.cpu.usage");
    }
}
