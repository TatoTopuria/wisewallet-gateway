package com.wisewallet.gateway.config.filter;

import com.wisewallet.gateway.blocklist.JwtBlocklistService;
import com.wisewallet.gateway.jwt.InternalJwtSigner;
import com.wisewallet.gateway.jwt.JwtClaims;
import com.wisewallet.gateway.jwt.JwtValidationException;
import com.wisewallet.gateway.jwt.JwtValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global pre-filter that validates JWT tokens and injects user context headers.
 * Order: HIGHEST_PRECEDENCE + 20 — after CorrelationIdFilter, before rate limiter.
 * Requests matching PUBLIC_PATHS bypass JWT validation.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh"
    );
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtValidator jwtValidator;
    private final JwtBlocklistService blocklistService;
    private final InternalJwtSigner internalJwtSigner;

    public JwtAuthenticationFilter(JwtValidator jwtValidator,
                                   JwtBlocklistService blocklistService,
                                   InternalJwtSigner internalJwtSigner) {
        this.jwtValidator = jwtValidator;
        this.blocklistService = blocklistService;
        this.internalJwtSigner = internalJwtSigner;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (PUBLIC_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        JwtClaims claims;
        try {
            claims = jwtValidator.validateAndExtract(token);
        } catch (JwtValidationException e) {
            String message = e.getMessage() != null && e.getMessage().startsWith("Token expired")
                    ? "Token expired"
                    : "Authentication required";
            return writeError(exchange, HttpStatus.UNAUTHORIZED, message);
        }

        return blocklistService.isBlocked(claims.jti())
                .flatMap(blocked -> {
                    if (Boolean.TRUE.equals(blocked)) {
                        return writeError(exchange, HttpStatus.UNAUTHORIZED, "Token has been revoked");
                    }

                    String serviceToken = internalJwtSigner.sign(
                            claims.userId(), claims.roles(), claims.accountIds());

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", claims.userId())
                            .header("X-User-Roles", String.join(",", claims.roles()))
                            .header("X-Account-Ids", String.join(",", claims.accountIds()))
                            .header("X-Service-Token", serviceToken)
                            .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                            .build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(mutatedRequest)
                            .build();

                    return chain.filter(mutatedExchange);
                });
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] body = ("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
