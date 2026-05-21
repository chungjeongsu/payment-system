package com.v_payment.pay.log;

import com.v_payment.pay.global.LTimer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.v_payment.pay.log.ApiLogContext.*;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ApiLogContext apiLogContext = ApiLogContext.create(request);

        long start = LTimer.getCurrTime();
        try {
            MDC.put(TRACE_ID, apiLogContext.traceId());
            MDC.put(METHOD, apiLogContext.method());
            MDC.put(PATH, apiLogContext.path());

            log.info("요청 시작");
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(ELAPSED_MS, String.valueOf(LTimer.getDiff(start)));
            log.info("요청 끝 status = {}", response.getStatus());

            MDC.remove(TRACE_ID);
            MDC.remove(METHOD);
            MDC.remove(PATH);
            MDC.remove(ELAPSED_MS);
        }
    }
}
