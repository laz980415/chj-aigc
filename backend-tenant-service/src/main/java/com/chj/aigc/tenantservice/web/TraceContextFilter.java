package com.chj.aigc.tenantservice.web;

import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthSession;
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
 * 租户服务请求链路过滤器。
 * 负责透传或生成 traceId，写入响应头和日志 MDC，并打印请求起止日志。
 */
public class TraceContextFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTRIBUTE = "traceId";

    private static final Logger log = LoggerFactory.getLogger(TraceContextFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_HEADER));
        long startedAt = System.currentTimeMillis();

        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        MDC.put("traceId", traceId);

        log.info("租户请求开始 traceId={} method={} path={}",
                traceId, request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            log.error("租户请求异常 traceId={} method={} path={} error={}",
                    traceId, request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
            throw exception;
        } finally {
            AuthSession session = (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("租户请求完成 traceId={} method={} path={} status={} durationMs={} userId={} role={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    session == null ? "-" : session.userId(),
                    session == null ? "-" : session.roleKey());
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
