package com.v_payment.pay.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.v_payment.pay.log.ApiLogContext.*;

@Slf4j(topic = "API_LOGGER")
@Aspect
@Component
public class ApiLogAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    void apiLogPointcut(){}

    @Around("apiLogPointcut()")
    public Object applyApiMdc(ProceedingJoinPoint joinPoint) throws Throwable{
        ApiLogContext apiLogContext = resolveApiLogContext();
        Long startTime = System.currentTimeMillis();

        try{
            MDC.put(TRACE_ID, apiLogContext.traceId());
            MDC.put(METHOD, apiLogContext.method());
            MDC.put(PATH, apiLogContext.path());

            return joinPoint.proceed();
        } finally {
            Long endTime = System.currentTimeMillis();
            MDC.put(ELAPSED_MS, String.valueOf(endTime - startTime));
            MDC.clear();
        }
    }

    private ApiLogContext resolveApiLogContext() {
        ServletRequestAttributes reqAttr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(reqAttr == null) return ApiLogContext.empty();
        HttpServletRequest request = reqAttr.getRequest();
        return ApiLogContext.create(request);
    }
}
