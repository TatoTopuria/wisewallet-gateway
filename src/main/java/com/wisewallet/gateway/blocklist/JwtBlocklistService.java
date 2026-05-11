package com.wisewallet.gateway.blocklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class JwtBlocklistService {

    private static final Logger log = LoggerFactory.getLogger(JwtBlocklistService.class);
    private static final String KEY_PREFIX = "jwt:blocklist:";
    private static final Duration CHECK_TIMEOUT = Duration.ofMillis(50);

    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtBlocklistService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if the given jti is on the revocation blocklist.
     * Fails open on timeout or Redis unavailability (returns false with WARN log).
     */
    public Mono<Boolean> isBlocked(String jti) {
        return redisTemplate.hasKey(KEY_PREFIX + jti)
                .timeout(CHECK_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("Redis blocklist check failed for jti={}, failing open. Cause: {}", jti, ex.getMessage());
                    return Mono.just(Boolean.FALSE);
                });
    }
}
