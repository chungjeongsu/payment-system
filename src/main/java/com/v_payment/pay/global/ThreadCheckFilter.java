package com.v_payment.pay.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ThreadCheckFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().contains("/approve")) {
            Thread t = Thread.currentThread();

            log.info("[THREAD-CHECK][START] uri={} threadName={} virtual={} thread={}",
                    request.getRequestURI(),
                    t.getName(),
                    t.isVirtual(),
                    t
            );
        }

        filterChain.doFilter(request, response);
    }
}