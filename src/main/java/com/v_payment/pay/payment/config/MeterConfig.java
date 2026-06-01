package com.v_payment.pay.payment.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterConfig {
    @Bean
    public MeterFilter dashboardMetricsOnlyFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return isDashboardMetric(id.getName()) ? MeterFilterReply.NEUTRAL : MeterFilterReply.DENY;
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
