package com.chj.aigc.gateway.logging;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 为网关请求生成或透传 traceId，并在入口与出口打印链路日志。
 */
@Component
public class GatewayTraceFilter implements GlobalFilter, Ordered {
    public static final String TRACE_HEADER = "X-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(GatewayTraceFilter.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange.getRequest().getHeaders());
        long startedAt = System.currentTimeMillis();

        ServerHttpRequest tracedRequest = exchange.getRequest().mutate()
                .header(TRACE_HEADER, traceId)
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(tracedRequest).build();
        tracedExchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);

        log.info("网关请求开始 traceId={} method={} path={}",
                traceId,
                tracedExchange.getRequest().getMethod(),
                tracedExchange.getRequest().getURI().getRawPath());

        return chain.filter(tracedExchange)
                .doOnSuccess(unused -> logCompletion(tracedExchange, traceId, startedAt, null))
                .doOnError(error -> logCompletion(tracedExchange, traceId, startedAt, error));
    }

    private void logCompletion(ServerWebExchange exchange, String traceId, long startedAt, Throwable error) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route == null ? "unknown" : route.getId();
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode == null ? 200 : statusCode.value();
        long durationMs = System.currentTimeMillis() - startedAt;
        if (error == null) {
            log.info("网关请求完成 traceId={} routeId={} status={} durationMs={}",
                    traceId, routeId, status, durationMs);
            return;
        }
        log.error("网关请求异常 traceId={} routeId={} status={} durationMs={} error={}",
                traceId, routeId, status, durationMs, error.getMessage(), error);
    }

    private String resolveTraceId(HttpHeaders headers) {
        String traceId = headers.getFirst(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }
}
