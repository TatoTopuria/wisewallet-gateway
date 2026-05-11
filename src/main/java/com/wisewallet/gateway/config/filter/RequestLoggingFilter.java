package com.wisewallet.gateway.config.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.time.Instant;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, Instant.now().toEpochMilli());

        return chain.filter(exchange)
                .doOnEach(signal -> {
                    if (signal.isOnComplete() || signal.isOnError()) {
                        logRequest(exchange, signal.getContextView());
                    }
                });
    }

    private void logRequest(ServerWebExchange exchange, ContextView context) {
        String correlationId = context.getOrDefault(CorrelationIdFilter.CORRELATION_ID_CONTEXT_KEY, "");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String remoteAddr = getRemoteAddr(exchange);
        int status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value()
                : 0;
        long duration = Instant.now().toEpochMilli()
                - (long) exchange.getAttributeOrDefault(START_TIME_ATTR, Instant.now().toEpochMilli());

        log.info("event=GATEWAY_RESPONSE status={} durationMs={} method={} path={} correlationId={} userId={} remoteAddr={}",
                status, duration, method, path, correlationId, userId != null ? userId : "", remoteAddr);
    }

    private String getRemoteAddr(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
