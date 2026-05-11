package com.wisewallet.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private final GatewayProperties props;
    private final RateLimiterConfig rateLimiterConfig;

    public RouteConfig(GatewayProperties props, RateLimiterConfig rateLimiterConfig) {
        this.props = props;
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String accountUri = props.getRoutes().getAccountService().getUri();
        String transactionUri = props.getRoutes().getTransactionService().getUri();
        String notificationUri = props.getRoutes().getNotificationService().getUri();
        String advisorUri = props.getRoutes().getAdvisorService().getUri();

        return builder.routes()

                // --- Public auth routes (no JWT, IP rate limit) ---
                .route("account-register", r -> r
                        .path("/api/auth/register")
                        .and().method("POST")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.publicRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.ipKeyResolver());
                                }))
                        .uri(accountUri))

                .route("account-login", r -> r
                        .path("/api/auth/login")
                        .and().method("POST")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.publicRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.ipKeyResolver());
                                }))
                        .uri(accountUri))

                .route("account-refresh", r -> r
                        .path("/api/auth/refresh")
                        .and().method("POST")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.publicRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.ipKeyResolver());
                                }))
                        .uri(accountUri))

                .route("account-verify-email", r -> r
                        .path("/api/auth/verify")
                        .and().method("GET")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.publicRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.ipKeyResolver());
                                }))
                        .uri(accountUri))

                // --- Protected auth routes ---
                .route("account-logout", r -> r
                        .path("/api/auth/logout")
                        .and().method("POST")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.userRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.userKeyResolver());
                                })
                                .circuitBreaker(c -> c
                                        .setName("account-service-cb")
                                        .setFallbackUri("forward:/fallback/account")))
                        .uri(accountUri))

                // --- Account service routes ---
                .route("accounts-routes", r -> r
                        .path("/api/accounts/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.userRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.userKeyResolver());
                                })
                                .circuitBreaker(c -> c
                                        .setName("account-service-cb")
                                        .setFallbackUri("forward:/fallback/account")))
                        .uri(accountUri))

                .route("admin-routes", r -> r
                        .path("/api/admin/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.userRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.userKeyResolver());
                                })
                                .circuitBreaker(c -> c
                                        .setName("account-service-cb")
                                        .setFallbackUri("forward:/fallback/account")))
                        .uri(accountUri))

                // --- Transaction service routes ---
                .route("transactions-routes", r -> r
                        .path("/api/transactions/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.userRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.userKeyResolver());
                                })
                                .circuitBreaker(c -> c
                                        .setName("transaction-service-cb")
                                        .setFallbackUri("forward:/fallback/transaction")))
                        .uri(transactionUri))

                // --- Notification service routes ---
                .route("notifications-routes", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.userRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.userKeyResolver());
                                })
                                .circuitBreaker(c -> c
                                        .setName("notification-service-cb")
                                        .setFallbackUri("forward:/fallback/notification")))
                        .uri(notificationUri))

                // --- Advisor service routes ---
                .route("advisor-routes", r -> r
                        .path("/api/advisor/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(rateLimiterConfig.userRateLimiter());
                                    c.setKeyResolver(rateLimiterConfig.userKeyResolver());
                                })
                                .circuitBreaker(c -> c
                                        .setName("advisor-service-cb")
                                        .setFallbackUri("forward:/fallback/advisor")))
                        .uri(advisorUri))

                .build();
    }
}
