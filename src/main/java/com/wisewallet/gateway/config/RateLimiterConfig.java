package com.wisewallet.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    private final GatewayProperties props;

    public RateLimiterConfig(GatewayProperties props) {
        this.props = props;
    }

    @Bean
    public RedisRateLimiter publicRateLimiter() {
        GatewayProperties.RateLimit.Bucket cfg = props.getRateLimit().getPublicBucket();
        return new RedisRateLimiter(cfg.getReplenishRate(), cfg.getBurstCapacity());
    }

    @Bean
    @Primary
    public RedisRateLimiter userRateLimiter() {
        GatewayProperties.RateLimit.Bucket cfg = props.getRateLimit().getUser();
        return new RedisRateLimiter(cfg.getReplenishRate(), cfg.getBurstCapacity());
    }

    /**
     * Key resolver based on source IP — used for public (unauthenticated) routes.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            if (exchange.getRequest().getRemoteAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }

    /**
     * Key resolver based on X-User-Id header injected by JwtAuthenticationFilter.
     * Only used on authenticated routes — filter runs before rate limiter.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            // Fall back to IP if header somehow absent
            if (exchange.getRequest().getRemoteAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("anonymous");
        };
    }
}
