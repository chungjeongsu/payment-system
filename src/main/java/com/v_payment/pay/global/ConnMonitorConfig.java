package com.v_payment.pay.global;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ConnMonitorConfig {
    private final DataSource dataSource;

    @PostConstruct
    void init() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        ConnMonitor.init(hikariDataSource.getHikariPoolMXBean());
    }
}
