package com.chj.aigc.authservice.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 认证服务链路日志过滤器。
 */
public class TraceContextFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(TraceContextFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_HEADER));
        long startedAt = System.currentTimeMillis();
        response.setHeader(TRACE_HEADER, traceId);
        MDC.put("traceId", traceId);

        log.info("认证请求开始 traceId={} method={} path={}", traceId, request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("认证请求完成 traceId={} method={} path={} status={} durationMs={}",
                    traceId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                    System.currentTimeMillis() - startedAt);
            MDC.remove("traceId");
        }
    }

    private String resolveTraceId(String incomingTraceId) {
        if (incomingTraceId == null || incomingTraceId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return incomingTraceId;
    }
}
