package com.chj.aigc.gateway.logging;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 为网关自身控制器请求补充 traceId。
 * 这样 `/gateway/health` 这类不经过路由转发的接口也能带统一链路头。
 */
@Component
public class GatewayTraceWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(GatewayTraceWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = resolveTraceId(exchange.getRequest().getHeaders());
        long startedAt = System.currentTimeMillis();

        ServerHttpRequest tracedRequest = exchange.getRequest().mutate()
                .header(GatewayTraceFilter.TRACE_HEADER, traceId)
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(tracedRequest).build();
        tracedExchange.getResponse().getHeaders().set(GatewayTraceFilter.TRACE_HEADER, traceId);

        log.info("网关本地请求开始 traceId={} method={} path={}",
                traceId,
                tracedExchange.getRequest().getMethod(),
                tracedExchange.getRequest().getURI().getRawPath());

        return chain.filter(tracedExchange)
                .doOnSuccess(unused -> log.info("网关本地请求完成 traceId={} status={} durationMs={}",
                        traceId,
                        tracedExchange.getResponse().getStatusCode() == null ? 200 : tracedExchange.getResponse().getStatusCode().value(),
                        System.currentTimeMillis() - startedAt))
                .doOnError(error -> log.error("网关本地请求异常 traceId={} durationMs={} error={}",
                        traceId,
                        System.currentTimeMillis() - startedAt,
                        error.getMessage(),
                        error));
    }

    private String resolveTraceId(HttpHeaders headers) {
        String traceId = headers.getFirst(GatewayTraceFilter.TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }
}
