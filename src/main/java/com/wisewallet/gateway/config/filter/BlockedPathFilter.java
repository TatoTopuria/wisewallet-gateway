package com.wisewallet.gateway.config.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Hard-blocks sensitive paths before any route matching or security processing.
 * Order: -2 (before Spring Security which runs at -100, but after Reactor Netty).
 * Returns 403 Forbidden for all matched paths.
 */
@Component
public class BlockedPathFilter implements WebFilter, Ordered {

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "/internal/",
            "/actuator/",
            "/h2-console/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        boolean blocked = BLOCKED_PREFIXES.stream().anyMatch(path::startsWith);

        if (blocked) {
            byte[] body = "{\"status\":403,\"message\":\"Access denied\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        }

        return chain.filter(exchange);
    }
}
