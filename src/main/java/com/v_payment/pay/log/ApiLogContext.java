package com.v_payment.pay.log;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public record ApiLogContext(
    String traceId,
    String method,
    String path
) {
    public static final String TRACE_ID = "TRACE_ID";
    public static final String METHOD = "METHOD";
    public static final String PATH = "PATH";
    public static final String ELAPSED_MS = "ELAPSED_MS";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static ApiLogContext empty() {
        return new ApiLogContext("", "", "");
    }

    public static ApiLogContext create(HttpServletRequest request) {
        return new ApiLogContext(
            resolveTraceId(request),
            request.getMethod(),
            request.getRequestURI()
        );
    }

    private static String resolveTraceId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }
}
