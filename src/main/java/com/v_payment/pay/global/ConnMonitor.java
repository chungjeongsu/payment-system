package com.v_payment.pay.global;

import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnMonitor {
    private static volatile HikariPoolMXBean pool;

    private ConnMonitor() {}

    public static void init(HikariPoolMXBean hikariPoolMXBean) {
        pool = hikariPoolMXBean;
    }

    public static void logConnectionStatus(String methodName) {
        if(!log.isDebugEnabled()) return;

        log.debug("현재 {} 커넥션 갯수 active = {}, idle = {}, total = {}, pending = {}",
            methodName, pool.getActiveConnections(), pool.getIdleConnections(), pool.getTotalConnections(), pool.getThreadsAwaitingConnection());
    }
}
